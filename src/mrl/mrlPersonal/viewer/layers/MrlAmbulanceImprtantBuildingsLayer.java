package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Erfan Jazeb Nikoo
 */

public class MrlAmbulanceImprtantBuildingsLayer extends MrlAreaLayer<Building> {

    private static final Color OUTLINE_COLOUR = Color.GRAY.darker().darker();

    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static Map<EntityID, List<EntityID>> PARTITION_VISITED_BUILDINGS_MAP = Collections.synchronizedMap(new FastMap<EntityID, List<EntityID>>());
    public static Map<EntityID, List<EntityID>> VICTIM_BUILDINGS_MAP = Collections.synchronizedMap(new FastMap<EntityID, List<EntityID>>());
    public static Map<EntityID, Boolean> IS_MERGED_VISITED_BUILDINGS_MAP = Collections.synchronizedMap(new FastMap<EntityID, Boolean>());
    private static final Color VISITED_COLOR = Color.YELLOW.darker();
    private static final Color VICTIM_COLOR = Color.CYAN;

    /**
     * Construct a building view layer.
     */
    public MrlAmbulanceImprtantBuildingsLayer() {
        super(Building.class);
    }


    private boolean visible;

    private boolean victimBuildings;
    private Action victimBuildingsAction;

    private boolean visitedBuildings;
    private Action visitedBuildingsAction;

    @Override
    public String getName() {
        return "Ambulance Important Buildings";

    }

    @Override
    public void setVisible(boolean b) {
        visible = b;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void initialise(Config config) {
        visitedBuildings = false;
        visitedBuildingsAction = new VisitedBuildingsAction();
        victimBuildings = false;
        victimBuildingsAction = new VictimBuildingsAction();
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(victimBuildingsAction));
        result.add(new JMenuItem(visitedBuildingsAction));

        return result;
    }

    private final class VictimBuildingsAction extends AbstractAction {

        public VictimBuildingsAction() {
            super("Victim Buildings");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(victimBuildings));
            putValue(Action.SMALL_ICON, victimBuildings ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            victimBuildings = !victimBuildings;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(victimBuildings));
            putValue(Action.SMALL_ICON, victimBuildings ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }

    }

    private final class VisitedBuildingsAction extends AbstractAction {

        public VisitedBuildingsAction() {
            super("Visited Buildings");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(visitedBuildings));
            putValue(Action.SMALL_ICON, visitedBuildings ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            visitedBuildings = !visitedBuildings;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(visitedBuildings));
            putValue(Action.SMALL_ICON, visitedBuildings ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }

    }

    @Override
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
        g.setColor(OUTLINE_COLOUR);
        g.setStroke(e.isPassable() ? ENTRANCE_STROKE : WALL_STROKE);
        g.drawLine(t.xToScreen(e.getStartX()),
                t.yToScreen(e.getStartY()),
                t.xToScreen(e.getEndX()),
                t.yToScreen(e.getEndY()));
    }

    @Override
    protected void paintShape(Building b, Polygon shape, Graphics2D g) {
        if (visitedBuildings) {
            drawVisited(b, shape, g);
        }
        if (victimBuildings) {
            drawVictims(b, shape, g);
        }
    }

    private void drawVictims(Building b, Polygon shape, Graphics2D g) {
        if (StaticViewProperties.selectedObject != null) {

            List<EntityID> victim = null;

            try {
                victim = Collections.synchronizedList(VICTIM_BUILDINGS_MAP.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
            }

            if (victim != null && victim.contains(b.getID())) {
                g.setColor(VICTIM_COLOR);
                g.fill(shape);
            }
        }
    }

    private void drawVisited(Building b, Polygon shape, Graphics2D g) {
        if (StaticViewProperties.selectedObject != null) {

            List<EntityID> visited = null;
            boolean isMerged;

            try {
                visited = Collections.synchronizedList(PARTITION_VISITED_BUILDINGS_MAP.get(StaticViewProperties.selectedObject.getID()));
                isMerged = IS_MERGED_VISITED_BUILDINGS_MAP.get(StaticViewProperties.selectedObject.getID());
                if (isMerged) {
                    VISITED_COLOR.brighter();
                }
            } catch (NullPointerException ignored) {
            }

            if (visited != null && visited.contains(b.getID())) {
                g.setColor(VISITED_COLOR);
                g.fill(shape);
            }
        }
    }


}
