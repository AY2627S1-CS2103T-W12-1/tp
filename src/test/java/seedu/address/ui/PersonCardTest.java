package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Label;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class PersonCardTest {

    @BeforeAll
    public static void setUpJavaFx() throws InterruptedException {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.startup(startupLatch::countDown);
        startupLatch.await();
    }

    @AfterAll
    public static void tearDownJavaFx() {
        Platform.exit();
    }

    @Test
    public void constructor_personWithRemark_displaysRemark() throws Exception {
        String expectedRemark = "Likes baseball";
        Person person = new PersonBuilder(ALICE).withRemark(expectedRemark).build();
        FutureTask<String> createCard = new FutureTask<>(() -> {
            PersonCard personCard = new PersonCard(person, 1);
            Label remarkLabel = (Label) personCard.getRoot().lookup("#remark");
            return remarkLabel.getText();
        });

        Platform.runLater(createCard);

        assertEquals(expectedRemark, createCard.get());
    }
}
