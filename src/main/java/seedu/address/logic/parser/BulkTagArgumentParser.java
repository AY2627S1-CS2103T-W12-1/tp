package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.List;
import java.util.stream.Stream;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.tag.Tag;

/**
 * Parses and validates the {@code t/TAG} arguments of the tag commands: {@code tag}, {@code untag},
 * {@code tagall}, {@code untagall} and {@code renametag}.
 */
public final class BulkTagArgumentParser {
    public static final String MESSAGE_EMPTY_TAG = "Tag cannot be empty.";
    public static final String MESSAGE_INVALID_TAG = Tag.MESSAGE_CONSTRAINTS + ".";
    public static final String MESSAGE_UNSUPPORTED_CHARACTER = "Tag contains unsupported character \"%s\". "
            + "Use letters (A-Z, a-z), digits (0-9), or internal hyphens (-), e.g. year-1.";
    public static final String MESSAGE_WHITESPACE =
            "Tag cannot contain spaces. Use a hyphen instead, e.g. year-1.";
    public static final String MESSAGE_LEADING_HYPHEN =
            "Tag cannot start with a hyphen (-). Start with a letter or digit, e.g. year-1.";
    public static final String MESSAGE_TRAILING_HYPHEN =
            "Tag cannot end with a hyphen (-). End with a letter or digit, e.g. year-1.";
    public static final String MESSAGE_REPEATED_HYPHENS =
            "Tag cannot contain consecutive hyphens (--). Use a single hyphen, e.g. year-1.";
    public static final String MESSAGE_MISSING_PREFIX = "Missing t/ prefix. Supply tags using t/TAG.";
    public static final String MESSAGE_UNEXPECTED_TEXT = "Unexpected text before t/: \"%s\".";
    public static final String MESSAGE_UNSUPPORTED_FIELD =
            "Field %s is not supported. This command accepts only t/TAG parameters.";
    public static final String MESSAGE_TAG_TOO_LONG =
            "Tag is too long: %d characters. Maximum: " + Tag.MAX_LENGTH + ".";
    public static final String MESSAGE_MULTIPLE_TAGS =
            "Only one tag can be applied per command. Run the command once for each tag.";

    private BulkTagArgumentParser() {
        // Prevents instantiation of this utility class.
    }

    /**
     * Parses {@code args} into exactly one tag of 1 to 30 ASCII letters or digits,
     * with optional internal hyphens.
     * Surrounding whitespace around the tag is ignored.
     *
     * @param args Arguments following the command word.
     * @param usage Usage message of the calling command, shown when the {@code t/} prefix is missing,
     *         other text appears before it, or another member field prefix is present.
     * @throws ParseException If the prefix is missing, other text appears before it,
     *         another member field prefix is present, more than one tag is given,
     *         or the tag is empty or invalid.
     */
    public static Tag parse(String args, String usage) throws ParseException {
        requireAllNonNull(args, usage);
        List<String> tagValues = parseTagValues(args, usage);

        if (tagValues.isEmpty()) {
            throw new ParseException(MESSAGE_MISSING_PREFIX + "\n" + usage);
        }
        if (tagValues.size() > 1) {
            throw new ParseException(MESSAGE_MULTIPLE_TAGS);
        }

        return parseTagName(tagValues.get(0));
    }

    /**
     * Returns the trimmed {@code t/} values in {@code args}, in the order given.
     * Other member field prefixes are recognized so the error can name the unsupported field
     * instead of reading it as part of a tag.
     *
     * @param args Arguments following the command word.
     * @param usage Usage message of the calling command, shown when the arguments are malformed.
     * @throws ParseException If other text appears before the first prefix,
     *         or a {@code n/}, {@code p/}, {@code e/} or {@code a/} prefix is present.
     */
    public static List<String> parseTagValues(String args, String usage) throws ParseException {
        requireAllNonNull(args, usage);
        // A leading space lets the tokenizer recognize a prefix at the very start of the arguments.
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(" " + args, PREFIX_TAG, PREFIX_NAME,
                PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS);
        validateFieldPrefixes(argMultimap, usage);
        validatePreamble(argMultimap, usage);
        return argMultimap.getAllValues(PREFIX_TAG);
    }

    /** Rejects member fields that cannot be used by tag commands. */
    private static void validateFieldPrefixes(ArgumentMultimap arguments, String usage)
            throws ParseException {
        var unsupportedPrefix = Stream.of(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS)
                .filter(prefix -> arguments.getValue(prefix).isPresent())
                .findFirst();
        if (unsupportedPrefix.isPresent()) {
            throw new ParseException(String.format(MESSAGE_UNSUPPORTED_FIELD, unsupportedPrefix.get())
                    + "\n" + usage);
        }
    }

    /** Distinguishes a missing tag prefix from extra text before a supplied tag. */
    private static void validatePreamble(ArgumentMultimap arguments, String usage) throws ParseException {
        if (arguments.getPreamble().isEmpty()) {
            return;
        }
        String message = arguments.getAllValues(PREFIX_TAG).isEmpty() ? MESSAGE_MISSING_PREFIX
                : String.format(MESSAGE_UNEXPECTED_TEXT, arguments.getPreamble());
        throw new ParseException(message + "\n" + usage);
    }

    /**
     * Parses one trimmed {@code t/} value into a tag of 1 to 30 ASCII letters or digits,
     * with optional internal hyphens.
     *
     * @throws ParseException If {@code tagName} is empty or invalid.
     */
    public static Tag parseTagName(String tagName) throws ParseException {
        requireNonNull(tagName);
        if (tagName.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_TAG);
        }
        validateSupportedCharacters(tagName);
        validateTagLength(tagName);
        validateHyphenPlacement(tagName);
        // Defensive guard: keep the model authoritative if its rule changes before these diagnostics.
        if (!Tag.isValidTagName(tagName)) {
            throw new ParseException(MESSAGE_INVALID_TAG);
        }
        return new Tag(tagName);
    }

    /** Rejects overlong ASCII tags using the model limit; supported characters are checked first. */
    private static void validateTagLength(String tagName) throws ParseException {
        if (tagName.length() > Tag.MAX_LENGTH) {
            throw new ParseException(String.format(MESSAGE_TAG_TOO_LONG, tagName.length()));
        }
    }

    /** Rejects the first unsupported character and explains the accepted alternatives. */
    private static void validateSupportedCharacters(String tagName) throws ParseException {
        var unsupportedCharacter = tagName.codePoints()
                .filter(character -> !isSupportedCharacter(character))
                .findFirst();
        if (unsupportedCharacter.isPresent()) {
            int character = unsupportedCharacter.getAsInt();
            if (isSpace(character)) {
                throw new ParseException(MESSAGE_WHITESPACE);
            }
            throw new ParseException(String.format(MESSAGE_UNSUPPORTED_CHARACTER, describeCharacter(character)));
        }
    }

    /** Returns whether a code point is a space, tab, line break or other whitespace. */
    private static boolean isSpace(int character) {
        return Character.isWhitespace(character) || Character.isSpaceChar(character);
    }

    /**
     * Makes invisible control characters and the double quote readable inside the quoted error message.
     */
    private static String describeCharacter(int character) {
        if (Character.isISOControl(character) || Character.getType(character) == Character.FORMAT) {
            return String.format("U+%04X", character);
        }
        if (character == '"') {
            return "\\\"";
        }
        return new String(Character.toChars(character));
    }

    /** Returns whether a code point is an ASCII letter, digit or hyphen. */
    private static boolean isSupportedCharacter(int character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == '-';
    }

    /** Explains the first invalid hyphen position in an otherwise supported tag name. */
    private static void validateHyphenPlacement(String tagName) throws ParseException {
        if (tagName.startsWith("-")) {
            throw new ParseException(MESSAGE_LEADING_HYPHEN);
        }
        if (tagName.endsWith("-")) {
            throw new ParseException(MESSAGE_TRAILING_HYPHEN);
        }
        if (tagName.contains("--")) {
            throw new ParseException(MESSAGE_REPEATED_HYPHENS);
        }
    }

}
