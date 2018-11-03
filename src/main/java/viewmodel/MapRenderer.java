package viewmodel;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import model.Map.Cell;
import model.Map.Occupant.Crate;
import model.Map.Occupant.Player;
import model.Map.Occupiable.DestTile;
import model.Map.Occupiable.Occupiable;
import model.Map.Occupiable.Tile;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static viewmodel.Config.LEVEL_EDITOR_TILE_SIZE;

/**
 * Renders maps onto canvases
 */
public class MapRenderer {
    private static Image wall = null;
    private static Image crateOnTile = null;
    private static Image crateOnDest = null;

    private static Image playerOnTile = null;
    private static Image playerOnDest = null;

    private static Image dest = null;
    private static Image tile = null;

    private static class TileImageMapping {
        private Image tile;
        private Image tileWithCrate;
        private Image tileWithPlayer;

        private TileImageMapping(Image tile, Image withCrate, Image withPlayer) {
            this.tile = tile;
            this.tileWithCrate = withCrate;
            this.tileWithPlayer = withPlayer;

            // TODO(Derppening): Add post-condition assertions
        }
    }

    private final static Map<Class<? extends Tile>, TileImageMapping> IMAGE_MAPPING = new HashMap<Class<? extends Tile>, TileImageMapping>() {{
        put(Tile.class, new TileImageMapping(tile, crateOnTile, playerOnTile));
        put(DestTile.class, new TileImageMapping(dest, crateOnDest, playerOnDest));
    }};
    private final static Map<LevelEditorCanvas.Brush, Image> BRUSH_IMAGE_MAP = new HashMap<LevelEditorCanvas.Brush, Image>() {{
        put(LevelEditorCanvas.Brush.CRATE_ON_DEST, crateOnDest);
        put(LevelEditorCanvas.Brush.CRATE_ON_TILE, crateOnTile);
        put(LevelEditorCanvas.Brush.DEST, dest);
        put(LevelEditorCanvas.Brush.PLAYER_ON_DEST, playerOnDest);
        put(LevelEditorCanvas.Brush.PLAYER_ON_TILE, playerOnTile);
        put(LevelEditorCanvas.Brush.TILE, tile);
        put(LevelEditorCanvas.Brush.WALL, wall);
    }};

    static {
        try {
            wall = new Image(MapRenderer.class.getResource("/assets/images/wall.png").toURI().toString());
            crateOnTile = new Image(MapRenderer.class.getResource("/assets/images/crateOnTile.png").toURI().toString());
            crateOnDest = new Image(MapRenderer.class.getResource("/assets/images/crateOnDest.png").toURI().toString());
            playerOnTile = new Image(MapRenderer.class.getResource("/assets/images/playerOnTile.png").toURI().toString());
            playerOnDest = new Image(MapRenderer.class.getResource("/assets/images/playerOnDest.png").toURI().toString());
            dest = new Image(MapRenderer.class.getResource("/assets/images/dest.png").toURI().toString());
            tile = new Image(MapRenderer.class.getResource("/assets/images/tile.png").toURI().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Render the map onto the canvas. This method can be used in Level Editor
     * <p>
     * Hint: set the canvas height and width as a multiple of the rows and cols
     *
     * @param canvas The canvas to be rendered onto
     * @param map    The map holding the current state of the game
     */
    static void render(Canvas canvas, LevelEditorCanvas.Brush[][] map) {
        //TODO
        canvas.setWidth(map[0].length * LEVEL_EDITOR_TILE_SIZE);
        canvas.setHeight(map.length * LEVEL_EDITOR_TILE_SIZE);

        for (int r = 0; r < map.length; ++r) {
            for (int c = 0; c < map[r].length; ++c) {
                Image image = BRUSH_IMAGE_MAP.get(map[r][c]);
                assert image != null;

                canvas.getGraphicsContext2D().drawImage(image, c * LEVEL_EDITOR_TILE_SIZE, r * LEVEL_EDITOR_TILE_SIZE);
            }
        }
    }

    /**
     * Render the map onto the canvas. This method can be used in GamePlayPane and LevelSelectPane
     * <p>
     * Hint: set the canvas height and width as a multiple of the rows and cols
     *
     * @param canvas The canvas to be rendered onto
     * @param map    The map holding the current state of the game
     */
    public static void render(Canvas canvas, Cell[][] map) {
        //TODO
        canvas.setWidth(map[0].length * LEVEL_EDITOR_TILE_SIZE);
        canvas.setHeight(map.length * LEVEL_EDITOR_TILE_SIZE);

        for (int r = 0; r < map.length; ++r) {
            for (int c = 0; c < map[r].length; ++c) {
                Image image;

                if (map[r][c] instanceof Tile) {
                    image = getTileImage((Tile) map[r][c]);
                } else {
                    image = wall;
                }

                canvas.getGraphicsContext2D().drawImage(image, c * LEVEL_EDITOR_TILE_SIZE, r * LEVEL_EDITOR_TILE_SIZE);
            }
        }
    }

    // TODO(Derppening): Add @NotNull annotation
    private static Image getTileImage(final Tile t) {
        Image image;
        if (t.getOccupant().orElse(null) instanceof Crate) {
            image = IMAGE_MAPPING.get(t.getClass()).tileWithCrate;
        } else if (t.getOccupant().orElse(null) instanceof Player) {
            image = IMAGE_MAPPING.get(t.getClass()).tileWithPlayer;
        } else if (!t.getOccupant().isPresent()) {
            image = IMAGE_MAPPING.get(t.getClass()).tile;
        } else {
            throw new IllegalArgumentException("No asset for unknown occupant type");
        }

        assert image != null;

        return image;
    }
}