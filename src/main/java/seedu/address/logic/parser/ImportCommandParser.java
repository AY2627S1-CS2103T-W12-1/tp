package seedu.address.logic.parser;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.ImportCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses arguments for the {@code import} command.
 */
public class ImportCommandParser implements Parser<ImportCommand> {

    public static final String MESSAGE_INVALID_FORMAT = "Invalid command format. Usage: " + ImportCommand.MESSAGE_USAGE;
    public static final String MESSAGE_EMPTY_PATH = "File path cannot be empty.";
    public static final String MESSAGE_NON_CSV_FILE = "File must have a .csv extension.";
    public static final String MESSAGE_UNMATCHED_QUOTES = "File path has unmatched quotes.";
    public static final String MESSAGE_REPEATED_PATH = "Parameter p/ may only be specified once.";
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Unknown parameter: %s";

    private static final Pattern PARAMETER_PREFIX = Pattern.compile("(?i)^([a-z][a-z0-9-]*/)");

    @Override
    public ImportCommand parse(String args) throws ParseException {
        String trimmedArgs = args.trim();
        if (trimmedArgs.isEmpty()) {
            throw new ParseException(MESSAGE_INVALID_FORMAT);
        }
        if (!trimmedArgs.startsWith("p/")) {
            throw getUnknownParameterOrInvalidFormatError(trimmedArgs);
        }

        String rawPath = trimmedArgs.substring(2).trim();
        if (rawPath.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_PATH);
        }

        String pathValue = extractPath(rawPath);
        if (pathValue.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_PATH);
        }
        if (!pathValue.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ParseException(MESSAGE_NON_CSV_FILE);
        }

        try {
            return new ImportCommand(Path.of(pathValue));
        } catch (InvalidPathException e) {
            throw new ParseException(String.format(ImportCommand.MESSAGE_FAILURE, e.getMessage()), e);
        }
    }

    private static String extractPath(String rawPath) throws ParseException {
        if (rawPath.startsWith("\"")) {
            int closingQuote = rawPath.indexOf('"', 1);
            if (closingQuote == -1) {
                throw new ParseException(MESSAGE_UNMATCHED_QUOTES);
            }
            String path = rawPath.substring(1, closingQuote);
            String trailingArguments = rawPath.substring(closingQuote + 1).trim();
            if (!trailingArguments.isEmpty()) {
                throw getExtraArgumentsError(trailingArguments);
            }
            return path;
        }

        if (rawPath.indexOf('"') >= 0) {
            throw new ParseException(MESSAGE_UNMATCHED_QUOTES);
        }
        int whitespaceIndex = findWhitespace(rawPath);
        if (whitespaceIndex >= 0) {
            throw getExtraArgumentsError(rawPath.substring(whitespaceIndex).trim());
        }
        return rawPath;
    }

    private static int findWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static ParseException getExtraArgumentsError(String arguments) {
        for (String token : arguments.split("\\s+")) {
            if (token.startsWith("p/")) {
                return new ParseException(MESSAGE_REPEATED_PATH);
            }
            Matcher matcher = PARAMETER_PREFIX.matcher(token);
            if (matcher.find()) {
                return new ParseException(String.format(MESSAGE_UNKNOWN_PARAMETER, matcher.group(1)));
            }
        }
        return new ParseException(MESSAGE_INVALID_FORMAT);
    }

    private static ParseException getUnknownParameterOrInvalidFormatError(String arguments) {
        Matcher matcher = PARAMETER_PREFIX.matcher(arguments);
        if (matcher.find()) {
            return new ParseException(String.format(MESSAGE_UNKNOWN_PARAMETER, matcher.group(1)));
        }
        return new ParseException(MESSAGE_INVALID_FORMAT);
    }
}
