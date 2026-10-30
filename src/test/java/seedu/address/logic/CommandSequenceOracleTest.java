package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ActiveFilter;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks user-visible command sequences against an independent roster made of plain Java records. */
public class CommandSequenceOracleTest {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    public Path directory;

    @Test
    public void execute_generatedUserWorkflows_matchesIndependentRosterViewAndRestartState() throws Exception {
        int seeds = Integer.parseInt(System.getenv().getOrDefault("ORACLE_SEEDS", "16"));
        int steps = Integer.parseInt(System.getenv().getOrDefault("ORACLE_STEPS", "500"));
        int firstSeed = Integer.parseInt(System.getenv().getOrDefault("ORACLE_FIRST_SEED", "0"));
        assertTrue(seeds > 0 && steps > 0, "Use positive seed and step counts");
        for (int seed = firstSeed; seed < firstSeed + seeds; seed++) {
            Session session = new Session(directory.resolve("seed" + seed), seed);
            for (int step = 0; step < steps; step++) {
                session.step(step);
            }
        }
    }

    @Test
    public void execute_composedViewEditsAndDeletion_surviveExportRestoreAndRestart() throws Exception {
        for (int seed = 0; seed < 12; seed++) {
            Session session = new Session(directory.resolve("composed" + seed), seed);
            int step = 0;
            for (int member = 0; member < 9; member++) {
                session.checkedOperation(0, step++);
            }
            session.checkedOperation(4, step++);
            String tag = session.getVisibleMembers().getFirst().tags().iterator().next();
            session.command("filter t/" + tag);
            session.filterKeys.add("tag:" + tag);
            session.restrictions.add(member -> member.tags().contains(tag));
            session.assertState(session.createStepInput(step++), Session.Change.NONE);
            // These operations must have a target, even if the random walks mostly encounter empty views.
            for (int operation : new int[] {8, 9, 10, 22, 20, 21, 23, 24, 25}) {
                assertTrue(!session.getVisibleMembers().isEmpty(), session.describeStep(step));
                session.checkedOperation(operation, step++);
            }
            Session.StepInput deletion = session.createStepInput(step++);
            session.command("delete " + (deletion.index() + 1));
            session.command("n");
            session.assertState(deletion, Session.Change.NONE);
            session.command("delete " + (deletion.index() + 1));
            session.command("yes");
            session.roster.remove(deletion.target());
            session.selections.forEach(selection -> selection.remove(deletion.target().id()));
            session.assertState(deletion, Session.Change.APPLIED);
            session.checkedOperation(26, step++);
            session.checkedOperation(14, step);
        }
    }

    @Test
    public void clear_saveFailsAfterComposedView_restoresSelectionsAndRetainsCorrectBulkTargets() throws Exception {
        Session session = new Session(directory.resolve("failed-clear"), 99);
        session.command("add n/Zulu p/111 e/z@example.com t/keep");
        session.command("add n/Bob p/222 e/b@example.com t/keep");
        session.command("add n/Alice p/333 e/a@example.com t/keep");
        session.command("find Alice Bob");
        session.command("filter t/keep");
        session.command("sort");
        session.command("show 1 2");
        session.command("show 1");
        byte[] saved = Files.readAllBytes(session.rosterFile);
        Path backup = JsonAddressBookStorage.getBackupFilePath(session.rosterFile);
        Files.delete(backup);
        Files.createDirectory(backup);
        Path blocker = backup.resolve("blocked.txt");
        Files.writeString(blocker, "Force the clear save to fail");

        session.logic.execute("clear /all");
        SaveFailureException failure = assertThrows(SaveFailureException.class, () -> session.logic.execute("yes"));

        assertTrue(failure.getMessage().contains("No members were removed"));
        assertArrayEquals(saved, Files.readAllBytes(session.rosterFile));
        assertEquals(List.of("Alice", "Bob", "Zulu"), session.model.getAddressBook().getPersonList().stream()
                .map(person -> person.getName().fullName).toList());
        assertEquals(List.of("Alice"), session.model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName).toList());
        assertEquals(4, session.model.getActiveFilters().size());
        assertEquals(2, getShowFilters(session.model.getActiveFilters()).size());
        Files.delete(blocker);
        Files.delete(backup);
        session.command("tagall t/retry");
        var savedPeople = new JsonAddressBookStorage(session.rosterFile)
                .readAddressBook().orElseThrow().getPersonList();
        assertEquals(List.of("Alice"), savedPeople.stream()
                .filter(person -> person.getTags().stream().anyMatch(tag -> tag.tagName.equals("retry")))
                .map(person -> person.getName().fullName).toList());
        getShowFilters(session.logic.getActiveFilters()).forEach(session.logic::removeActiveFilter);
        assertEquals(List.of("Alice", "Bob"), session.model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName).toList());
    }

    private static List<ActiveFilter> getShowFilters(List<ActiveFilter> filters) {
        return filters.stream().filter(filter -> filter.getKind() == ActiveFilter.Kind.SHOW).toList();
    }

    private record Member(int id, String name, Set<String> tags) {
        private Member {
            tags = Set.copyOf(tags);
        }

        private String getEmail() {
            return "member" + id + "@group" + id % 3 + ".example.com";
        }

        private String formatSnapshot() {
            return (100000 + id) + "|" + name + "|" + getEmail() + "||"
                    + String.join(",", tags.stream().sorted().toList());
        }
    }

    private static class Session {
        private final Path folder;
        private final Path rosterFile;
        private final Random random;
        private final int seed;
        private final List<Member> roster = new ArrayList<>();
        private final List<Predicate<Member>> restrictions = new ArrayList<>();
        private final List<String> filterKeys = new ArrayList<>();
        private final List<Set<Integer>> selections = new ArrayList<>();
        private final List<String> history = new ArrayList<>();
        private ModelManager model;
        private LogicManager logic;
        private int nextId;

        private Session(Path folder, int seed) throws Exception {
            this.folder = folder;
            this.seed = seed;
            random = new Random(seed);
            Files.createDirectories(folder);
            rosterFile = folder.resolve("members.json");
            restart();
            command("clear /all");
        }

        private void restart() throws Exception {
            JsonAddressBookStorage storage = new JsonAddressBookStorage(rosterFile);
            model = new ModelManager(storage.readAddressBook().orElseGet(seedu.address.model.AddressBook::new),
                    new UserPrefs());
            logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(folder.resolve("preferences.json"))));
            resetFilters();
        }

        private void resetFilters() {
            restrictions.clear();
            filterKeys.clear();
            selections.clear();
        }

        private List<Member> getVisibleMembers() {
            return roster.stream().filter(member -> restrictions.stream().allMatch(p -> p.test(member))
                    && selections.stream().allMatch(ids -> ids.contains(member.id()))).toList();
        }

        private void command(String text) throws Exception {
            history.add(text);
            logic.execute(text);
        }

        private enum Change { NONE, APPLIED }

        private record StepInput(List<Member> displayed, int index, Member target,
                String tag, byte[] savedBefore, int step) {}

        private void step(int step) throws Exception {
            checkedOperation(random.nextInt(27), step);
        }

        private void checkedOperation(int operation, int step) throws Exception {
            try {
                StepInput input = createStepInput(step);
                Change change = performOperation(operation, input);
                assertState(input, change);
            } catch (Exception | AssertionError failure) {
                Path trace = folder.resolve("failure-trace.txt");
                Files.writeString(trace, describeStep(step) + "\n" + String.join("\n", history));
                throw new AssertionError(describeStep(step) + ", full trace=" + trace, failure);
            }
        }

        private StepInput createStepInput(int step) throws Exception {
            List<Member> displayed = getVisibleMembers();
            byte[] before = Files.readAllBytes(rosterFile);
            String tag = "Group" + random.nextInt(4);
            int index = displayed.isEmpty() ? -1 : random.nextInt(displayed.size());
            Member target = index < 0 ? null : displayed.get(index);
            return new StepInput(displayed, index, target, tag, before, step);
        }

        private Change performOperation(int operation, StepInput input) throws Exception {
            return switch (operation) {
                case 0, 1, 2 -> addMember(input);
                case 3 -> listMembers();
                case 4 -> sortMembers();
                case 5 -> findByName();
                case 6 -> findByEmail();
                case 7 -> filterByTag(input);
                case 8 -> showMembers(input);
                case 9 -> editName(input);
                case 10 -> tagMember(input);
                case 11 -> updateDisplayedTags(input);
                case 12 -> answerDeletion(input);
                case 13 -> roundTripCsv(input);
                case 14 -> restartSession();
                case 15 -> rejectInvalidIndex(input);
                case 16 -> renameTag(input);
                case 17 -> clearRoster();
                case 18 -> clearMemberTags(input);
                case 19 -> removeRestriction();
                case 20 -> rejectMalformedCommands(input);
                case 21 -> rejectDuplicateMember(input);
                case 22 -> verifyExport(input);
                case 23 -> rejectInvalidImport(input);
                case 24 -> rejectBlockedExport(input);
                case 25 -> repeatSort();
                case 26 -> restoreExport(input);
                default -> throw new AssertionError("Unknown generated operation");
            };
        }

        private Change addMember(StepInput input) throws Exception {
            String tag = input.tag();
            int id = nextId++;
            Member member = new Member(id, "Team" + (char) ('A' + random.nextInt(4)) + " Member" + id,
                    Set.of(tag));
            command("add n/" + member.name() + " p/" + (100000 + id) + " e/" + member.getEmail() + " t/" + tag);
            roster.add(member);
            resetFilters();
            return Change.APPLIED;
        }

        private Change listMembers() throws Exception {
            command("list");
            resetFilters();
            return Change.NONE;
        }

        private Change sortMembers() throws Exception {
            command("sort");
            roster.sort(Comparator.comparing(Member::name, String.CASE_INSENSITIVE_ORDER));
            return Change.APPLIED;
        }

        private Change findByName() throws Exception {
            String query = "Team" + (char) ('A' + random.nextInt(4));
            command("find " + query);
            if (!filterKeys.contains("name:" + query)) {
                filterKeys.add("name:" + query);
                restrictions.add(member -> member.name().toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)));
            }
            return Change.NONE;
        }

        private Change findByEmail() throws Exception {
            String query = "@group" + random.nextInt(3);
            command("findemail " + query);
            if (!filterKeys.contains("email:" + query)) {
                filterKeys.add("email:" + query);
                restrictions.add(member -> member.getEmail().contains(query));
            }
            return Change.NONE;
        }

        private Change filterByTag(StepInput input) throws Exception {
            String tag = input.tag();
            command("filter t/" + tag);
            if (!filterKeys.contains("tag:" + tag)) {
                filterKeys.add("tag:" + tag);
                restrictions.add(member -> member.tags().contains(tag));
            }
            return Change.NONE;
        }

        private Change showMembers(StepInput input) throws Exception {
            List<Member> displayed = input.displayed();
            int index = input.index();
            Member target = input.target();
            if (target != null) {
                Member second = displayed.get(random.nextInt(displayed.size()));
                command("show " + (index + 1) + " " + (displayed.indexOf(second) + 1));
                Set<Integer> ids = new HashSet<>(List.of(target.id(), second.id()));
                if (!selections.contains(ids)) {
                    selections.add(ids);
                }
            }
            return Change.NONE;
        }

        private Change editName(StepInput input) throws Exception {
            int index = input.index();
            Member target = input.target();
            if (target != null) {
                String name = "Team" + (char) ('A' + random.nextInt(4)) + " Edited" + target.id();
                command("edit " + (index + 1) + " n/" + name);
                replace(new Member(target.id(), name, target.tags()));
                return Change.APPLIED;
            }
            return Change.NONE;
        }

        private Change tagMember(StepInput input) throws Exception {
            String tag = input.tag();
            int index = input.index();
            Member target = input.target();
            if (target != null) {
                command("tag " + (index + 1) + " t/" + tag);
                changeTag(target, tag, true);
                return Change.APPLIED;
            }
            return Change.NONE;
        }

        private Change updateDisplayedTags(StepInput input) throws Exception {
            List<Member> displayed = input.displayed();
            String tag = input.tag();
            Member target = input.target();
            if (target != null) {
                boolean shouldAddTag = random.nextBoolean();
                command((shouldAddTag ? "tagall" : "untagall") + " t/" + tag);
                for (Member member : displayed) {
                    changeTag(member, tag, shouldAddTag);
                }
                return Change.APPLIED;
            }
            return Change.NONE;
        }

        private Change answerDeletion(StepInput input) throws Exception {
            int index = input.index();
            Member target = input.target();
            if (target != null) {
                command("delete " + (index + 1));
                boolean shouldConfirmDeletion = random.nextBoolean();
                command(shouldConfirmDeletion ? "yes" : "add n/Cancelled p/123 e/cancelled@example.com");
                if (shouldConfirmDeletion) {
                    roster.remove(target);
                    selections.forEach(selection -> selection.remove(target.id()));
                    return Change.APPLIED;
                }
            }
            return Change.NONE;
        }

        private Change roundTripCsv(StepInput input) throws Exception {
            int step = input.step();
            byte[] before = input.savedBefore();
            Path csv = folder.resolve("round trip.csv");
            command("export " + csv);
            assertCsv(csv, step);
            assertArrayEquals(before, Files.readAllBytes(rosterFile), describeStep(step));
            command("import p/\"" + csv + "\"");
            resetFilters();
            return Change.APPLIED;
        }

        private Change rejectMalformedCommands(StepInput input) throws Exception {
            for (String text : List.of("edit 0 n/Invalid", "delete -1", "show 2147483648",
                    "add n/Invalid p/notaphone e/bad", "find", "import")) {
                history.add(text);
                assertThrows(ParseException.class, () -> logic.execute(text), describeStep(input.step()));
                assertState(input, Change.NONE);
            }
            String text = "edit " + (input.displayed().size() + 1) + " n/OutOfRange";
            history.add(text);
            assertThrows(CommandException.class, () -> logic.execute(text), describeStep(input.step()));
            return Change.NONE;
        }

        private Change rejectDuplicateMember(StepInput input) throws Exception {
            if (!roster.isEmpty()) {
                Member member = roster.get(random.nextInt(roster.size()));
                String text = "add n/" + member.name() + " p/" + (100000 + member.id())
                        + " e/" + member.getEmail();
                history.add(text);
                assertThrows(CommandException.class, () -> logic.execute(text), describeStep(input.step()));
            }
            return Change.NONE;
        }

        private Change verifyExport(StepInput input) throws Exception {
            Path csv = folder.resolve("independent export.csv");
            command("export " + csv);
            assertCsv(csv, input.step());
            return Change.NONE;
        }

        /** Reads CSV syntax independently of the application's importer and compares every field and row order. */
        private void assertCsv(Path csv, int step) throws Exception {
            List<List<String>> expected = new ArrayList<>();
            expected.add(List.of("name", "phone", "email", "address", "tags"));
            for (Member member : getVisibleMembers()) {
                expected.add(List.of(member.name(), Integer.toString(100000 + member.id()), member.getEmail(), "",
                        String.join(";", member.tags().stream().sorted().toList())));
            }
            try (var reader = Files.newBufferedReader(csv); var parser = CSVFormat.DEFAULT.parse(reader)) {
                assertEquals(expected, parser.getRecords().stream().map(record -> record.toList()).toList(),
                        describeStep(step));
            }
        }

        private Change rejectInvalidImport(StepInput input) throws Exception {
            Path csv = folder.resolve("invalid import.csv");
            // A valid first row must not leak into the roster when a later row is rejected.
            Files.writeString(csv, "name,phone,email,address,tags\n"
                    + "New Candidate,999999,new@example.com,,\n"
                    + "Invalid Candidate,notaphone,bad,,\n");
            String text = "import p/\"" + csv + "\"";
            history.add(text);
            assertThrows(CommandException.class, () -> logic.execute(text), describeStep(input.step()));
            return Change.NONE;
        }

        private Change rejectBlockedExport(StepInput input) throws Exception {
            Path destination = folder.resolve("blocked export.csv");
            Files.createDirectories(destination);
            Path sentinel = destination.resolve("keep.txt");
            Files.writeString(sentinel, "existing destination");
            String text = "export " + destination;
            history.add(text);
            assertThrows(CommandException.class, () -> logic.execute(text), describeStep(input.step()));
            assertEquals("existing destination", Files.readString(sentinel), describeStep(input.step()));
            return Change.NONE;
        }

        private Change repeatSort() throws Exception {
            sortMembers();
            return sortMembers();
        }

        private Change restoreExport(StepInput input) throws Exception {
            listMembers();
            Path csv = folder.resolve("restore roster.csv");
            command("export " + csv);
            assertCsv(csv, input.step());
            command("clear /all");
            if (!roster.isEmpty()) {
                command("yes");
            }
            assertTrue(model.getAddressBook().getPersonList().isEmpty(), describeStep(input.step()));
            command("import p/\"" + csv + "\"");
            resetFilters();
            restartSession();
            return Change.APPLIED;
        }

        private Change restartSession() throws Exception {
            history.add("restart");
            restart();
            return Change.NONE;
        }

        private Change rejectInvalidIndex(StepInput input) throws Exception {
            int step = input.step();
            history.add("invalid: edit 0 p/123");
            assertThrows(ParseException.class, () -> logic.execute("edit 0 p/123"), describeStep(step));
            return Change.NONE;
        }

        private Change renameTag(StepInput input) throws Exception {
            String tag = input.tag();
            String renamed = "Group" + (Integer.parseInt(tag.substring(5)) + 1) % 4;
            List<Member> targets = input.displayed().stream().filter(member -> member.tags().contains(tag)).toList();
            if (targets.isEmpty()) {
                assertThrows(CommandException.class, () ->
                        logic.execute("renametag t/" + tag + " t/" + renamed));
            } else {
                command("renametag t/" + tag + " t/" + renamed);
                for (Member member : targets) {
                    Set<String> tags = new HashSet<>(member.tags());
                    tags.remove(tag);
                    tags.add(renamed);
                    replace(new Member(member.id(), member.name(), tags));
                }
                return Change.APPLIED;
            }
            return Change.NONE;
        }

        private Change clearRoster() throws Exception {
            if (random.nextInt(5) == 0) {
                command("clear /all");
                if (!roster.isEmpty()) {
                    command("yes");
                }
                roster.clear();
                resetFilters();
                return Change.APPLIED;
            }
            return Change.NONE;
        }

        private Change clearMemberTags(StepInput input) throws Exception {
            int index = input.index();
            Member target = input.target();
            if (target != null) {
                command("edit " + (index + 1) + " t/");
                replace(new Member(target.id(), target.name(), Set.of()));
                return Change.APPLIED;
            }
            return Change.NONE;
        }

        private Change removeRestriction() throws Exception {
            int choice = random.nextInt(3);
            if (choice == 0 && !selections.isEmpty()) {
                int removed = random.nextInt(selections.size());
                history.add("click: remove show selection " + removed);
                List<ActiveFilter> showFilters = getShowFilters(model.getActiveFilters());
                model.removeActiveFilter(showFilters.get(removed));
                selections.remove(removed);
            } else if (choice == 1) {
                history.add("click: remove all show selections");
                getShowFilters(logic.getActiveFilters()).forEach(logic::removeActiveFilter);
                selections.clear();
            } else if (!restrictions.isEmpty()) {
                int removed = random.nextInt(restrictions.size());
                history.add("click: remove filter " + removed);
                List<ActiveFilter> nonShowFilters = model.getActiveFilters().stream()
                        .filter(filter -> filter.getKind() != ActiveFilter.Kind.SHOW).toList();
                model.removeActiveFilter(nonShowFilters.get(removed));
                restrictions.remove(removed);
                filterKeys.remove(removed);
            }
            return Change.NONE;
        }

        private void assertState(StepInput input, Change change) throws Exception {
            int step = input.step();
            byte[] before = input.savedBefore();
            String context = describeStep(step);
            assertEquals(filterKeys.size() + selections.size(), model.getActiveFilters().size(), context);
            assertEquals(selections.size(), getShowFilters(model.getActiveFilters()).size(), context);
            assertEquals(roster.stream().map(Member::formatSnapshot).toList(),
                    model.getAddressBook().getPersonList().stream().map(Session::formatSnapshot).toList(), context);
            assertEquals(getVisibleMembers().stream().map(Member::formatSnapshot).toList(),
                    model.getFilteredPersonList().stream().map(Session::formatSnapshot).toList(), context);
            assertEquals(roster.stream().map(Member::formatSnapshot).toList(), new JsonAddressBookStorage(rosterFile)
                    .readAddressBook().orElseThrow().getPersonList().stream()
                    .map(Session::formatSnapshot).toList(), context);
            if (change == Change.NONE) {
                assertArrayEquals(before, Files.readAllBytes(rosterFile), context);
            }
        }

        private void replace(Member updated) {
            for (int index = 0; index < roster.size(); index++) {
                if (roster.get(index).id() == updated.id()) {
                    roster.set(index, updated);
                    return;
                }
            }
            throw new AssertionError("Reference roster member missing");
        }

        private void changeTag(Member member, String tag, boolean shouldAddTag) {
            Set<String> tags = new HashSet<>(member.tags());
            if (shouldAddTag) {
                tags.add(tag);
            } else {
                tags.remove(tag);
            }
            replace(new Member(member.id(), member.name(), tags));
        }

        private String describeStep(int step) {
            return "seed=" + seed + ", step=" + step + ", recent commands="
                    + history.subList(Math.max(0, history.size() - 20), history.size());
        }

        private static String formatSnapshot(Person person) {
            return person.getPhone().value + "|" + person.getName().fullName + "|" + person.getEmail().value + "|"
                    + person.getAddress().value + "|"
                    + String.join(",", person.getTags().stream().map(tag -> tag.tagName).sorted().toList());
        }
    }
}
