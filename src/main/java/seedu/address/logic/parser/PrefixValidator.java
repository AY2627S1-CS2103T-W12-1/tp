package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.Set;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;

/** Validates the member field prefixes shared by add and edit commands. */
final class PrefixValidator {

    static final String MESSAGE_AMBIGUOUS_NAME_ABBREVIATION = "a/l or a/p directly after a name could be part of "
            + "the name or the start of the address. For a name such as Ravi A/L Kumar, write A/L or A/P "
            + "in uppercase. For an address, move a/ so it does not directly follow the name.";

    private static final Set<String> SUPPORTED_PREFIXES = Set.of(PREFIX_NAME.getPrefix(), PREFIX_PHONE.getPrefix(),
            PREFIX_EMAIL.getPrefix(), PREFIX_ADDRESS.getPrefix(), PREFIX_TAG.getPrefix());

    /** Matches a standalone a/l or a/p in any letter case. */
    private static final Pattern NAME_ABBREVIATION = Pattern.compile("(?i)a/[lp](?=\\p{javaWhitespace}|$)");

    private PrefixValidator() {
    }

    /**
     * Reserves whitespace-delimited ASCII single-letter slash prefixes for member fields.
     * Standalone c/o, s/o and d/o abbreviations (case-insensitive) remain literal text, as do uppercase A/L and A/P
     * inside a name. URLs, numeric slashes and slashes embedded in words are not field prefixes.
     *
     * @param args Arguments to validate before tokenization.
     * @param commandUsage Usage message for the command being parsed.
     * @throws ParseException if an unsupported member field prefix is present, or if a lowercase a/l or a/p
     *     directly follows a name, where it could be either a name abbreviation or an address.
     */
    static void verifySupportedPrefixes(String args, String commandUsage) throws ParseException {
        String currentPrefix = "";
        for (int i = 0; i + 1 < args.length(); i++) {
            char letter = args.charAt(i);
            boolean isAsciiLetter = (letter >= 'a' && letter <= 'z') || (letter >= 'A' && letter <= 'Z');
            if (!isAsciiLetter || args.charAt(i + 1) != '/'
                    || (i > 0 && !Character.isWhitespace(args.charAt(i - 1)))) {
                continue;
            }
            boolean isCareOf = args.regionMatches(true, i, "c/o", 0, 3)
                    && (i + 3 == args.length() || Character.isWhitespace(args.charAt(i + 3)));
            String prefix = args.substring(i, i + 2);
            if (currentPrefix.equals(PREFIX_NAME.getPrefix())
                    && NAME_ABBREVIATION.matcher(args).region(i, args.length()).lookingAt()) {
                if (letter == 'a') {
                    throw new ParseException(MESSAGE_AMBIGUOUS_NAME_ABBREVIATION);
                }
                continue;
            }
            if (SUPPORTED_PREFIXES.contains(prefix)) {
                currentPrefix = prefix;
                continue;
            }
            // Preserve s/o and d/o.
            if (!isCareOf && !args.substring(i).matches("(?is)[sd]/o(?:\\p{javaWhitespace}.*)?")) {
                throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, commandUsage));
            }
        }
    }
}
