package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.Person;

/**
 * Panel containing the list of persons.
 */
public class PersonListPanel extends UiPart<Region> {
    private static final String FXML = "PersonListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(PersonListPanel.class);

    @FXML
    private ListView<Person> personListView;

    private Runnable onSelectionChanged = () -> { };

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(ObservableList<Person> personList) {
        super(FXML);
        personListView.setItems(personList);
        personListView.setCellFactory(listView -> new PersonListViewCell());
        personListView.getSelectionModel().selectedItemProperty().addListener((unused, oldPerson, newPerson) ->
                onSelectionChanged.run());
        // JavaFX returns at both arrow-key boundaries when the sole row is focused but not selected.
        personListView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!event.isShiftDown() && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()
                    && (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN
                            || event.getCode() == KeyCode.KP_UP || event.getCode() == KeyCode.KP_DOWN)
                    && personListView.getSelectionModel().isEmpty() && personListView.getItems().size() == 1) {
                personListView.getSelectionModel().selectFirst();
                event.consume();
            }
        });
    }

    /** Notifies the details panel when an explicit roster selection changes. */
    public void setOnSelectionChanged(Runnable listener) {
        onSelectionChanged = requireNonNull(listener);
    }

    public Person getSelectedPerson() {
        return personListView.getSelectionModel().getSelectedItem();
    }

    public int getSelectedIndex() {
        return personListView.getSelectionModel().getSelectedIndex();
    }

    /** Removes any revealed contact details before a command can change the roster. */
    public void clearSelection() {
        personListView.getSelectionModel().clearSelection();
        onSelectionChanged.run();
    }

    /** Requests keyboard focus for the roster so arrow keys can select members. */
    public void focusList() {
        personListView.requestFocus();
    }

    public boolean isListFocused() {
        return personListView.isFocused();
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Person} using a {@code PersonCard}.
     */
    class PersonListViewCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);

            if (empty || person == null) {
                setGraphic(null);
                setText(null);
            } else {
                Region card = new PersonCard(person, getIndex() + 1).getRoot();
                card.prefWidthProperty().bind(widthProperty().subtract(16));
                card.setMinWidth(0);
                setGraphic(card);
            }
        }
    }

}
