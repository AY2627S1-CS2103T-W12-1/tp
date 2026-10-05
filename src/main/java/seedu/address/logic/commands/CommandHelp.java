package seedu.address.logic.commands;

import java.util.Optional;

/**
 * Offline help for the commands implemented by TrackCall. Planned commands do not belong in this catalogue.
 */
public enum CommandHelp {
    LIST("Browse members", "list", "Show the full roster", "list", "list",
            "Displays every member and their current list number.",
            "The complete roster is shown, including members hidden by a previous find.",
            "Use list before choosing a member by number. List numbers can change after an edit or deletion.",
            "Use the lowercase command list with no extra arguments."),
    SORT("Browse members", "sort", "Sort members by name", "sort", "sort",
            "Orders the displayed members by full name from A to Z, ignoring letter case.",
            "Shows the current search results alphabetically without changing the saved roster.",
            "Sorting stays active until list or a restart. Check list numbers again before editing or deleting.",
            "Use sort with no extra arguments. Use list to restore the complete roster in stored order."),
    FIND("Browse members", "find", "Find a member by name", "find KEYWORD [MORE_KEYWORDS]", "find Alice Tan",
            "Searches all members for any complete name word, ignoring letter case.",
            "Shows members whose names contain Alice or Tan. No match produces an empty list.",
            "A new search replaces the previous search. Jo does not match John; use list to show everyone again.",
            "Supply at least one name word. Searches use names, not phone numbers or tags."),
    ADD("Manage members", "add", "Add a member",
            "add n/NAME p/PHONE e/EMAIL a/ADDRESS [t/TAG]...",
            "add n/Alice Tan p/91234567 e/alice@example.com a/12 Orchard Road t/committee",
            "Creates a member with a name, phone, email and address. Tags are optional.",
            "Adds Alice Tan and shows the complete roster after the record is saved.",
            "Fields can appear in any order. Use one t/ prefix per tag; tag names contain letters and digits.",
            "Include all four required fields once. Phone numbers need at least three digits. "
                    + "An existing member with exactly the same name is rejected. If saving fails, the member remains "
                    + "in this session and the command box clears; follow the recovery guidance before exiting."),
    EDIT("Manage members", "edit", "Update a member",
            "edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...",
            "edit 1 p/98765432 t/committee t/year1",
            "Updates the member at a displayed list number. Fields you leave out stay unchanged.",
            "Changes member 1's phone and replaces their tags with committee and year1, then shows all members.",
            "Give the full tag set you want to keep. Use edit 1 t/ to remove all tags from member 1.",
            "Use a current positive list number and supply at least one field. Invalid values or a duplicate name "
                    + "are rejected. If saving fails, the edit remains in this session and the command box clears; "
                    + "follow the recovery guidance before exiting."),
    DELETE("Remove records", "delete", "Delete one member", "delete INDEX", "delete 2",
            "Permanently removes the member at a displayed list number.",
            "Deletes the second member in the current list and shows their details after saving.",
            "Search first if needed, then check the number and contact details. There is no undo or confirmation.",
            "Supply one positive number shown in the current list. Zero, negative numbers and missing members "
                    + "are rejected. If saving fails, the deletion remains in this session and the command box clears; "
                    + "do not repeat the deletion to retry saving."),
    CLEAR("Remove records", "clear", "Clear the whole roster", "clear", "clear",
            "Permanently removes every member, including members hidden by a search.",
            "The roster becomes empty after saving. The result reports how many members were cleared, "
                    + "or confirms that an already empty roster needed no changes.",
            "There is no undo or confirmation. Back up your data file before clearing records you may need later.",
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
