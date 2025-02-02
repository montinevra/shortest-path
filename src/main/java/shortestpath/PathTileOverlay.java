package shortestpath;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class PathTileOverlay extends Overlay {
    private final Client client;
    private final ShortestPathPlugin plugin;
    private final ShortestPathConfig config;

    @Inject
    public PathTileOverlay(Client client, ShortestPathPlugin plugin, ShortestPathConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    private void renderTransports(Graphics2D graphics) {
        for (WorldPoint a : plugin.pathfinder.transports.keySet()) {
            drawTile(graphics, a, config.colourTransports(), -1);

            Point ca = tileCenter(a);

            if (ca == null) {
                continue;
            }

            for (WorldPoint b : plugin.pathfinder.transports.get(a)) {
                Point cb = tileCenter(b);

                if (cb != null) {
                    graphics.drawLine(ca.x, ca.y, cb.x, cb.y);
                }
            }

            StringBuilder s = new StringBuilder();
            for (WorldPoint b : plugin.pathfinder.transports.get(a)) {
                if (b.getPlane() > a.getPlane()) {
                    s.append("+");
                } else if (b.getPlane() < a.getPlane()) {
                    s.append("-");
                } else {
                    s.append("=");
                }
            }
            graphics.setColor(Color.WHITE);
            graphics.drawString(s.toString(), ca.x, ca.y);
        }
    }

    private void renderCollisionMap(Graphics2D graphics) {
        for (Tile[] row : client.getScene().getTiles()[client.getPlane()]) {
            for (Tile tile : row) {
                if (tile == null) {
                    continue;
                }

                Polygon tilePolygon = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());

                if (tilePolygon == null) {
                    continue;
                }

                int x = tile.getWorldLocation().getX();
                int y = tile.getWorldLocation().getY();
                int z = tile.getWorldLocation().getPlane();

                String s = (!plugin.pathfinder.map.n(x, y, z) ? "n" : "") +
                        (!plugin.pathfinder.map.s(x, y, z) ? "s" : "") +
                        (!plugin.pathfinder.map.e(x, y, z) ? "e" : "") +
                        (!plugin.pathfinder.map.w(x, y, z) ? "w" : "");

                if (!s.isEmpty() && !s.equals("nsew")) {
                    graphics.setColor(Color.WHITE);
                    int stringX = (int) (tilePolygon.getBounds().getCenterX() - graphics.getFontMetrics().getStringBounds(s, graphics).getWidth() / 2);
                    int stringY = (int) tilePolygon.getBounds().getCenterY();
                    graphics.drawString(s, stringX, stringY);
                } else if (!s.isEmpty()) {
                    graphics.setColor(config.colourCollisionMap());
                    graphics.fill(tilePolygon);
                }
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.drawTransports()) {
            this.renderTransports(graphics);
        }

        if (config.drawCollisionMap()) {
            this.renderCollisionMap(graphics);
        }

        if (config.drawTiles() && plugin.currentPath != null) {
            int counter = 0;
            if (!plugin.currentPath.loading) {
                List<WorldPoint> pathPoints = plugin.currentPath.getPath();
                if (pathPoints != null) {
                    for (WorldPoint point : pathPoints) {
                        drawTile(graphics, point, new Color(
                            config.colourPath().getRed(),
                            config.colourPath().getGreen(),
                            config.colourPath().getBlue(),
                            config.colourPath().getAlpha() / 2),
                            counter++);
                    }
                }
            } else {
                List<WorldPoint> bestPath = plugin.currentPath.currentBest();
                if (bestPath != null) {
                    for (WorldPoint point : bestPath) {
                        drawTile(graphics, point, new Color(
                            config.colourPathCalculating().getRed(),
                            config.colourPathCalculating().getGreen(),
                            config.colourPathCalculating().getBlue(),
                            config.colourPathCalculating().getAlpha() / 2),
                            counter++);
                    }
                }
            }
        }

        return null;
    }

    private Point tileCenter(WorldPoint b) {
        if (b.getPlane() != client.getPlane()) {
            return null;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, b);
        if (lp == null) {
            return null;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            return null;
        }

        int cx = poly.getBounds().x + poly.getBounds().width / 2;
        int cy = poly.getBounds().y + poly.getBounds().height / 2;
        return new Point(cx, cy);
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, Color color, int counter) {
        if (point.getPlane() != client.getPlane()) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, point);
        if (lp == null) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            return;
        }

        graphics.setColor(color);
        graphics.fill(poly);

        if (counter >= 0 && !TileCounter.DISABLED.equals(config.showTileCounter())) {
            if (TileCounter.REMAINING.equals(config.showTileCounter())) {
                counter = (!plugin.currentPath.loading ?
                    plugin.currentPath.getPath().size() : plugin.currentPath.currentBest().size()) - counter - 1;
            }
            String counterText = Integer.toString(counter);
            graphics.setColor(Color.WHITE);
            graphics.drawString(
                counterText,
                (int) (poly.getBounds().getCenterX() -
                    graphics.getFontMetrics().getStringBounds(counterText, graphics).getWidth() / 2),
                (int) poly.getBounds().getCenterY());
        }
    }
}
