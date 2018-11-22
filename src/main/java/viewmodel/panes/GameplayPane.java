package viewmodel.panes;

import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Exceptions.InvalidMapException;
import model.LevelManager;
import viewmodel.AudioManager;
import viewmodel.MapRenderer;
import viewmodel.SceneManager;
import viewmodel.customNodes.GameplayInfoPane;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Represents the gameplay pane in the game
 */
public class GameplayPane extends BorderPane {

    private final GameplayInfoPane info;
    private VBox canvasContainer;
    private Canvas gamePlayCanvas;
    private HBox buttonBar;
    private Button restartButton;
    private Button quitToMenuButton;

    /**
     * Instantiate the member components and connect and style them. Also set the callbacks.
     * Use 20 for the VBox spacing
     */
    public GameplayPane() {
        LevelManager manager = LevelManager.getInstance();

        info = new GameplayInfoPane(manager.currentLevelNameProperty(), manager.curGameLevelExistedDurationProperty(), manager.getGameLevel().numPushesProperty(), manager.curGameLevelNumRestartsProperty());
        canvasContainer = new VBox(20);
        gamePlayCanvas = new Canvas();
        buttonBar = new HBox(20);
        restartButton = new Button("Restart");
        quitToMenuButton = new Button("Quit to menu");

        connectComponents();
        styleComponents();
        setCallbacks();

        renderCanvas();
    }

    /**
     * Connects the components together (think adding them into another, setting their positions, etc).
     */
    private void connectComponents() {
        canvasContainer.getChildren().addAll(
                gamePlayCanvas
        );
        buttonBar.getChildren().addAll(
                info,
                restartButton,
                quitToMenuButton
        );

        this.setCenter(canvasContainer);
        this.setBottom(buttonBar);
    }

    /**
     * Apply CSS styling to components.
     */
    private void styleComponents() {
        buttonBar.getStyleClass().add("bottom-menu");
        canvasContainer.getStyleClass().add("big-vbox");

        for (Button b : Arrays.asList(restartButton, quitToMenuButton)) {
            b.getStyleClass().add("big-button");
        }
    }

    /**
     * Set the event handlers for the 2 buttons.
     * <p>
     * Also listens for key presses (w, a, s, d), which move the character.
     * <p>
     * Hint: {@link GameplayPane#setOnKeyPressed(EventHandler)}  is needed.
     * You will need to make the move, rerender the canvas, play the sound (if the move was made), and detect
     * for win and deadlock conditions. If win, play the win sound, and do the appropriate action regarding the timers
     * and generating the popups. If deadlock, play the deadlock sound, and do the appropriate action regarding the timers
     * and generating the popups.
     */
    private void setCallbacks() {
        restartButton.setOnAction(event -> doRestartAction());
        quitToMenuButton.setOnAction(event -> doQuitToMenuAction());

        this.setOnKeyPressed(event -> {
            LevelManager lvl = LevelManager.getInstance();
            AudioManager audio = AudioManager.getInstance();

            switch (event.getCode()) {
                case W:
                    lvl.getGameLevel().makeMove('w');
                    break;
                case A:
                    lvl.getGameLevel().makeMove('a');
                    break;
                case S:
                    lvl.getGameLevel().makeMove('s');
                    break;
                case D:
                    lvl.getGameLevel().makeMove('d');
                    break;
                default:
                    // not handled
            }

            renderCanvas();

            if (audio.isEnabled()) {
                audio.playMoveSound();
            }

            if (lvl.getGameLevel().isWin()) {
                if (audio.isEnabled()) {
                    audio.playWinSound();
                }

                // TODO(Derppening): Check with JAR
                lvl.resetLevelTimer();
                createLevelClearPopup();
            } else if (lvl.getGameLevel().isDeadlocked()) {
                if (audio.isEnabled()) {
                    audio.playDeadlockSound();
                }

                // TODO(Derppening): Check with JAR
                lvl.resetLevelTimer();
                createDeadlockedPopup();
            }
        });
    }

    /**
     * Called when the tries to quit to menu. Show a popup (see the documentation). If confirmed,
     * do the appropriate action regarding the level timer, level number of restarts, and go to the
     * main menu scene.
     */
    private void doQuitToMenuAction() {
        Alert box = new Alert(Alert.AlertType.CONFIRMATION);
        box.setTitle("Confirm");
        box.setHeaderText("Return to menu?");
        box.setContentText("Game progress will be lost.");
        box.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        box.showAndWait();
        if (box.getResult().equals(ButtonType.OK)) {
            LevelManager manager = LevelManager.getInstance();
            manager.resetLevelTimer();
            manager.resetNumRestarts();

            SceneManager.getInstance().showMainMenuScene();
        }
    }

    /**
     * Called when the user encounters deadlock. Show a popup (see the documentation).
     * If the user chooses to restart the level, call {@link GameplayPane#doRestartAction()}. Otherwise if they
     * quit to menu, switch to the level select scene, and do the appropriate action regarding
     * the number of restarts.
     */
    private void createDeadlockedPopup() {
        Alert box = new Alert(Alert.AlertType.CONFIRMATION);
        box.setTitle("Confirm");
        box.setHeaderText("Level deadlocked!");

        ButtonType restartButton = new ButtonType("Restart");
        ButtonType returnButton = new ButtonType("Return");

        box.getButtonTypes().setAll(restartButton, returnButton);

        Optional<ButtonType> result = box.showAndWait();
        if (!result.isPresent()) {
            if (System.getenv("CI") != null && System.getenv("CI").equals("true")) {
                System.out.println("This is normal in CI environment");
            } else {
                System.err.println("Should be an impossible!");
            }
        } else if (box.getResult().getText().equals("Return")) {
            SceneManager.getInstance().showLevelSelectMenuScene();
            LevelManager.getInstance().resetNumRestarts();
        } else {
            doRestartAction();
        }
    }

    /**
     * Called when the user clears the level successfully. Show a popup (see the documentation).
     * If the user chooses to go to the next level, set the new level, rerender, and do the appropriate action
     * regarding the timers and num restarts. If they choose to return, show the level select menu, and do
     * the appropriate action regarding the number of level restarts.
     * <p>
     * Hint:
     * Take care of the edge case for when the user clears the last level. In this case, there shouldn't
     * be an option to go to the next level.
     */
    private void createLevelClearPopup() {
        Alert box = new Alert(Alert.AlertType.CONFIRMATION);
        box.setTitle("Confirm");
        box.setHeaderText("Level cleared!");

        ButtonType nextLevelButton = new ButtonType("Next level");
        ButtonType returnButton = new ButtonType("Return");

        if (LevelManager.getInstance().getNextLevelName() != null) {
            box.getButtonTypes().setAll(nextLevelButton, returnButton);
        } else {
            box.getButtonTypes().setAll(returnButton);
        }

        Optional<ButtonType> result = box.showAndWait();
        if (!result.isPresent()) {
            if (System.getenv("CI") != null && System.getenv("CI").equals("true")) {
                System.out.println("This is normal in CI environment");
            } else {
                System.err.println("Should be an impossible!");
            }
        } else if (result.get().getText().equals("Return")) {
            SceneManager.getInstance().showLevelSelectMenuScene();
            LevelManager.getInstance().resetNumRestarts();
        } else {
            String nextLevel = "";
            while (nextLevel != null) {
                try {
                    nextLevel = LevelManager.getInstance().getNextLevelName();
                    LevelManager.getInstance().setLevel(nextLevel);
                    break;
                } catch (InvalidMapException e) {
                    LevelManager.getInstance().currentLevelNameProperty().set(nextLevel);
                } catch (FileNotFoundException e) {
                    Alert wBox = new Alert(Alert.AlertType.WARNING);
                    wBox.setHeaderText("Cannot open map");
                    wBox.setContentText(nextLevel + " is missing.");
                    wBox.showAndWait();

                    LevelManager.getInstance().currentLevelNameProperty().set(nextLevel);
                }
            }

            renderCanvas();

            LevelManager.getInstance().resetNumRestarts();
            LevelManager.getInstance().resetLevelTimer();
            LevelManager.getInstance().curGameLevelExistedDurationProperty().set(0);
            LevelManager.getInstance().getGameLevel().numPushesProperty().setValue(0);
            LevelManager.getInstance().startLevelTimer();
        }
    }

    /**
     * Set the current level to the current level name, rerender the canvas, reset and start the timer, and
     * increment the number of restarts
     */
    private void doRestartAction() {
        try {
            LevelManager.getInstance().setLevel(LevelManager.getInstance().currentLevelNameProperty().getValue());
        } catch (FileNotFoundException | InvalidMapException e) {
            Alert box = new Alert(Alert.AlertType.WARNING);
            box.setHeaderText("Cannot open current map");
            box.setContentText("You will be returned to the Main Menu.");
            box.showAndWait();

            SceneManager.getInstance().showMainMenuScene();
        }

        renderCanvas();
        LevelManager.getInstance().resetLevelTimer();
        LevelManager.getInstance().incrementNumRestarts();
        LevelManager.getInstance().curGameLevelExistedDurationProperty().set(0);
        LevelManager.getInstance().getGameLevel().numPushesProperty().setValue(0);
        LevelManager.getInstance().startLevelTimer();
    }

    private void doLoadMapFail() {
        // TODO(Derppening): Gracefully fail!
        Alert box = new Alert(Alert.AlertType.ERROR, "Unable to load map!");
        box.showAndWait();
        SceneManager.getInstance().showMainMenuScene();
    }

    /**
     * Render the canvas with updated data
     * <p>
     * Hint: {@link MapRenderer}
     */
    private void renderCanvas() {
        MapRenderer.render(gamePlayCanvas, LevelManager.getInstance().getGameLevel().getMap().getCells());
    }
}
