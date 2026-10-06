package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.onJavaFxThread;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import seedu.address.model.person.Person;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/**
 * Checks member-card content and off-screen JavaFX layout without opening application windows.
 */
public class PersonCardTest {

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @Test
    public void card_memberWithTags_showsEveryFieldAndSortedTags() throws Exception {
        onJavaFxThread(() -> {
            Person person = new PersonBuilder().withTags("year1", "Committee", "2026").build();
            Region card = layoutCard(person, 3, 700);
            assertEquals("3", ((Label) card.lookup("#id")).getText());
            assertEquals(person.getName().fullName, ((Label) card.lookup("#name")).getText());
            assertEquals(person.getPhone().value, ((Label) card.lookup("#phone")).getText());
            assertEquals(person.getEmail().value, ((Label) card.lookup("#email")).getText());
            assertEquals(person.getAddress().value, ((Label) card.lookup("#address")).getText());
            FlowPane tags = (FlowPane) card.lookup("#tags");
            assertTrue(tags.isVisible());
            assertEquals(List.of("2026", "Committee", "year1"), tags.getChildren().stream()
                    .map(node -> ((Label) node).getText()).toList());
        });
    }

    @Test
    public void card_noTags_omitsEmptyTagRow() throws Exception {
        onJavaFxThread(() -> {
            Region card = layoutCard(new PersonBuilder().withTags().build(), 1, 400);
            FlowPane tags = (FlowPane) card.lookup("#tags");
            assertTrue(tags.getChildren().isEmpty());
            assertFalse(tags.isVisible());
            assertFalse(tags.isManaged());
        });
    }

    @Test
    public void card_narrowWidth_wrapsLongAddressAndTagWithoutLosingText() throws Exception {
        onJavaFxThread(() -> {
            String addressText = "12 Orchard Road, Membership Office, Level 3, Community Building beside the "
                    + "main entrance, Singapore, Visiting hours Monday to Friday, please contact reception for access";
            String tagText = "A".repeat(30);
            Person person = new PersonBuilder().withAddress(addressText).withTags(tagText).build();
            Region card = layoutCard(person, 1, 160);
            Label address = (Label) card.lookup("#address");
            Label tag = (Label) ((FlowPane) card.lookup("#tags")).getChildren().getFirst();

            assertEquals(addressText, address.getText());
            assertEquals(tagText, tag.getText());
            assertTrue(address.getHeight() > 2 * address.getFont().getSize(), "Address should occupy multiple lines");
            assertTrue(tag.getHeight() > 2 * tag.getFont().getSize(), "Long tag should occupy multiple lines");
            assertTrue(address.getWidth() < card.getWidth(), "Address should fit within the card");
            assertTrue(tag.getWidth() < card.getWidth(), "Tag should fit within the card");
        });
    }

    /** Creates and lays out a card in an off-screen scene at the requested available width. */
    private Region layoutCard(Person person, int index, double width) {
        Region card = new PersonCard(person, index).getRoot();
        card.setPrefWidth(width);
        StackPane container = new StackPane(card);
        Scene scene = new Scene(container, width, 900);
        scene.getStylesheets().add(getClass().getResource("/view/LightTheme.css").toExternalForm());
        container.applyCss();
        container.resize(width, 900);
        container.layout();
        return card;
    }

}
