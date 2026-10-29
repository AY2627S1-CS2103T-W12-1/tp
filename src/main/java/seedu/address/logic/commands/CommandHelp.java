package seedu.address.logic.commands;

import java.util.Optional;

import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

/**
 * Offline help for the commands implemented by TrackCall. Planned commands do not belong in this catalogue.
 */
public enum CommandHelp {
    LIST("Browse members", "list", "Show the full roster", "list", "list",
            "Displays every member and their current list number.",
            "Shows the complete roster in stored order, clearing searches, tag filters, show selections and sorting.",
            "Use list before choosing a member by number. List numbers can change after an edit or deletion.",
            "Use the lowercase command list with no extra arguments."),
    SHOW("Browse members", "show", "Show only selected members", "show INDEX [MORE_INDEXES]...", "show 1 3",
            "Show only the members at the specified numbers in the current list.",
            "Shows members 1 and 3 in their current order, then renumbers the displayed list.",
            "Each distinct show selection adds a box with its selected-member count. Repeating the same "
                    + "selection adds no box. Hover over a box label to see names in alphabetical order, "
                    + "ignoring case; use its X to remove only that selection while keeping other filters, "
                    + "or use list to restore everyone. "
                    + "Repeated numbers select a member once. "
                    + "A later show uses the new numbers.",
            "Supply space-separated positive displayed numbers. An invalid or missing number leaves the view "
                    + "unchanged."),
    SORT("Browse members", "sort", "Sort members by name", "sort", "sort",
            "Orders the displayed members by full name from A to Z, ignoring letter case.",
            "Shows the current search results alphabetically without changing the saved roster.",
            "Sorting stays active until list, a successful import, or a restart. "
                    + "Check list numbers again before editing or deleting.",
            "Use sort with no extra arguments. Use list to restore the complete roster in stored order."),
    FIND("Browse members", "find", "Find a member by name", "find KEYWORD [MORE_KEYWORDS]", "find Alice Tan",
            "Narrows the displayed members by any complete or partial name word, ignoring letter case.",
            "Shows members whose names contain Alice or Tan. No match produces an empty list.",
            "Each distinct find adds a name filter. Boxes appear left to right in command order, but find and filter "
                    + "order does not change the final matches. Use X to remove one box or list to show everyone. "
                    + "Jo and oh both match John. Repeats ignore keyword case, repetition and order; "
                    + "they keep the existing box and report that the search is already active. "
                    + "A different query that keeps the same results adds its own box and reports Results unchanged.",
            "Supply at least one name word or part of one. Searches use names, not phone numbers or tags."),
    FINDEMAIL("Browse members", "findemail", "Find members by email",
            "findemail KEYWORD [MORE_KEYWORDS]...", "findemail @example.com",
            "Narrows displayed members by any literal email fragment, ignoring letter case.",
            "Shows members whose email contains @example.com. No match produces an empty list.",
            "Use a full email address or domain fragment. Whitespace separates keywords; any keyword can match. "
                    + "Each distinct search adds an Email box and retains earlier searches, tag filters, "
                    + "show selections and name sorting. "
                    + "Repeats ignore keyword case, repetition and order; they keep the existing box "
                    + "and report that the search is already active. A different query with the same results "
                    + "adds its own box and reports Results unchanged. "
                    + "Use a box's X to remove only that email condition. "
                    + "Filter narrows these results; list shows everyone again. Check displayed numbers before "
                    + "editing or deleting. An edited member whose email no longer matches disappears from view.",
            "Supply at least one keyword. Matching uses literal text, not regular expressions, and searches "
                    + "email addresses only, not names or phone numbers. Searching does not save the roster."),
    FILTER("Browse members", "filter", "Filter members by tag", "filter t/TAG [t/MORE_TAGS]...",
            "filter t/committee t/alumni",
            "Shows currently displayed members with any of the specified tags without changing saved data.",
            "Shows only members tagged committee or alumni, in their current order with new numbers.",
            "Matching ignores letter case. Tags in the same command mean or, and the box reads "
                    + "Tag: committee / alumni. A separate filter command narrows the list further (and), "
                    + "and the result names all active tags. Repeating an active filter leaves the view "
                    + "unchanged and reports that no changes were made. A box names at most four tags, then "
                    + "+N more; hover for the full list. In the result, a comma means and and a slash means "
                    + "or. Boxes appear left to right in command order, not matching priority. "
                    + "Use a filter's X to remove it, or list to show everyone. Find also narrows the current results. "
                    + "Findemail also narrows the current results by email. "
                    + "Edit and delete use displayed numbers; "
                    + "editing keeps the active search and filters. Members that no longer match disappear from view.",
            "Supply at least one tag, each after t/. " + Tag.MESSAGE_CONSTRAINTS + ". "
                    + "Empty tags, internal spaces, "
                    + "and extra arguments are rejected, and one invalid tag rejects the whole command."),
    ADD("Manage members", "add", "Add a member",
            "add n/NAME p/PHONE e/EMAIL [a/ADDRESS] [t/TAG]...",
            "add n/Alice Tan p/91234567 e/alice@example.com a/12 Orchard Road t/committee",
            "Creates a member with a name, phone and email. Address and tags are optional.",
            "Adds Alice Tan and shows the complete roster after the record is saved.",
            "Fields can appear in any order. Use one t/ prefix per tag. " + memberFieldRules(),
            "Include name, phone and email once. Omit a/ if the address is unknown; a/ needs a value when used. "
                    + "Duplicates ignore name case and repeated spaces when phone, email and address match. "
                    + "Names keep their formatting. Use find and edit for an existing member. "
                    + "If saving fails, the member "
                    + "remains in this session and the command box clears; follow the recovery guidance before "
                    + "exiting."),
    EDIT("Manage members", "edit", "Update a member",
            "edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...",
            "edit 1 p/98765432 t/committee t/year1",
            "Updates the member at a displayed list number. Fields you leave out stay unchanged.",
            "Changes member 1's phone and replaces their tags with committee and year1, "
                    + "keeping the active search and tag filters.",
            "Give the full tag set you want to keep. Use edit 1 t/ to remove all tags, "
                    + "or edit 1 a/ to clear the address. " + memberFieldRules(),
            "Use a current positive list number and supply at least one field. Invalid values or duplicate contact "
                    + "details are rejected, ignoring name case and repeated spaces when other contact fields match. "
                    + "Your own record is excluded. If saving fails, the edit remains in this session "
                    + "and the command box "
                    + "clears; follow the recovery guidance before exiting."),
    TAG("Manage members", "tag", "Tag one member", "tag INDEX t/TAG", "tag 1 t/committee",
            "Adds one tag to the member at a displayed list number while retaining their other tags.",
            "Adds committee to member 1 and shows their name and updated tags after saving.",
            "Check the current list number first. Existing tags, searches, filters and sorting remain in place. "
                    + "A member who already has the tag, even with different capitalization, is left unchanged.",
            "Supply one current positive displayed number and one tag. " + Tag.MESSAGE_CONSTRAINTS + ". "
                    + "Invalid input or a missing member is rejected. If saving fails, the change remains in this "
                    + "session; follow the recovery guidance before exiting."),
    TAGALL("Manage members", "tagall", "Tag every displayed member", "tagall t/TAG", "tagall t/committee",
            "Adds one tag to every member in the current list. Hidden members and other tags stay unchanged.",
            "Adds committee to each displayed member, skips members who already have it, and reports both counts "
                    + "after saving.",
            "Use find or filter first to choose the group, then check the list. The current search, filters and "
                    + "sorting stay applied.",
            "Supply exactly one tag after t/. " + Tag.MESSAGE_CONSTRAINTS + ". "
                    + "Empty tags, internal spaces, "
                    + "multiple tags and extra arguments are rejected. An empty list is rejected without changes. "
                    + "If saving fails, the changes remain in this session and the command box clears; follow the "
                    + "recovery guidance before exiting."),
    UNTAGALL("Manage members", "untagall", "Untag every displayed member", "untagall t/TAG", "untagall t/committee",
            "Removes one tag from every member in the current list. Hidden members and other tags stay unchanged.",
            "Removes committee from each displayed member, skips members without it, and reports both counts "
                    + "after saving.",
            "Use find or filter first to choose the group, then check the list. Updated members disappear "
                    + "only if they no longer satisfy all active conditions. Removing the tag of a single-tag "
                    + "filter hides them, but a member can stay visible by matching another tag in a filter "
                    + "group. Use list to show everyone again.",
            "Supply exactly one tag after t/. " + Tag.MESSAGE_CONSTRAINTS + ". "
                    + "Empty tags, internal spaces, "
                    + "multiple tags and extra arguments are rejected. An empty list is rejected without changes. "
                    + "If saving fails, the changes remain in this session and the command box clears; follow the "
                    + "recovery guidance before exiting."),
    UNTAG("Manage members", "untag", "Untag one member", "untag INDEX t/TAG", "untag 1 t/friends",
            "Removes one tag from the member at a displayed list number while retaining their other tags.",
            "Removes friends from member 1 and shows their name and updated tags after saving.",
            "Check the current list number first. Other tags, searches, filters and sorting remain in place. "
                    + "Removing a tag used by an active filter may hide the member from the current list.",
            "Supply one current positive displayed number and one tag. " + Tag.MESSAGE_CONSTRAINTS + ". "
                    + "Invalid input or a missing member is rejected. If the member lacks the tag, no change is "
                    + "made. If saving fails, follow the recovery guidance before exiting."),
    RENAMETAG("Manage members", "renametag", "Rename a tag for the current list", "renametag t/CURRENT_TAG t/NEW_TAG",
            "renametag t/batch2026 t/alumni",
            "Replaces one tag with another for every member in the current list. Hidden members are not changed.",
            "Changes batch2026 to alumni for each displayed member who has it, and reports how many members "
                    + "changed after saving.",
            "Tags match regardless of letter case. Run list first to rename for everyone, for example before "
                    + "renametag t/committee t/Committee to standardize capitalization. A member with both "
                    + "distinct tags keeps only the replacement. Updated members disappear only if they no "
                    + "longer satisfy all active conditions: renaming the tag of a single-tag filter hides "
                    + "them, but a member can stay visible by matching another tag in a filter group. Use "
                    + "list to show everyone again.",
            "Supply exactly two tags, each after t/. " + Tag.MESSAGE_CONSTRAINTS + ". "
                    + "Their spellings must differ. "
                    + "A current tag that no displayed member has is rejected without changes. If saving fails, "
                    + "the changes remain in this session and the command box clears; follow the recovery "
                    + "guidance before exiting."),
    EXPORT("Manage members", "export", "Export members to CSV", "export [FILEPATH]", "export members.csv",
            "Writes displayed members to a UTF-8 CSV file in their current displayed order.",
            "Creates members.csv with name, phone, email, address and tags columns.",
            "The default path is addressbook.csv beside the JAR, or in the working directory during development. "
                    + "Packaged builds use only paths inside the JAR folder. Existing destination files are "
                    + "overwritten. Tags are sorted and joined with semicolons. The roster and current search "
                    + "stay unchanged. Run list first to export everyone. Hidden members are excluded and counted "
                    + "in the result; an empty displayed list writes only the header.",
            "Use an output path ending in .csv (ignoring letter case), with an existing writable parent directory. "
                    + "Double quotes are not allowed in export paths; spaces need no surrounding quotes. "
                    + "The active roster, preferences, application lock, and running JAR "
                    + "cannot be export destinations. "
                    + "An invalid path or a failed write reports an error and preserves any previous export."),
    IMPORT("Manage members", "import", "Import members from CSV", "import p/FILE_PATH",
            "import p/\"data/member list.csv\"",
            "Validates a UTF-8 CSV file and appends member records without overwriting existing members.",
            "Imports valid new members, skips duplicate contacts and restores the complete roster.",
            "The header must contain name, phone, email, address and tags exactly once, in any order. "
                    + "Duplicate names ignore case and repeated spaces when other contact fields match; "
                    + "tags are not merged. "
                    + "Names keep their formatting. Use edit to update a skipped contact. "
                    + "Quote paths containing whitespace. "
                    + "Packaged builds read files only inside the JAR folder. Values in the tags column are "
                    + "separated with semicolons.",
            "Supply one readable .csv path after p/. The file must be at most 10 MiB with at most 10,000 member rows. "
                    + "Split larger files before importing. A missing file reports its resolved path; "
                    + "check the file name and location. Invalid files do not change the roster. "
                    + "If saving fails, the imported batch remains unsaved in this session. "
                    + "Fix storage, then reapply an existing field with edit to save it; "
                    + "use clear for an empty roster. Exiting before a successful retry loses the batch."),
    DELETE("Remove records", "delete", "Delete one member", "delete INDEX", "delete 2",
            "Permanently removes the member at a displayed list number after you confirm the deletion.",
            "Asks you to confirm deleting the second member in the current list. Typing yes deletes them and shows "
                    + "their name and tags after saving.",
            "Search first if needed, then check the number and contact details. The confirmation names the member "
                    + "and their number. Only yes, in lowercase, confirms. y, Y or YES keeps the prompt and "
                    + "reminds you; any other input "
                    + "cancels without running that input. There is no undo after confirming.",
            "Supply one positive number shown in the current list. Zero, negative numbers and missing members "
                    + "are rejected. If saving fails, the deletion remains in this session and the command box clears; "
                    + "do not repeat the deletion to retry saving."),
    CLEAR("Remove records", "clear", "Clear the whole roster", "clear", "clear",
            "Permanently removes every member after confirmation, including members hidden by a search or filter.",
            "For a non-empty roster, first shows the total member count and asks you to confirm with yes. "
                    + "After confirmation and saving, the result reports how many members were cleared, "
                    + "or confirms that an already empty roster needed no changes.",
            "Only yes, in lowercase, confirms. y, Y or YES keeps the prompt and reminds you; "
                    + "any other input cancels without running "
                    + "that input. There is no undo after confirmation. "
                    + "An already-empty roster saves immediately without confirmation. "
                    + "Back up records you may need later.",
            "Extra arguments are rejected. If saving fails, the previous roster and displayed list are restored. "
                    + "No members were removed; fix the storage problem before retrying clear."),
    HELP("Help & session", "help", "Open this command guide", "help [COMMAND]", "help edit",
            "Opens the complete offline guide, or instructions for one available command.",
            "Shows the edit command's purpose, syntax, example and common errors.",
            "Use help for all commands. F1 also opens the guide; Escape closes it. No internet connection is needed.",
            "Use one lowercase command name. Unknown topics and extra topics are rejected; help shows the choices."),
    EXIT("Help & session", "exit", "Finish your session", "exit", "exit",
            "Closes TrackCall. Successful changes have already been saved automatically.",
            "The main window and help window close.",
            "Exit does not retry a failed save. Resolve any storage error before leaving if you need to keep changes.",
            "Use the lowercase command exit with no extra arguments.");

    private final String category;
    private final String keyword;
    private final String title;
    private final String syntax;
    private final String example;
    private final String purpose;
    private final String expectedResult;
    private final String notes;
    private final String errors;

    CommandHelp(String category, String keyword, String title, String syntax, String example, String purpose,
            String expectedResult, String notes, String errors) {
        this.category = category;
        this.keyword = keyword;
        this.title = title;
        this.syntax = syntax;
        this.example = example;
        this.purpose = purpose;
        this.expectedResult = expectedResult;
        this.notes = notes;
        this.errors = errors;
    }

    /**
     * Reuses the field constraints so add and edit explain the same accepted values as validation errors.
     */
    private static String memberFieldRules() {
        return Name.MESSAGE_CONSTRAINTS + ".\n" + Phone.MESSAGE_CONSTRAINTS + ".\n"
                + "Phone formatting is preserved; country codes are not inferred.\n"
                + "Within an address, A/ followed by a digit stays literal, as in a/Block A/12.\n"
                + Email.MESSAGE_CONSTRAINTS + "\n" + Tag.MESSAGE_CONSTRAINTS
                + ". Tag matching ignores letter case.";
    }

    /**
     * Finds help for an exact, lowercase command keyword.
     */
    public static Optional<CommandHelp> find(String keyword) {
        for (CommandHelp entry : values()) {
            if (entry.keyword.equals(keyword)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the category used to group related commands.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the lowercase command keyword.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns the short description shown in the card heading.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the command syntax with parameter placeholders.
     */
    public String getSyntax() {
        return syntax;
    }

    /**
     * Returns the complete example accepted by the command parser.
     */
    public String getExample() {
        return example;
    }

    /**
     * Returns the user-facing purpose of the command.
     */
    public String getPurpose() {
        return purpose;
    }

    /**
     * Returns the expected result of the example.
     */
    public String getExpectedResult() {
        return expectedResult;
    }

    /**
     * Returns the usage details and precautions.
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Returns the common input errors and recovery guidance.
     */
    public String getErrors() {
        return errors;
    }
}
