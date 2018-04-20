package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.routing.graph.MyEdge;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.view.StandardEntityViewLayer;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by P.D.G. edited by Mostafa & Mahdi
 * User: mrl
 * Date: Aug 8, 2010
 * Time: 5:52:04 PM
 */
public class MrlGraphLayer extends StandardEntityViewLayer<Area> {

    private static final Color COLOR = Color.GREEN;
    private static final Stroke STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static Map<EntityID, Map<EntityID, ArrayList<MyEdge>>> GRAPH_EDGES = new FastMap<EntityID, Map<EntityID, ArrayList<MyEdge>>>();
    public boolean showEdgeValue;
    private RenderEdgeValueAction edgeValueAction;

    /**
     * Construct an Map Graph view layer.
     */
    public MrlGraphLayer() {
        super(Area.class);
    }

    @Override
    public String getName() {
        return "Graph";
    }

    @Override
    public void initialise(Config config) {
        showEdgeValue = false;
        edgeValueAction = new RenderEdgeValueAction();
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(edgeValueAction));

        return result;
    }

    @Override
    public Shape render(Area area, Graphics2D g, ScreenTransform t) {
        g.setColor(COLOR);
        g.setStroke(STROKE);
        if (StaticViewProperties.selectedObject != null) {
            try {
                ArrayList<MyEdge> thisAreaMyEdges = GRAPH_EDGES.get(StaticViewProperties.selectedObject.getID()).get(area.getID());
                if (thisAreaMyEdges != null) {
                    for (MyEdge a_edge : thisAreaMyEdges) {
                        if (a_edge.isPassable()) {
                            g.setColor(COLOR);
                        } else {
                            g.setColor(Color.RED);
                        }
                        g.drawLine(t.xToScreen(a_edge.getNodes().first().getPosition().first()),
                                t.yToScreen(a_edge.getNodes().first().getPosition().second()),
                                t.xToScreen(a_edge.getNodes().second().getPosition().first()),
                                t.yToScreen(a_edge.getNodes().second().getPosition().second()));

                        if (showEdgeValue) {
                            int x1 = (a_edge.getNodes().first().getPosition().first());
                            int y1 = (a_edge.getNodes().first().getPosition().second());
                            int x2 = (a_edge.getNodes().second().getPosition().first());
                            int y2 = (a_edge.getNodes().second().getPosition().second());
                            int x = Math.abs(x1 + x2);
                            int y = Math.abs(y1 + y2);
                            Point pos = new Point(x / 2, y / 2);
                            if (a_edge.getEntranceWeight() > 0) {
                                drawInfo(g, t, String.valueOf(a_edge.getWeight()), pos, 0, 0, Color.RED);
                            } else {
                                drawInfo(g, t, String.valueOf(a_edge.getWeight()), pos, 0, 0, Color.ORANGE.darker().darker());
                            }
                        }
//                }
                    }
                }
            } catch (NullPointerException ignored) {
            }
        }
        return null;
    }

    private final class RenderEdgeValueAction extends AbstractAction {

        public RenderEdgeValueAction() {
            super("Edge Weight");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisitedCivilianRenderInfo(!showEdgeValue);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showEdgeValue));
            putValue(Action.SMALL_ICON, showEdgeValue ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setVisitedCivilianRenderInfo(boolean render) {
        showEdgeValue = render;
        edgeValueAction.update();
    }


    private void drawInfo(Graphics2D g, ScreenTransform t, String strInfo, Point location, int changeXPos, int changeYPos, Color color) {
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.x);
            y = t.yToScreen(location.y);
            g.setColor(color);
            g.drawString(strInfo, x + changeXPos, y + changeYPos);
        }
    }
}