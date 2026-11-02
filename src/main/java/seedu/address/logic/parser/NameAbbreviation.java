package seedu.address.logic.parser;

import java.util.regex.Pattern;

/** Recognises the standalone a/l and a/p abbreviations that may appear inside a member name. */
final class NameAbbreviation {

    private static final Pattern ABBREVIATION = Pattern.compile("(?i)a/[lp](?=\\p{javaWhitespace}|$)");

    private NameAbbreviation() {
    }

    /** Returns true if a standalone a/l or a/p, in any letter case, starts at {@code index} of {@code args}. */
    static boolean startsAt(String args, int index) {
        return ABBREVIATION.matcher(args).region(index, args.length()).lookingAt();
    }
}
