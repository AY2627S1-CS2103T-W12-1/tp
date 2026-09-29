package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    private static final String FXML = "ResultDisplay.fxml";

    @FXML
    private TextArea resultDisplay;

    /**
     * Creates a wrapped, selectable display for command results and recovery guidance.
     */
    public ResultDisplay() {
        super(FXML);
        setFeedbackToUser("Welcome to TrackCall.\nType help to see all commands, or help add to add a member.");
    }

    /**
     * Shows new feedback from its first line, even if the previous result was scrolled.
     */
    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
        resultDisplay.positionCaret(0);
        resultDisplay.setScrollTop(0);
    }

}
