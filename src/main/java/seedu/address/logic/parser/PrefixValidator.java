package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.Set;

import seedu.address.logic.parser.exceptions.ParseException;

/** Validates the member field prefixes shared by add and edit commands. */
final class PrefixValidator {

    private static final Set<String> SUPPORTED_PREFIXES = Set.of(PREFIX_NAME.getPrefix(), PREFIX_PHONE.getPrefix(),
            PREFIX_EMAIL.getPrefix(), PREFIX_ADDRESS.getPrefix(), PREFIX_TAG.getPrefix());

    private PrefixValidator() {
    }

    /**
     * Reserves whitespace-delimited ASCII single-letter slash prefixes for member fields.
     * The standalone care-of abbreviation (c/o, case-insensitive) remains literal text;
     * A/ followed by a digit is also literal inside an address, as in Block A/12.
     * URLs, numeric slashes and slashes embedded in words are not field prefixes.
     *
     * @param args Arguments to validate before tokenization.
     * @param commandUsage Usage message for the command being parsed.
     * @throws ParseException if an unsupported member field prefix is present.
     */
    static void verifySupportedPrefixes(String args, String commandUsage) throws ParseException {
        boolean inAddress = false;
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
            if (SUPPORTED_PREFIXES.contains(prefix)) {
                inAddress = prefix.equals(PREFIX_ADDRESS.getPrefix());
                continue;
            }
            boolean isAddressUnit = inAddress && letter == 'A' && i + 2 < args.length()
                    && args.charAt(i + 2) >= '0' && args.charAt(i + 2) <= '9';
            if (!isCareOf && !isAddressUnit) {
                throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, commandUsage));
            }
        }
    }
}
