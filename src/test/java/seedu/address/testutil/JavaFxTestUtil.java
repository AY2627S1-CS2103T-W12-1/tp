package seedu.address.testutil;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/** Starts one JavaFX toolkit per test process and runs UI assertions on its application thread. */
public final class JavaFxTestUtil {
    private static boolean started;

    private JavaFxTestUtil() {
    }

    /** Starts JavaFX once, keeping it alive between tests that close their windows. */
    public static synchronized void start() throws InterruptedException {
        if (started) {
            return;
        }
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        });
        assertTrue(ready.await(20, TimeUnit.SECONDS), "JavaFX did not start");
        started = true;
    }

    /** Runs assertions on the JavaFX thread and propagates failures to JUnit. */
    public static void onJavaFxThread(Runnable assertions) throws Exception {
        FutureTask<Void> task = new FutureTask<>(assertions, null);
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }
}
