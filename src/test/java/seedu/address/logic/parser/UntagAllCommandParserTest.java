package seedu.address.logic.parser;

import seedu.address.logic.commands.UntagAllCommand;
import seedu.address.model.tag.Tag;

public class UntagAllCommandParserTest extends BulkTagCommandParserTest<UntagAllCommand> {

    @Override
    protected Parser<UntagAllCommand> getParser() {
        return new UntagAllCommandParser();
    }

    @Override
    protected UntagAllCommand createCommand(Tag tag) {
        return new UntagAllCommand(tag);
    }

    @Override
    protected String getUsage() {
        return UntagAllCommand.MESSAGE_USAGE;
    }
}
