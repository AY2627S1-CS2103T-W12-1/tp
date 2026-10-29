package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import seedu.address.model.person.Person;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/** Checks how {@code PersonListPanel} notifies its selection listener. */
public class PersonListPanelTest {

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @Test
    public void setOnSelectionChanged_nullListener_throwsNullPointerException() throws Exception {
        onJavaFxThread(() -> {
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList());
            assertThrows(NullPointerException.class, () -> panel.setOnSelectionChanged(null));
        });
    }

    @Test
    public void clearSelection_withListener_notifiesListener() throws Exception {
        onJavaFxThread(() -> {
            Person person = new PersonBuilder().build();
            PersonListPanel panel = new PersonListPanel(FXCollections.observableArrayList(person));
            int[] notifications = {0};
            panel.setOnSelectionChanged(() -> notifications[0]++);

            panel.clearSelection();

            assertEquals(1, notifications[0]);
        });
    }
}
