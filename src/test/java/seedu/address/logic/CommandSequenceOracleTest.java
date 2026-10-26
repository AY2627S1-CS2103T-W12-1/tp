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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.SaveFailureException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks user-visible command sequences against an independent roster made of plain Java records. */
public class CommandSequenceOracleTest {
    @TempDir
    public Path directory;

    @Test
    public void execute_generatedUserWorkflows_matchesIndependentRosterViewAndRestartState() throws Exception {
        for (int seed = 0; seed < 8; seed++) {
            Session session = new Session(directory.resolve("seed" + seed), seed);
            for (int step = 0; step < 400; step++) {
                session.step(step);
            }
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

        SaveFailureException failure = assertThrows(SaveFailureException.class, () -> session.logic.execute("clear"));

        assertTrue(failure.getMessage().contains("No members were removed"));
        assertArrayEquals(saved, Files.readAllBytes(session.rosterFile));
        assertEquals(List.of("Zulu", "Bob", "Alice"), session.model.getAddressBook().getPersonList().stream()
                .map(person -> person.getName().fullName).toList());
        assertEquals(List.of("Alice"), session.model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName).toList());
        assertEquals(2, session.model.getActiveFilters().size());
        assertEquals(2, session.model.getActiveShowSelectionCount());
        Files.delete(blocker);
        Files.delete(backup);
        session.command("tagall t/retry");
        var savedPeople = new JsonAddressBookStorage(session.rosterFile)
                .readAddressBook().orElseThrow().getPersonList();
        assertEquals(List.of("Alice"), savedPeople.stream()
                .filter(person -> person.getTags().stream().anyMatch(tag -> tag.tagName.equals("retry")))
                .map(person -> person.getName().fullName).toList());
        session.logic.clearShowSelections();
        assertEquals(List.of("Alice", "Bob"), session.model.getFilteredPersonList().stream()
                .map(person -> person.getName().fullName).toList());
    }

    private record Member(int id, String name, Set<String> tags) {
        private Member {
            tags = Set.copyOf(tags);
        }

        private String email() {
            return "member" + id + "@group" + id % 3 + ".example.com";
        }

        private String snapshot() {
            return (100000 + id) + "|" + name + "|" + email() + "||"
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
        private boolean sorted;
        private int nextId;

        private Session(Path folder, int seed) throws Exception {
            this.folder = folder;
            this.seed = seed;
            random = new Random(seed);
            Files.createDirectories(folder);
            rosterFile = folder.resolve("members.json");
            restart();
            command("clear");
        }

        private void restart() throws Exception {
            JsonAddressBookStorage storage = new JsonAddressBookStorage(rosterFile);
            model = new ModelManager(storage.readAddressBook().orElseGet(seedu.address.model.AddressBook::new),
                    new UserPrefs());
            logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(folder.resolve("preferences.json"))));
            restrictions.clear();
            filterKeys.clear();
            selections.clear();
            sorted = false;
        }

        private List<Member> visible() {
            var stream = roster.stream().filter(member -> restrictions.stream().allMatch(p -> p.test(member))
                    && selections.stream().allMatch(ids -> ids.contains(member.id())));
            if (sorted) {
                stream = stream.sorted(Comparator.comparing(member -> member.name().toLowerCase(Locale.ROOT)));
            }
            return stream.toList();
        }

        private void command(String text) throws Exception {
            history.add(text);
            logic.execute(text);
        }

        private void step(int step) throws Exception {
            List<Member> displayed = visible();
            int operation = random.nextInt(20);
            boolean changesData = false;
            byte[] before = Files.readAllBytes(rosterFile);
            String tag = "Group" + random.nextInt(4);
            int index = displayed.isEmpty() ? -1 : random.nextInt(displayed.size());
            Member target = index < 0 ? null : displayed.get(index);
            switch (operation) {
                case 0, 1, 2 -> {
                    int id = nextId++;
                    Member member = new Member(id, "Team" + (char) ('A' + random.nextInt(4)) + " Member" + id,
                            Set.of(tag));
                    command("add n/" + member.name() + " p/" + (100000 + id) + " e/" + member.email() + " t/" + tag);
                    roster.add(member);
                    restrictions.clear();
                    filterKeys.clear();
                    selections.clear();
                    changesData = true;
                }
                case 3 -> {
                    command("list");
                    restrictions.clear();
                    filterKeys.clear();
                    selections.clear();
                    sorted = false;
                }
                case 4 -> {
                    command("sort");
                    sorted = true;
                }
                case 5 -> {
                    String query = "Team" + (char) ('A' + random.nextInt(4));
                    command("find " + query);
                    filterKeys.add("name:" + query);
                    restrictions.add(member -> member.name().toLowerCase(Locale.ROOT)
                            .contains(query.toLowerCase(Locale.ROOT)));
                }
                case 6 -> {
                    String query = "@group" + random.nextInt(3);
                    command("findemail " + query);
                    restrictions.clear();
                    filterKeys.clear();
                    selections.clear();
                    filterKeys.add("email:" + query);
                    restrictions.add(member -> member.email().contains(query));
                }
                case 7 -> {
                    command("filter t/" + tag);
                    if (!filterKeys.contains("tag:" + tag)) {
                        filterKeys.add("tag:" + tag);
                        restrictions.add(member -> member.tags().contains(tag));
                    }
                }
                case 8 -> {
                    if (target != null) {
                        Member second = displayed.get(random.nextInt(displayed.size()));
                        command("show " + (index + 1) + " " + (displayed.indexOf(second) + 1));
                        Set<Integer> ids = new HashSet<>(List.of(target.id(), second.id()));
                        selections.add(ids);
                    }
                }
                case 9 -> {
                    if (target != null) {
                        String name = "Team" + (char) ('A' + random.nextInt(4)) + " Edited" + target.id();
                        command("edit " + (index + 1) + " n/" + name);
                        replace(new Member(target.id(), name, target.tags()));
                        changesData = true;
                    }
                }
                case 10 -> {
                    if (target != null) {
                        command("tag " + (index + 1) + " t/" + tag);
                        changeTag(target, tag, true);
                        changesData = true;
                    }
                }
                case 11 -> {
                    if (target != null) {
                        boolean add = random.nextBoolean();
                        command((add ? "tagall" : "untagall") + " t/" + tag);
                        for (Member member : displayed) {
                            changeTag(member, tag, add);
                        }
                        changesData = true;
                    }
                }
                case 12 -> {
                    if (target != null) {
                        command("delete " + (index + 1));
                        boolean confirm = random.nextBoolean();
                        command(confirm ? "y" : "add n/Cancelled p/123 e/cancelled@example.com");
                        if (confirm) {
                            roster.remove(target);
                            changesData = true;
                        }
                    }
                }
                case 13 -> {
                    Path csv = folder.resolve("round trip.csv");
                    command("export " + csv);
                    assertArrayEquals(before, Files.readAllBytes(rosterFile), context(step));
                    command("import p/\"" + csv + "\"");
                    restrictions.clear();
                    filterKeys.clear();
                    selections.clear();
                    sorted = false;
                    changesData = true;
                }
                case 14 -> {
                    history.add("restart");
                    restart();
                }
                case 15 -> {
                    history.add("invalid: edit 0 p/123");
                    assertThrows(ParseException.class, () -> logic.execute("edit 0 p/123"), context(step));
                }
                case 16 -> {
                    String renamed = "Group" + (Integer.parseInt(tag.substring(5)) + 1) % 4;
                    List<Member> targets = roster.stream().filter(member -> member.tags().contains(tag)).toList();
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
                        changesData = true;
                    }
                }
                case 17 -> {
                    if (random.nextInt(5) == 0) {
                        command("clear");
                        roster.clear();
                        restrictions.clear();
                        filterKeys.clear();
                        selections.clear();
                        changesData = true;
                    }
                }
                case 18 -> {
                    if (target != null) {
                        command("edit " + (index + 1) + " t/");
                        replace(new Member(target.id(), target.name(), Set.of()));
                        changesData = true;
                    }
                }
                case 19 -> {
                    if (random.nextBoolean()) {
                        history.add("click: remove all show selections");
                        logic.clearShowSelections();
                        selections.clear();
                    } else if (!restrictions.isEmpty()) {
                        int removed = random.nextInt(restrictions.size());
                        history.add("click: remove filter " + removed);
                        model.removeActiveFilter(model.getActiveFilters().get(removed));
                        restrictions.remove(removed);
                        filterKeys.remove(removed);
                    }
                }
                default -> throw new AssertionError("Unknown generated operation");
            }
            String context = context(step);
            assertEquals(filterKeys.size(), model.getActiveFilters().size(), context);
            assertEquals(selections.size(), model.getActiveShowSelectionCount(), context);
            assertEquals(roster.stream().map(Member::snapshot).toList(),
                    model.getAddressBook().getPersonList().stream().map(Session::snapshot).toList(), context);
            assertEquals(visible().stream().map(Member::snapshot).toList(),
                    model.getFilteredPersonList().stream().map(Session::snapshot).toList(), context);
            assertEquals(roster.stream().map(Member::snapshot).toList(), new JsonAddressBookStorage(rosterFile)
                    .readAddressBook().orElseThrow().getPersonList().stream().map(Session::snapshot).toList(), context);
            if (!changesData) {
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

        private void changeTag(Member member, String tag, boolean add) {
            Set<String> tags = new HashSet<>(member.tags());
            if (add) {
                tags.add(tag);
            } else {
                tags.remove(tag);
            }
            replace(new Member(member.id(), member.name(), tags));
        }

        private String context(int step) {
            return "seed=" + seed + ", step=" + step + ", recent commands="
                    + history.subList(Math.max(0, history.size() - 20), history.size());
        }

        private static String snapshot(Person person) {
            return person.getPhone().value + "|" + person.getName().fullName + "|" + person.getEmail().value + "|"
                    + person.getAddress().value + "|"
                    + String.join(",", person.getTags().stream().map(tag -> tag.tagName).sorted().toList());
        }
    }
}
