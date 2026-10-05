package seedu.address.logic.commands;

import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stores the self-contained synopsis and topic entries displayed by {@link HelpCommand}. */
public final class HelpTopics {

    private static final Map<String, String> TOPICS = createTopics();

    private HelpTopics() {
        // Utility class.
    }

    /** Returns the complete command synopsis. */
    public static String getSummary() {
        StringBuilder summary = new StringBuilder(HelpCommand.SHOWING_HELP_MESSAGE).append("\n\n");
        TOPICS.forEach((command, topic) -> summary.append(command)
                .append(" - ")
                .append(firstLine(topic))
                .append("\n"));
        return summary.toString().trim();
    }

    /** Returns the Vim-like topic entry for a command. */
    public static String getTopic(String command) {
        return "Showing help for " + command + ".\n\n"
                + "==============================================================================\n"
                + command.toUpperCase() + "\n"
                + "==============================================================================\n"
                + TOPICS.get(command);
    }

    /** Returns true if a help topic exists for the command. */
    public static boolean hasTopic(String command) {
        return TOPICS.containsKey(command);
    }

    private static String firstLine(String topic) {
        return topic.substring(0, topic.indexOf('\n'));
    }

    private static Map<String, String> createTopics() {
        Map<String, String> topics = new LinkedHashMap<>();
        topics.put("add", "add n/NAME p/PHONE e/EMAIL a/ADDRESS [t/TAG]...\n"
                + "Adds a person to the address book.\n"
                + "Required fields: name, phone, email, and address.\n"
                + "Optional fields: one or more tags.");
        topics.put("edit", "edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...\n"
                + "Edits the person at INDEX in the displayed list.\n"
                + "At least one field must be supplied; existing values are overwritten.");
        topics.put("delete", "delete INDEX\n"
                + "Deletes the person at INDEX in the displayed list.\n"
                + "INDEX must be a positive integer.");
        topics.put("export", "export [FILEPATH]\n"
                + "Exports all people to a CSV file.\n"
                + "FILEPATH defaults to addressbook.csv in the current directory.\n"
                + "The parent directory must exist. An existing destination file is overwritten.");
        topics.put("find", "find KEYWORD [MORE_KEYWORDS]...\n"
                + "Displays people whose names contain any supplied keyword.\n"
                + "Name matching is case-insensitive.");
        topics.put("list", "list\n"
                + "Displays all people in the address book.");
        topics.put("clear", "clear\n"
                + "Removes all people from the address book.");
        topics.put("exit", "exit\n"
                + "Closes the application.");
        topics.put("help", "help [COMMAND]\n"
                + "Displays this synopsis or the help topic for one command.\n"
                + "COMMAND is case-sensitive and accepts exactly one valid command keyword.");
        return unmodifiableMap(topics);
    }
}
