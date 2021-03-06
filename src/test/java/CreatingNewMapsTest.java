import com.sun.javafx.fxml.PropertyNotFoundException;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import viewmodel.LevelEditorCanvas;
import viewmodel.SceneManager;
import viewmodel.customNodes.NumberTextField;
import viewmodel.panes.LevelEditorPane;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Test cases for all conditions as specified by Compulsory Demo Tasks, Creating New Maps section.
 */
public class CreatingNewMapsTest extends ApplicationTest {
    private static final Class<?> LEVEL_EDITOR_CANVAS_CLAZZ = LevelEditorCanvas.class;

    private Node listViewNode;
    private Node rowBoxNode;
    private Node colBoxNode;
    private Node newGridNode;
    private Node saveNode;
    private Node canvasNode;

    /**
     * Assigns the member fields to the appropriate nodes.
     */
    @BeforeEach
    void setupEach() {
        Parent currentRoot = SceneManager.getInstance().getStage().getScene().getRoot();
        assertTrue(currentRoot instanceof LevelEditorPane);

        Node leftVBoxNode = currentRoot.getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof VBox && ((VBox) it).getChildren().size() == 6)
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);
        Node centerVBoxNode = currentRoot.getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof VBox && ((VBox) it).getChildren().size() == 1)
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);

        listViewNode = ((VBox) leftVBoxNode).getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof ListView)
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);
        rowBoxNode = ((VBox) leftVBoxNode).getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof BorderPane && ((Label) ((BorderPane) it).getLeft()).getText().equals("Rows"))
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);
        colBoxNode = ((VBox) leftVBoxNode).getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof BorderPane && ((Label) ((BorderPane) it).getLeft()).getText().equals("Columns"))
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);
        newGridNode = ((VBox) leftVBoxNode).getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof Button && ((Button) it).getText().equals("New Grid"))
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);
        saveNode = ((VBox) leftVBoxNode).getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof Button && ((Button) it).getText().equals("Save"))
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);
        canvasNode = ((VBox) centerVBoxNode).getChildrenUnmodifiable().stream()
                .filter(it -> it instanceof Canvas)
                .findFirst()
                .orElseThrow(PropertyNotFoundException::new);

        setBoardSize(null, null);
    }

    /**
     * Displays the Level Editor scene.
     *
     * @param stage Primary Stage.
     */
    @Override
    @Start
    public void start(Stage stage) {
        Platform.runLater(() -> SceneManager.getInstance().setStage(stage));
        Platform.runLater(() -> SceneManager.getInstance().showLevelEditorScene());

        waitForFxEvents();
    }

    /**
     * @return Topmost modal stage, or null if there are none.
     */
    private Optional<Stage> getTopModalStage() {
        final List<Window> allWindows = new ArrayList<>(robotContext().getWindowFinder().listWindows());
        return Optional.ofNullable(
                (Stage) allWindows.stream()
                        .filter(it -> it instanceof Stage)
                        .filter(it -> ((Stage) it)
                                .getModality() == Modality.APPLICATION_MODAL)
                        .findFirst()
                        .orElse(null));
    }

    /**
     * Helper function for setting the board size and resetting the board.
     *
     * @param rows New number of rows, or {@code null} if using the previous value.
     * @param cols New number of columns, or {@code null} if using the previous value.
     */
    private void setBoardSize(@Nullable Integer rows, @Nullable Integer cols) {
        if (rows != null && cols != null) {
            final NumberTextField rowField = ((NumberTextField) ((BorderPane) rowBoxNode).getRight());
            final NumberTextField colField = ((NumberTextField) ((BorderPane) colBoxNode).getRight());

            rowField.clear();
            colField.clear();
            rowField.replaceSelection(rows.toString());
            colField.replaceSelection(cols.toString());
        }
        waitForFxEvents();

        clickOn(newGridNode);
        waitForFxEvents();
    }

    /**
     * <p>Tests for Condition A:</p>
     *
     * <p>Select each of the 7 brushes in the list view, and ensure they each correctly draw the element on
     * the canvas.</p>
     */
    @Test
    void testSelectBrushAndDrawCanvas() {
        setBoardSize(5, 5);

        final LevelEditorCanvas canvas = (LevelEditorCanvas) canvasNode;

        @SuppressWarnings("unchecked") final ListView<LevelEditorCanvas.Brush> listView = (ListView<LevelEditorCanvas.Brush>) listViewNode;
        for (int i = 0; i < listView.getItems().size(); ++i) {
            final LevelEditorCanvas.Brush brush = listView.getItems().get(i);

            listView.getSelectionModel().clearAndSelect(i);
            waitForFxEvents();

            clickOn(offset(canvasNode, -64.0, -64.0));
            waitForFxEvents();

            Class<?> clazz = LevelEditorCanvas.class;
            try {
                final Field mapField = clazz.getDeclaredField("map");
                mapField.setAccessible(true);
                final LevelEditorCanvas.Brush[][] map = (LevelEditorCanvas.Brush[][]) mapField.get(canvas);

                assertEquals(brush, map[0][0]);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * <p>Tests for Condition B:</p>
     *
     * <p>Select Player on Destination brush, and click somewhere on the grid. Then, select Player on
     * Tile brush, and click elsewhere. Ensure that:</p>
     * <ul>
     * <li>The player is drawn in the new location</li>
     * <li>The player is removed from the old location</li>
     * <li>The old location now shows the destination tile dot</li>
     * </ul>
     */
    @Test
    void testPlayerReplacement() {
        setBoardSize(5, 5);

        final LevelEditorCanvas canvas = (LevelEditorCanvas) canvasNode;

        @SuppressWarnings("unchecked") final ListView<LevelEditorCanvas.Brush> listView = (ListView<LevelEditorCanvas.Brush>) listViewNode;

        // Select Player on Destination brush, and click somewhere on the grid.

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.PLAYER_ON_DEST);
        waitForFxEvents();

        clickOn(offset(canvasNode, -64.0, -64.0));
        waitForFxEvents();

        // Then, select Player on Tile brush, and click elsewhere.

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.PLAYER_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, 64.0, 64.0));
        waitForFxEvents();

        Class<?> clazz = LevelEditorCanvas.class;
        try {
            final Field mapField = clazz.getDeclaredField("map");
            mapField.setAccessible(true);
            final LevelEditorCanvas.Brush[][] map = (LevelEditorCanvas.Brush[][]) mapField.get(canvas);

            // Ensure that...
            assertEquals(LevelEditorCanvas.Brush.DEST, map[0][0]);
            assertEquals(LevelEditorCanvas.Brush.PLAYER_ON_TILE, map[4][4]);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    /**
     * <p>Tests for Condition C:</p>
     *
     * <p>Select Player on Tile brush, and click somewhere on the grid. Then, using the same brush, click
     * elsewhere. Ensure that:</p>
     * <ul>
     * <li>The player is drawn in the new location</li>
     * <li>The player is removed from the old location</li>
     * <li>The old location is now a normal tile</li>
     * </ul>
     */
    @Test
    void testMovePlayer() {
        setBoardSize(5, 5);

        final LevelEditorCanvas canvas = (LevelEditorCanvas) canvasNode;

        @SuppressWarnings("unchecked") final ListView<LevelEditorCanvas.Brush> listView = (ListView<LevelEditorCanvas.Brush>) listViewNode;

        // Select Player on Tile brush, and click somewhere on the grid.

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.PLAYER_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, -64.0, -64.0));

        // Then, using the same brush, click elsewhere.

        clickOn(offset(canvasNode, 64.0, 64.0));
        waitForFxEvents();

        try {
            final Field mapField = LEVEL_EDITOR_CANVAS_CLAZZ.getDeclaredField("map");
            mapField.setAccessible(true);
            final LevelEditorCanvas.Brush[][] map = (LevelEditorCanvas.Brush[][]) mapField.get(canvas);

            // Ensure that...
            assertEquals(LevelEditorCanvas.Brush.TILE, map[0][0]);
            assertEquals(LevelEditorCanvas.Brush.PLAYER_ON_TILE, map[4][4]);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    /**
     * <p>Performs action as specified by Condition D.</p>
     *
     * <p>Select Player on Tile brush, and click the top left location.</p>
     */
    private void placePlayerOnTopLeft() {
        setBoardSize(5, 5);

        @SuppressWarnings("unchecked") final ListView<LevelEditorCanvas.Brush> listView = (ListView<LevelEditorCanvas.Brush>) listViewNode;

        // Select Player on Tile brush, and click the top left location.

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.PLAYER_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, -64.0, -64.0));
        waitForFxEvents();
    }

    /**
     * <p>Tests for Condition D+E:</p>
     *
     * <p>Select Player on Tile brush, and click the top left location.</p>
     *
     * <p>Without exiting the level editor, change map dimensions to 4 by 4, and click new grid. Ensure
     * that</p>
     * <ul>
     * <li>The map is resized appropriately</li>
     * <li>The map is reset</li>
     * </ul>
     */
    @Test
    void testResizeMap() {
        setBoardSize(5, 5);

        final LevelEditorCanvas canvas = (LevelEditorCanvas) canvasNode;

        // technically, i need to reset the map to the state of Condition D, so...
        placePlayerOnTopLeft();

        // Without exiting the level editor, change map dimensions to 4 by 4, and click new grid.

        setBoardSize(4, 4);

        try {
            final Field mapField = LEVEL_EDITOR_CANVAS_CLAZZ.getDeclaredField("map");
            mapField.setAccessible(true);
            final LevelEditorCanvas.Brush[][] map = (LevelEditorCanvas.Brush[][]) mapField.get(canvas);

            // Ensure that...
            assertEquals(4, map.length);
            for (LevelEditorCanvas.Brush[] b : map) {
                assertEquals(4, b.length);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    /**
     * <p>Tests for Condition F:</p>
     *
     * <p>Select Crate on Tile brush, and click the top left location (where the player used to be). Now,
     * select Player on Tile brush, and place player on bottom right corner.</p>
     * <ul>
     * <li>Ensure that the crate does not disappear</li>
     * </ul>
     */
    @Test
    void testOccupantOnTileCoexistence() {
        setBoardSize(4, 4);

        final LevelEditorCanvas canvas = (LevelEditorCanvas) canvasNode;

        @SuppressWarnings("unchecked") final ListView<LevelEditorCanvas.Brush> listView = (ListView<LevelEditorCanvas.Brush>) listViewNode;

        // Select Crate on Tile brush, and click the top left location (where the player used to be).

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.CRATE_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, -48.0, -48.0));
        waitForFxEvents();

        // Now, select Player on Tile brush, and place player on bottom right corner.

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.PLAYER_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, 48.0, 48.0));
        waitForFxEvents();

        try {
            final Field mapField = LEVEL_EDITOR_CANVAS_CLAZZ.getDeclaredField("map");
            mapField.setAccessible(true);
            final LevelEditorCanvas.Brush[][] map = (LevelEditorCanvas.Brush[][]) mapField.get(canvas);

            // Ensure that...
            assertEquals(LevelEditorCanvas.Brush.CRATE_ON_TILE, map[0][0]);
            assertEquals(LevelEditorCanvas.Brush.PLAYER_ON_TILE, map[3][3]);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    /**
     * <p>Tests for Condition G:</p>
     *
     * <p>Have the student demonstrate that the following map conditions cannot be violated when trying to
     * save the map:</p>
     * <ul>
     * <li>Map dimensions smaller than 3x3</li>
     * <li>Map without a player</li>
     * <li>Imbalanced number of crates and destinations. Ensure that Crate on Destination, Player on
     * Destination, Crate on Tile, and Destination are all used</li>
     * <li>Having fewer than 1 crate/destination pair</li>
     * </ul>
     */
    @Test
    void testSaveMapWithUnsatisfiedPreconditions() {
        setBoardSize(4, 4);

        @SuppressWarnings("unchecked") final ListView<LevelEditorCanvas.Brush> listView = (ListView<LevelEditorCanvas.Brush>) listViewNode;
        Stage dialog;

        // Having fewer than 1 crate/destination pair

        setBoardSize(null, null);
        clickOn(saveNode);

        // Ensure that...
        dialog = getTopModalStage().orElseThrow(NoSuchElementException::new);
        assertNotNull(dialog);

        type(KeyCode.SPACE);

        // Imbalanced number of crates and destinations. Ensure that Crate on Destination, Player on Destination, Crate
        // on Tile, and Destination are all used

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.CRATE_ON_DEST);
        waitForFxEvents();

        clickOn(offset(canvasNode, -48.0, -48.0));
        waitForFxEvents();

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.PLAYER_ON_DEST);
        waitForFxEvents();

        clickOn(offset(canvasNode, -16.0, -16.0));
        waitForFxEvents();

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.CRATE_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, 16.0, 16.0));
        waitForFxEvents();

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.DEST);
        waitForFxEvents();

        clickOn(offset(canvasNode, 48.0, 48.0));
        waitForFxEvents();

        clickOn(saveNode);
        waitForFxEvents();

        // Ensure that...
        dialog = getTopModalStage().orElseThrow(NoSuchElementException::new);
        assertNotNull(dialog);

        type(KeyCode.SPACE);
        waitForFxEvents();

        // Map without a player

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, -16.0, -16.0));
        waitForFxEvents();

        clickOn(saveNode);
        waitForFxEvents();

        // Ensure that...
        dialog = getTopModalStage().orElseThrow(NoSuchElementException::new);
        assertNotNull(dialog);

        type(KeyCode.SPACE);
        waitForFxEvents();

        // Map dimensions smaller than 3x3

        setBoardSize(1, 3);

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.PLAYER_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, -32.0, 0.0));
        waitForFxEvents();

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.CRATE_ON_TILE);
        waitForFxEvents();

        clickOn(offset(canvasNode, 0.0, 0.0));
        waitForFxEvents();

        listView.getSelectionModel().select(LevelEditorCanvas.Brush.DEST);
        waitForFxEvents();

        clickOn(offset(canvasNode, 32.0, 0.0));
        waitForFxEvents();

        clickOn(saveNode);
        waitForFxEvents();

        // Ensure that...
        dialog = getTopModalStage().orElseThrow(NoSuchElementException::new);
        assertNotNull(dialog);

        type(KeyCode.SPACE);
    }
}
