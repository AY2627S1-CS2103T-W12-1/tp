package seedu.address.ui;

import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import seedu.address.model.person.Person;

/** Displays one explicitly selected member's contact details and copy controls. */
public class MemberDetailsPanel extends UiPart<Region> {

    private static final String FXML = "MemberDetailsPanel.fxml";

    private Person selectedPerson;

    @FXML
    private Label emptyState;
    @FXML
    private ScrollPane detailsScroll;
    @FXML
    private Label displayedIndex;
    @FXML
    private Label name;
    @FXML
    private Label phone;
    @FXML
    private Label email;
    @FXML
    private Label address;
    @FXML
    private FlowPane tags;

    /** Starts with all contact details hidden until the user selects a roster row. */
    public MemberDetailsPanel() {
        super(FXML);
        tags.setMinHeight(Region.USE_PREF_SIZE);
        clear();
    }

    /** Reveals the selected member's details and their current displayed index. */
    public void showPerson(Person person, int index) {
        selectedPerson = person;
        displayedIndex.setText("MEMBER " + index);
        name.setText(person.getName().fullName);
        phone.setText(person.getPhone().value);
        email.setText(person.getEmail().value);
        address.setText(person.getAddress().value.isEmpty() ? "(none)" : person.getAddress().value);
        tags.getChildren().clear();
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(createTagLabel(tag.tagName)));
        if (tags.getChildren().isEmpty()) {
            tags.getChildren().add(new Label("None"));
        }
        emptyState.setVisible(false);
        detailsScroll.setVisible(true);
        detailsScroll.setManaged(true);
        detailsScroll.setVvalue(0);
    }

    /** Wraps long tag names within the details viewport instead of clipping their suffix. */
    private Label createTagLabel(String tagName) {
        Label label = new Label(tagName);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.maxWidthProperty().bind(tags.widthProperty());
        label.setTooltip(new Tooltip(tagName));
        return label;
    }

    /** Hides the selected member's details after deselection or a command. */
    public void clear() {
        selectedPerson = null;
        displayedIndex.setText("");
        name.setText("");
        phone.setText("");
        email.setText("");
        address.setText("");
        tags.getChildren().clear();
        detailsScroll.setVisible(false);
        detailsScroll.setManaged(false);
        emptyState.setVisible(true);
    }

    @FXML
    private void copyPhone() {
        copy(selectedPerson.getPhone().value);
    }

    @FXML
    private void copyEmail() {
        copy(selectedPerson.getEmail().value);
    }

    @FXML
    private void copyAddress() {
        copy(selectedPerson.getAddress().value);
    }

    private void copy(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
