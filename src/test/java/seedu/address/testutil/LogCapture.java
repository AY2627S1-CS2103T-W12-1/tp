package seedu.address.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;

/**
 * Temporarily records the INFO messages logged by one class's {@code LogsCenter} logger.
 * Use it in a try-with-resources statement so the handler is always removed afterwards.
 */
public class LogCapture extends Handler implements AutoCloseable {
    private final String loggerName;
    private final Logger parentLogger;
    private final List<String> infoMessages = new ArrayList<>();

    /**
     * Starts recording the INFO messages logged by the logger of {@code clazz}.
     */
    public LogCapture(Class<?> clazz) {
        Logger logger = LogsCenter.getLogger(clazz);
        loggerName = logger.getName();
        // Attaches to the parent, as LogsCenter removes the class logger's own handlers whenever it is requested.
        parentLogger = logger.getParent();
        parentLogger.addHandler(this);
    }

    /**
     * Returns the INFO messages recorded so far, in logging order.
     */
    public List<String> getInfoMessages() {
        return List.copyOf(infoMessages);
    }

    @Override
    public void publish(LogRecord logRecord) {
        if (loggerName.equals(logRecord.getLoggerName()) && logRecord.getLevel() == Level.INFO) {
            infoMessages.add(logRecord.getMessage());
        }
    }

    @Override
    public void flush() {
        // Nothing is buffered.
    }

    @Override
    public void close() {
        parentLogger.removeHandler(this);
    }
}
