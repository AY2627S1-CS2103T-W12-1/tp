package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.testutil.SerializableTestClass;
import seedu.address.testutil.TestUtil;

/**
 * Tests JSON Read and Write
 */
public class JsonUtilTest {

    private static final Path SERIALIZATION_FILE = TestUtil.getFilePathInSandboxFolder("serialize.json");

    @TempDir
    public Path directory;

    @Test
    public void readJsonFile_malformedPrivateData_keepsValuesOutOfLogsAndExceptionChain() throws Exception {
        // Initialize the log hierarchy before attaching the capture handler.
        JsonUtil.toJsonString(new SerializableTestClass());
        Path file = directory.resolve("private.json");
        Files.writeString(file, "{\"name\":\"private.member@example.com\",\"listOfLocalDateTimes\":[ }");
        StringBuilder records = new StringBuilder();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.append(new SimpleFormatter().format(record));
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger logger = Logger.getLogger("ab3");
        logger.addHandler(capture);
        try {
            DataLoadingException exception = assertThrows(DataLoadingException.class, () ->
                    JsonUtil.readJsonFile(file, SerializableTestClass.class));
            StringWriter trace = new StringWriter();
            exception.printStackTrace(new PrintWriter(trace));
            assertFalse(records.toString().contains("private.member@example.com"));
            assertFalse(trace.toString().contains("private.member@example.com"));
            assertTrue(exception.getMessage().contains("line 1, column"));
        } finally {
            logger.removeHandler(capture);
        }
    }

    @Test
    public void fromJsonString_invalidFieldType_keepsPrivateValueOutOfDiagnostic() {
        IOException exception = assertThrows(IOException.class, () -> JsonUtil.fromJsonString(
                "{\"listOfLocalDateTimes\":[\"private.member@example.com\"]}", SerializableTestClass.class));
        StringWriter trace = new StringWriter();
        exception.printStackTrace(new PrintWriter(trace));
        assertFalse(trace.toString().contains("private.member@example.com"));
        assertTrue(exception.getMessage().contains("Invalid JSON data"));
    }

    @Test
    public void serializeObjectToJsonFile_noExceptionThrown() throws IOException {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        serializableTestClass.setTestValues();

        JsonUtil.serializeObjectToJsonFile(SERIALIZATION_FILE, serializableTestClass);

        assertEquals(FileUtil.readFromFile(SERIALIZATION_FILE), SerializableTestClass.JSON_STRING_REPRESENTATION);
    }

    @Test
    public void deserializeObjectFromJsonFile_noExceptionThrown() throws IOException {
        FileUtil.writeToFile(SERIALIZATION_FILE, SerializableTestClass.JSON_STRING_REPRESENTATION);

        SerializableTestClass serializableTestClass = JsonUtil
                .deserializeObjectFromJsonFile(SERIALIZATION_FILE, SerializableTestClass.class);

        assertEquals(serializableTestClass.getName(), SerializableTestClass.getNameTestValue());
        assertEquals(serializableTestClass.getListOfLocalDateTimes(), SerializableTestClass.getListTestValues());
        assertEquals(serializableTestClass.getMapOfIntegerToString(), SerializableTestClass.getHashMapTestValues());
    }

    //TODO: @Test jsonUtil_readJsonStringToObjectInstance_correctObject()

    //TODO: @Test jsonUtil_writeThenReadObjectToJson_correctObject()
}
