package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.mrlPersonal.viewer.StandardEntityToPaint;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.MrlEdge;
import mrl.world.object.MrlRoad;
import rescuecore2.config.Config;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Road;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 12, 2010
 * Time: 4:42:03 PM
 */
public class MrlRoadLayer extends MrlAreaLayer<Road> {
    private static final Color ROAD_EDGE_COLOUR = Color.GRAY.darker();
    private static final Color ROAD_SHAPE_COLOUR = new Color(185, 185, 185);
    private static final Color HYDRANT_SHAPE_COLOUR = new Color(255, 128, 100);

    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static Map<EntityID, Map<EntityID, MrlRoad>> MRL_ROADS_MAP = new HashMap<EntityID, Map<EntityID, MrlRoad>>();

    private Action showIdsAction;
    private Action edgeOpenPartAction;
    private Action showRoadScaleAction;

    /**
     * Construct a road rendering layer.
     */
    public MrlRoadLayer() {
        super(Road.class);
    }

    @Override
    public String getName() {
        return "Roads";
    }

    @Override
    protected void paintShape(Road r, Polygon shape, Graphics2D g) {
        StandardEntityToPaint entityToPaint = StaticViewProperties.getPaintObject(r);
        if (entityToPaint != null) {
            g.setColor(entityToPaint.getColor());
            g.fill(shape);
            return;

        } else {
            if (r == StaticViewProperties.selectedObject) {
                g.setColor(Color.MAGENTA);
            } else {
                if (r instanceof Hydrant) {
                    g.setColor(HYDRANT_SHAPE_COLOUR);
                } else {
                    g.setColor(ROAD_SHAPE_COLOUR);
                }
            }
        }
        g.fill(shape);
        if (MrlViewer.agentSelected != null && r != null && MRL_ROADS_MAP.containsKey(MrlViewer.agentSelected))
            mrlRoad = MRL_ROADS_MAP.get(MrlViewer.agentSelected).get(r.getID());

    }

    public static MrlRoad mrlRoad = null;

    @Override
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
        g.setColor(ROAD_EDGE_COLOUR);
        g.setStroke(e.isPassable() ? ENTRANCE_STROKE : WALL_STROKE);
        g.drawLine(t.xToScreen(e.getStartX()),
                t.yToScreen(e.getStartY()),
                t.xToScreen(e.getEndX()),
                t.yToScreen(e.getEndY()));
        if (showEdgeOpenParts && mrlRoad != null) {
            try {
                MrlEdge mrlEdge = mrlRoad.getMrlEdge(e);
                if (mrlEdge != null && mrlEdge.isPassable()) {
                    Line2D line2D = mrlRoad.getMrlEdge(e).getOpenPart();
                    if (line2D != null && line2D.getOrigin() != null && line2D.getEndPoint() != null) {
                        int x1 = t.xToScreen(line2D.getOrigin().getX()), y1 = t.yToScreen(line2D.getOrigin().getY()),
                                x2 = t.xToScreen(line2D.getEndPoint().getX()), y2 = t.yToScreen(line2D.getEndPoint().getY());
                        if (!mrlEdge.isTooSmall()) {
                            g.setColor(Color.GREEN);
                        } else {
                            g.setStroke(WALL_STROKE);
                            g.setColor(Color.RED);
                        }
                        g.drawLine(x1, y1, x2, y2);
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }

    }

    @Override
    public void initialise(Config config) {
        showRIds = false;
        showRoadScale = false;
        showIdsAction = new ShowIdsAction();
        edgeOpenPartAction = new EdgeOpenPartAction();
        showRoadScaleAction = new ShowRoadScaleAction();
    }

    private final class ShowIdsAction extends AbstractAction {
        public ShowIdsAction() {
            super("show ids");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRIds));
            putValue(Action.SMALL_ICON, showRIds ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showRIds = !showRIds;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRIds));
            putValue(Action.SMALL_ICON, showRIds ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private final class EdgeOpenPartAction extends AbstractAction {
        public EdgeOpenPartAction() {
            super("edge open part");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showEdgeOpenParts));
            putValue(Action.SMALL_ICON, showEdgeOpenParts ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showEdgeOpenParts = !showEdgeOpenParts;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showEdgeOpenParts));
            putValue(Action.SMALL_ICON, showEdgeOpenParts ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private final class ShowRoadScaleAction extends AbstractAction {
        public ShowRoadScaleAction() {
            super("show road scale");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRoadScale));
            putValue(Action.SMALL_ICON, showRoadScale ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showRoadScale = !showRoadScale;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRoadScale));
            putValue(Action.SMALL_ICON, showRoadScale ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(showIdsAction));
        result.add(new JMenuItem(showRoadScaleAction));
        result.add(new JMenuItem(edgeOpenPartAction));
        return result;
    }
}
