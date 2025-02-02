package shortestpath;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

public class PathMapOverlay extends Overlay {
    private final Client client;
    private final ShortestPathPlugin plugin;
    private final ShortestPathConfig config;

    @Inject
    private WorldMapOverlay worldMapOverlay;

    private Area mapClipArea;

    @Inject
    private PathMapOverlay(Client client, ShortestPathPlugin plugin, ShortestPathConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.MANUAL);
        drawAfterLayer(WidgetInfo.WORLD_MAP_VIEW);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.drawMap()) {
            return null;
        }

        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null) {
            return null;
        }

        mapClipArea = getWorldMapClipArea(client.getWidget(WidgetInfo.WORLD_MAP_VIEW).getBounds());

        if (config.drawTransports()) {
            graphics.setClip(mapClipArea);
            for (WorldPoint a : plugin.pathfinder.transports.keySet()) {
                Point mapA = worldMapOverlay.mapWorldPointToGraphicsPoint(a);
                if (mapA == null) {
                    continue;
                }

                for (WorldPoint b : plugin.pathfinder.transports.get(a)) {
                    Point mapB = worldMapOverlay.mapWorldPointToGraphicsPoint(b);
                    if (mapB == null) {
                        continue;
                    }

                    graphics.drawLine(mapA.getX(), mapA.getY(), mapB.getX(), mapB.getY());
                }
            }
        }

        if (plugin.currentPath == null) {
            return null;
        }

        if (!plugin.currentPath.loading) {
            for (int i = 0; i < plugin.currentPath.getPath().size(); i++) {
                WorldPoint point = plugin.currentPath.getPath().get(i);
                drawOnMap(graphics, point, config.colourPath());
            }
        } else {
            List<WorldPoint> bestPath = plugin.currentPath.currentBest();

            if (bestPath != null) {
                for (WorldPoint point : bestPath) {
                    drawOnMap(graphics, point, config.colourPathCalculating());
                }
            }
        }

        return null;
    }

    private void drawOnMap(Graphics2D graphics, WorldPoint point, Color color) {
        Point start = worldMapOverlay.mapWorldPointToGraphicsPoint(point);
        Point end = worldMapOverlay.mapWorldPointToGraphicsPoint(point.dx(1).dy(-1));

        if (start == null || end == null) {
            return;
        }

        int x = start.getX();
        int y = start.getY();
        final int width = end.getX() - x;
        final int height = end.getY() - y;
        x -= width / 2;
        if (!mapClipArea.contains(x, y)) {
            return;
        }
        y -= height / 2;

        graphics.setColor(color);
        graphics.fillRect(x, y, width, height);
    }

    private Area getWorldMapClipArea(Rectangle baseRectangle) {
        final Widget overview = client.getWidget(WidgetInfo.WORLD_MAP_OVERVIEW_MAP);
        final Widget surfaceSelector = client.getWidget(WidgetInfo.WORLD_MAP_SURFACE_SELECTOR);

        Area clipArea = new Area(baseRectangle);

        if (overview != null && !overview.isHidden()) {
            clipArea.subtract(new Area(overview.getBounds()));
        }

        if (surfaceSelector != null && !surfaceSelector.isHidden()) {
            clipArea.subtract(new Area(surfaceSelector.getBounds()));
        }

        return clipArea;
    }
}
