package seedu.address.logic.parser;

import seedu.address.logic.commands.TagAllCommand;
import seedu.address.model.tag.Tag;

public class TagAllCommandParserTest extends BulkTagCommandParserTest<TagAllCommand> {

    @Override
    protected Parser<TagAllCommand> getParser() {
        return new TagAllCommandParser();
    }

    @Override
    protected TagAllCommand createCommand(Tag tag) {
        return new TagAllCommand(tag);
    }

    @Override
    protected String getUsage() {
        return TagAllCommand.MESSAGE_USAGE;
    }
}
