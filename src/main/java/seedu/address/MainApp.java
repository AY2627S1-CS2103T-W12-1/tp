package seedu.address;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.FileUtil;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.AppInstanceLock;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.ui.RosterRecoveryDialog;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    /** Version reported by the application and its startup log. */
    public static final String VERSION = "v1.5";

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);
    private static final Path USER_PREFS_FILE_PATH = FileUtil.getHomeFolder().resolve("preferences.json");
    private static final Path ADDRESS_BOOK_FILE_PATH = FileUtil.getHomeFolder().resolve("data/addressbook.json");

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;

    /** Held for the whole production session so another launch cannot save a stale roster. */
    private AppInstanceLock instanceLock;

    @Override
    public void init() throws Exception {
        instanceLock = AppInstanceLock.acquire(FileUtil.getHomeFolder().resolve(AppInstanceLock.FILE_NAME));
        try {
            init(USER_PREFS_FILE_PATH, ADDRESS_BOOK_FILE_PATH);
        } catch (Exception exception) {
            instanceLock.close();
            instanceLock = null;
            throw exception;
        }
    }

    /** Initializes components using explicit files, allowing isolated startup checks. */
    void init(Path prefsFile, Path rosterFile) throws Exception {
        logger.info("=============================[ Initializing TrackCall ]===========================");
        super.init();

        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(prefsFile);
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(rosterFile);
        storage = new StorageManager(addressBookStorage, userPrefsStorage);

        model = new ModelManager(new AddressBook(), userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic, storage.getAddressBookFilePath());
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (prefsOptional.isEmpty()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting TrackCall " + MainApp.VERSION);
        logger.info("Using data file : " + storage.getAddressBookFilePath());
        try {
            Optional<ReadOnlyAddressBook> saved = storage.readAddressBook();
            if (saved.isEmpty()) {
                logger.info("Data file not found. Loading sample members into TrackCall.");
            }
            model.setAddressBook(saved.orElseGet(SampleDataUtil::getSampleAddressBook));
        } catch (DataLoadingException e) {
            logger.warning("The saved roster could not be loaded. Recovery is required: " + e.getMessage());
            if (!recoverRoster(primaryStage)) {
                Platform.exit();
                return;
            }
        }
        ui.start(primaryStage);
    }

    /** Preserves the damaged file and requires an explicit choice before exposing the editable roster. */
    boolean recoverRoster(Stage owner) {
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(storage.getAddressBookFilePath());
        try {
            Path preservedFile = addressBookStorage.preserveDamagedFile();
            Optional<ReadOnlyAddressBook> backup;
            try {
                backup = addressBookStorage.readBackup();
            } catch (DataLoadingException e) {
                logger.warning("The backup could not be loaded: " + e.getMessage());
                backup = Optional.empty();
            }
            RosterRecoveryDialog.Choice choice = new RosterRecoveryDialog(owner, preservedFile,
                    JsonAddressBookStorage.getBackupFilePath(storage.getAddressBookFilePath()),
                    backup.isPresent()).showChoice();
            if (choice == RosterRecoveryDialog.Choice.CLOSE) {
                return false;
            }
            ReadOnlyAddressBook recovered = choice == RosterRecoveryDialog.Choice.RESTORE_BACKUP
                    ? backup.orElseThrow() : new AddressBook();
            addressBookStorage.recoverAddressBook(recovered, preservedFile);
            model.setAddressBook(recovered);
            return true;
        } catch (IOException e) {
            logger.severe("Could not recover the roster: " + StringUtil.getDetails(e));
            Alert alert = new Alert(Alert.AlertType.ERROR);
            if (owner.getScene() != null) {
                alert.initOwner(owner);
            }
            alert.setTitle("Roster recovery failed");
            alert.setHeaderText("TrackCall could not safely recover your roster.");
            alert.setContentText("The saved roster has not been replaced. Close TrackCall and check the file "
                    + "and folder permissions, or restore a backup manually.\n\n" + e.getMessage());
            alert.showAndWait();
            return false;
        }
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping TrackCall ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        } finally {
            if (instanceLock != null) {
                try {
                    instanceLock.close();
                } catch (IOException exception) {
                    logger.warning("Could not release the application lock: " + exception.getMessage());
                }
            }
        }
    }
}
