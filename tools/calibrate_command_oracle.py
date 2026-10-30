"""Run known semantic faults in an isolated copy; retain logs and a JSON summary.

Usage: python tools/calibrate_command_oracle.py --gradle /path/to/gradle
Only tracked working-tree files are copied, including local edits. Production
files in the original checkout are never changed. No third-party Python packages.
"""

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET


MUTATIONS = [
    ("export_includes_hidden_members", "logic/commands/ExportCommand.java",
     "List<Person> people = List.copyOf(model.getFilteredPersonList());",
     "List<Person> people = model.getAddressBook().getPersonList();"),
    ("edit_uses_storage_index", "logic/commands/EditCommand.java",
     "List<Person> lastShownList = model.getFilteredPersonList();",
     "List<Person> lastShownList = model.getAddressBook().getPersonList();"),
    ("edit_not_saved", "logic/LogicManager.java",
     "if (command.isReadOnly()) {",
     "if (command.isReadOnly() || command instanceof seedu.address.logic.commands.EditCommand) {"),
    ("sort_reversed", "model/person/UniquePersonList.java",
     "String.CASE_INSENSITIVE_ORDER));",
     "String.CASE_INSENSITIVE_ORDER.reversed()));"),
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gradle", required=True, help="Absolute path to an installed gradle or gradle.bat")
    parser.add_argument("--gradle-user-home", help="Optional existing Gradle cache")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    parent = root / ".integration"
    parent.mkdir(exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix="oracle-calibration-", dir=parent))
    tracked = subprocess.check_output(
        ["git", "-c", f"safe.directory={root.as_posix()}", "ls-files", "-z"], cwd=root
    ).decode().split("\0")
    for name in filter(None, tracked):
        source = root / name
        if source.is_file():
            target = work / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
    # The harness may itself be newly added and therefore not tracked yet.
    harness = Path("src/test/java/seedu/address/logic/CommandSequenceOracleTest.java")
    shutil.copy2(root / harness, work / harness)
    command = [str(Path(args.gradle).resolve()), "--offline", "--no-daemon", "--console=plain"]
    if args.gradle_user_home:
        command += ["--gradle-user-home", str(Path(args.gradle_user_home).resolve())]
    command += ["test", "--tests", "seedu.address.logic.CommandSequenceOracleTest."
                "execute_composedViewEditsAndDeletion_surviveExportRestoreAndRestart"]
    report = work / "build/test-results/test/TEST-seedu.address.logic.CommandSequenceOracleTest.xml"
    results = []

    def run(name):
        started = time.monotonic()
        report.unlink(missing_ok=True)
        with (work / f"{name}.log").open("w", encoding="utf-8") as log:
            result = subprocess.run(command, cwd=work, stdout=log, stderr=subprocess.STDOUT,
                                    timeout=300, check=False)
        failures = []
        errors = []
        tests = 0
        if report.exists():
            suite = ET.parse(report).getroot()
            tests = int(suite.get("tests", "0"))
            for case in suite.findall("testcase"):
                for failure in case.findall("failure") + case.findall("error"):
                    item = {"test": case.get("name"), "message": failure.get("message", "")}
                    # Compile errors, setup failures and unrelated crashes are not successful detections.
                    if "Assertion" in failure.get("type", ""):
                        failures.append(item)
                    else:
                        errors.append(item)
        if result.returncode == 0 and tests > 0:
            status = "PASS" if name == "baseline" else "SURVIVED"
        elif failures and not errors:
            status = "BASELINE_FAILED" if name == "baseline" else "DETECTED"
        else:
            status = "ERROR"
        entry = {"name": name, "status": status, "exit_code": result.returncode,
                 "seconds": round(time.monotonic() - started, 3),
                 "tests": tests, "failures": failures, "errors": errors}
        results.append(entry)
        (work / "summary.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
        print(f"{name}: {status}", flush=True)
        return status

    print(f"Artifacts: {work}", flush=True)
    if run("baseline") != "PASS":
        raise SystemExit("Baseline must pass before interpreting mutation results")
    for name, relative, old, new in MUTATIONS:
        source = work / "src/main/java/seedu/address" / relative
        original = source.read_bytes()
        text = original.decode("utf-8")
        if text.count(old) != 1:
            raise SystemExit(f"Mutation {name} no longer matches exactly one source location")
        try:
            source.write_bytes(text.replace(old, new).encode("utf-8"))
            run(name)
        finally:
            source.write_bytes(original)
    detected = sum(result["status"] == "DETECTED" for result in results[1:])
    print(f"Detected {detected}/{len(MUTATIONS)} selected faults. See {work / 'summary.json'}")
    if detected != len(MUTATIONS):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
