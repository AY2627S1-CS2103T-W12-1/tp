package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.text.Normalizer;
import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    private Button copyAddressButton;
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
        requireNonNull(person);
        selectedPerson = person;
        displayedIndex.setText("MEMBER " + index);
        name.setText(Normalizer.normalize(person.getName().fullName, Normalizer.Form.NFC));
        phone.setText(person.getPhone().value);
        email.setText(person.getEmail().value);
        String addressValue = person.getAddress().value;
        address.setText(addressValue.isEmpty() ? "(none)" : addressValue);
        copyAddressButton.setDisable(addressValue.isEmpty());
        showTags(person);
        emptyState.setVisible(false);
        detailsScroll.setVisible(true);
        detailsScroll.setManaged(true);
        detailsScroll.setVvalue(0);
    }

    /** Lists the member's tags alphabetically, or "None" if the member has no tags. */
    private void showTags(Person person) {
        tags.getChildren().clear();
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(createTagLabel(tag.tagName)));
        if (tags.getChildren().isEmpty()) {
            tags.getChildren().add(new Label("None"));
        }
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
        copyAddressButton.setDisable(true);
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

    /** Places {@code value} on the system clipboard as plain text. */
    private void copy(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
