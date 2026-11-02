package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Checks that compact roster rows show identity and tags without revealing contact fields.
 */
public class PersonCardTest {

    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        JavaFxTestUtil.start();
    }

    @Test
    public void card_memberWithTags_showsNameAndSortedTagsOnly() throws Exception {
        onJavaFxThread(() -> {
            Person person = new PersonBuilder().withTags("year1", "Committee", "2026").build();
            Region card = layoutCard(person, 3, 700);
            assertEquals("3", ((Label) card.lookup("#id")).getText());
            assertEquals(person.getName().fullName, ((Label) card.lookup("#name")).getText());
            assertNull(card.lookup("#phone"));
            assertNull(card.lookup("#email"));
            assertNull(card.lookup("#address"));
            FlowPane tags = (FlowPane) card.lookup("#tags");
            assertTrue(tags.isVisible());
            assertEquals(List.of("2026", "Committee", "year1"), tags.getChildren().stream()
                    .map(node -> ((Label) node).getText()).toList());
        });
    }

    @Test
    public void card_nameWithCombiningMark_showsComposedName() throws Exception {
        onJavaFxThread(() -> {
            Region card = layoutCard(new PersonBuilder().withName("Ale\u0301x").build(), 1, 450);
            assertEquals("Al\u00e9x", ((Label) card.lookup("#name")).getText());
        });
    }

    @Test
    public void card_mixedScriptName_showsComposedNameAndKeepsStoredName() throws Exception {
        onJavaFxThread(() -> {
            // Greek word followed by CJK plus decomposed accents, the combination that made layout slow.
            String greek = "\u0395\u03bb\u03bb\u03b7\u03bd\u03b9\u03ba\u03ac";
            String decomposedName = greek + "\u754c\u754cE\u0301e\u0301".repeat(24);
            String composedName = greek + "\u754c\u754c\u00c9\u00e9".repeat(24);
            Person person = new PersonBuilder().withName(decomposedName).build();
            Region card = layoutCard(person, 1, 450);
            assertEquals(composedName, ((Label) card.lookup("#name")).getText());
            // Only the displayed text is normalized; the stored name keeps its original spelling.
            assertEquals(decomposedName, person.getName().fullName);
        });
    }

    @Test
    public void card_composedName_showsNameUnchanged() throws Exception {
        onJavaFxThread(() -> {
            Region card = layoutCard(new PersonBuilder().withName("Al\u00e9x").build(), 1, 450);
            assertEquals("Al\u00e9x", ((Label) card.lookup("#name")).getText());
        });
    }

    @Test
    public void card_markWithoutComposedForm_showsNameAsTyped() throws Exception {
        onJavaFxThread(() -> {
            // Devanagari has no precomposed form for these marks, so normalization cannot change them.
            String devanagariName = "\u0928\u092e\u0938\u094d\u0924\u0947";
            Region card = layoutCard(new PersonBuilder().withName(devanagariName).build(), 1, 450);
            assertEquals(devanagariName, ((Label) card.lookup("#name")).getText());
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
    public void card_narrowWidth_wrapsLongNameAndTagWithoutLosingText() throws Exception {
        onJavaFxThread(() -> {
            String nameText = "Alexandra Catherine Bernadette Longname";
            String tagText = "A".repeat(30);
            Person person = new PersonBuilder().withName(nameText).withTags(tagText).build();
            Region card = layoutCard(person, 1, 160);
            Label name = (Label) card.lookup("#name");
            Label tag = (Label) ((FlowPane) card.lookup("#tags")).getChildren().getFirst();

            assertEquals(nameText, name.getText());
            assertEquals(tagText, tag.getText());
            assertTrue(name.getHeight() > 2 * name.getFont().getSize(), "Name should occupy multiple lines");
            assertTrue(tag.getHeight() > 2 * tag.getFont().getSize(), "Long tag should occupy multiple lines");
            assertTrue(name.getWidth() < card.getWidth(), "Name should fit within the card");
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
