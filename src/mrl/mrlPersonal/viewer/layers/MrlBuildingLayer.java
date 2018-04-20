package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.mrlPersonal.viewer.StandardEntityToPaint;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 12, 2010
 * Time: 4:47:21 PM
 */
public class MrlBuildingLayer extends MrlAreaLayer<Building> {
    public static Map<EntityID, MrlBuilding> MRL_BUILDINGS_MAP = new HashMap<EntityID, MrlBuilding>();
    public static Map<EntityID, Double> BUILDING_VALUE = new HashMap<EntityID, Double>();
    public static Map<EntityID, Set<EntityID>> BLOCKED_BUILDINGS = new FastMap<EntityID, Set<EntityID>>();
    public static Map<EntityID, Set<EntityID>> POSSIBLE_BUILDINGS = new HashMap<EntityID, Set<EntityID>>();            //map(agentID => BuildingIDs)
    public static Map<EntityID, Set<EntityID>> UNREACHABLE_POSSIBLE_BUILDINGS = new HashMap<EntityID, Set<EntityID>>();            //map(agentID => BuildingIDs)
    public static Map<EntityID, Set<EntityID>> EMPTY_BUILDINGS = new HashMap<EntityID, Set<EntityID>>();            //map(agentID => BuildingIDs)

    private static final Color HEATING = new Color(176, 176, 56, 128);
    private static final Color BURNING = new Color(204, 122, 50, 128);
    private static final Color INFERNO = new Color(160, 52, 52, 128);
    private static final Color WATER_DAMAGE = new Color(50, 120, 130, 128);
    private static final Color MINOR_DAMAGE = new Color(100, 140, 210, 128);
    private static final Color MODERATE_DAMAGE = new Color(100, 70, 190, 128);
    private static final Color SEVERE_DAMAGE = new Color(80, 60, 140, 128);
    private static final Color BURNT_OUT = new Color(0, 0, 0, 255);

    private static final Color OUTLINE_COLOUR = Color.GRAY.darker().darker();

    private static final Color REFUGE_BUILDING_COLOR = Color.GREEN.darker();
    private static final Color CENTER_BUILDING_COLOR = Color.WHITE.brighter().brighter();
    private static final Color BURNING_REFUGE_COLOR = Color.RED.darker().darker();
    private static final Color GAS_STATION_COLOR = new Color(230, 180, 200);
    private static final Color BURNING_GAS_STATION_COLOR = new Color(250, 80, 200);

    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    private Action showIdsAction;
    private Action renderPossibleBuildings;
    private Action renderEmptyBuildings;
    private Action renderUnreachableBuildings;
    private Action renderCenterVisitShapes;

    private boolean showUnreachableBuildings;

    public static Set<Building> VIEWER_BUILDINGS_SET = Collections.synchronizedSet(new FastSet<Building>());


    /**
     * Construct a building view layer.
     */
    public MrlBuildingLayer() {
        super(Building.class);
    }

    @Override
    public String getName() {
        return "Building shapes";
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
        VIEWER_BUILDINGS_SET.add(b);
        drawBrokenness(b, shape, g);
        drawFieriness(b, shape, g);
        if (showCenterVisitShapes && StaticViewProperties.selectedObject != null && StaticViewProperties.selectedObject.equals(b)) {
            drawCenterVisitShapes(b.getID(), shape, g);
        }
    }

    private void drawCenterVisitShapes(EntityID b, Polygon shape, Graphics2D g) {
        MrlBuilding mrlBuilding = MRL_BUILDINGS_MAP.get(b);
        if (mrlBuilding == null)
            return;
        for (Polygon polygon : mrlBuilding.getCenterVisitShapes()) {
            int size = polygon.npoints;
            int[] xs = new int[size];
            int[] ys = new int[size];
            int[] xpoints = polygon.xpoints;
            for (int i = 0, xpointsLength = xpoints.length; i < xpointsLength; i++) {
                int x = xpoints[i];
                xs[i] = transform.xToScreen(x);
            }
            int[] ypoints = polygon.ypoints;
            for (int i = 0, ypointsLength = ypoints.length; i < ypointsLength; i++) {
                int y = ypoints[i];
                ys[i] = transform.yToScreen(y);
            }
            Polygon transformedPolygon = new Polygon(xs, ys, size);
            g.setColor(Color.GREEN);
            g.draw(transformedPolygon);

        }
        java.util.List<EntityID> roadIDList = new ArrayList<EntityID>(mrlBuilding.getCenterVisitRoadShapes().keySet());
        for (EntityID roadID : roadIDList) {
            for (Polygon polygon : mrlBuilding.getCenterVisitRoadShapes().get(roadID)) {
                int size = polygon.npoints;
                int[] xs = new int[size];
                int[] ys = new int[size];
                int[] xpoints = polygon.xpoints;
                for (int i = 0, xpointsLength = xpoints.length; i < xpointsLength; i++) {
                    int x = xpoints[i];
                    xs[i] = transform.xToScreen(x);
                }
                int[] ypoints = polygon.ypoints;
                for (int i = 0, ypointsLength = ypoints.length; i < ypointsLength; i++) {
                    int y = ypoints[i];
                    ys[i] = transform.yToScreen(y);
                }
                Polygon transformedPolygon = new Polygon(xs, ys, size);
                g.setColor(Color.CYAN);

                g.fill(transformedPolygon);

            }
        }
    }

    private void drawFieriness(Building b, Polygon shape, Graphics2D g) {

//        try {
//            MrlZones.BURNING_BUILDING_LIST.put(b.getID(), b);
//        } catch (Exception ignore) {
//        }

        StandardEntityToPaint entityToPaint = StaticViewProperties.getPaintObject(b);
        if (entityToPaint != null) {
            g.setColor(entityToPaint.getColor());
            g.fill(shape);
            return;

        }
        if (b == StaticViewProperties.selectedObject) {
            g.setColor(Color.MAGENTA);
            g.fill(shape);
            return;
        }
        if (b instanceof Refuge) {
            g.setColor(REFUGE_BUILDING_COLOR);
            if (b.isFierynessDefined() && b.getFieryness() > 0) {
                g.setColor(BURNING_REFUGE_COLOR);
            }
            g.fill(shape);
        } else if (b instanceof GasStation) {
            g.setColor(GAS_STATION_COLOR);
            if (b.isFierynessDefined() && b.getFieryness() > 0) {
                g.setColor(BURNING_GAS_STATION_COLOR);
            }
            g.fill(shape);
        } else if ((b instanceof AmbulanceCentre) || (b instanceof FireStation) || (b instanceof PoliceOffice)) {
            g.setColor(CENTER_BUILDING_COLOR);
            if (b.isFierynessDefined() && b.getFieryness() > 0) {
                g.setColor(BURNING_REFUGE_COLOR);
            }
            g.fill(shape);
        }
        if (!b.isFierynessDefined()) {
            return;
        }
        switch (b.getFierynessEnum()) {
            case UNBURNT:
                return;
            case HEATING:
                g.setColor(HEATING);
                break;
            case BURNING:
                g.setColor(BURNING);
                break;
            case INFERNO:
                g.setColor(INFERNO);
                break;
            case WATER_DAMAGE:
                g.setColor(WATER_DAMAGE);
                break;
            case MINOR_DAMAGE:
                g.setColor(MINOR_DAMAGE);
                break;
            case MODERATE_DAMAGE:
                g.setColor(MODERATE_DAMAGE);
                break;
            case SEVERE_DAMAGE:
                g.setColor(SEVERE_DAMAGE);
                break;
            case BURNT_OUT:
                g.setColor(BURNT_OUT);
                break;
            default:
                throw new IllegalArgumentException("Don't know how to render fieriness " + b.getFierynessEnum());
        }
        g.fill(shape);
    }

    private void drawBrokenness(Building b, Shape shape, Graphics2D g) {
        int brokenness = b.getBrokenness();
        // CHECK STYLE:OFF:MagicNumber
        int colour = Math.max(0, 135 - brokenness / 2);
        // CHECK STYLE:ON:MagicNumber
        g.setColor(new Color(colour, colour, colour));
        if (showUnreachableBuildings) {
            if (StaticViewProperties.selectedObject != null && BLOCKED_BUILDINGS.containsKey(StaticViewProperties.selectedObject.getID()) &&
                    BLOCKED_BUILDINGS.get(StaticViewProperties.selectedObject.getID()).contains(b.getID())) {
                g.setColor(Color.RED.darker().darker());
            }
        }
        if (showPossibleBuildings) {
            if (StaticViewProperties.selectedObject != null && POSSIBLE_BUILDINGS.containsKey(StaticViewProperties.selectedObject.getID())) {
                Set<EntityID> possibles = POSSIBLE_BUILDINGS.get(StaticViewProperties.selectedObject.getID());
                if (possibles.contains(b.getID())) {
                    g.setColor(Color.WHITE);
                }
            }
            if (StaticViewProperties.selectedObject != null && UNREACHABLE_POSSIBLE_BUILDINGS.containsKey(StaticViewProperties.selectedObject.getID())) {
                Set<EntityID> possibles = UNREACHABLE_POSSIBLE_BUILDINGS.get(StaticViewProperties.selectedObject.getID());
                if (possibles.contains(b.getID())) {
                    g.setColor(Color.YELLOW);
                }
            }
        }

        if (showEmptyBuildings) {
            if (StaticViewProperties.selectedObject != null && EMPTY_BUILDINGS.containsKey(StaticViewProperties.selectedObject.getID())) {
                Set<EntityID> emptyBuildings = EMPTY_BUILDINGS.get(StaticViewProperties.selectedObject.getID());
                if (emptyBuildings.contains(b.getID())) {
                    g.setColor(Color.WHITE);
                }
            }

        }





        g.fill(shape);
    }

    @Override
    public void initialise(Config config) {
        showBIds = false;
        showPossibleBuildings = false;
        showEmptyBuildings = false;
        showUnreachableBuildings = false;
        showCenterVisitShapes = false;
        showIdsAction = new ShowIdsAction();
        renderPossibleBuildings = new RenderPossibleBuildings();
        renderEmptyBuildings =new RenderEmptyBuildings();
        renderUnreachableBuildings = new RenderUnreachableBuildings();
        renderCenterVisitShapes = new RenderCenterVisitShapes();
    }

    private final class ShowIdsAction extends AbstractAction {
        public ShowIdsAction() {
            super("show ids");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showBIds));
            putValue(Action.SMALL_ICON, showBIds ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showBIds = !showBIds;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showBIds));
            putValue(Action.SMALL_ICON, showBIds ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(showIdsAction));
        result.add(new JMenuItem(renderPossibleBuildings));
        result.add(new JMenuItem(renderUnreachableBuildings));
        result.add(new JMenuItem(renderCenterVisitShapes));
        result.add(new JMenuItem(renderEmptyBuildings));

        return result;
    }

    private boolean showPossibleBuildings;
    private boolean showEmptyBuildings;

    private final class RenderPossibleBuildings extends AbstractAction {
        public RenderPossibleBuildings() {
            super("Possible Buildings");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showPossibleBuildings));
            putValue(Action.SMALL_ICON, showPossibleBuildings ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showPossibleBuildings = !showPossibleBuildings;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showPossibleBuildings));
            putValue(Action.SMALL_ICON, showPossibleBuildings ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }
    private final class RenderEmptyBuildings extends AbstractAction {
        public RenderEmptyBuildings() {
            super("Empty Buildings");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showEmptyBuildings));
            putValue(Action.SMALL_ICON, showEmptyBuildings ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showEmptyBuildings = !showEmptyBuildings;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showEmptyBuildings));
            putValue(Action.SMALL_ICON, showEmptyBuildings ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private final class RenderUnreachableBuildings extends AbstractAction {
        public RenderUnreachableBuildings() {
            super("Unreachable Buildings");    //To change body of overridden methods use File | Settings | File Templates.
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showUnreachableBuildings));
            putValue(Action.SMALL_ICON, showUnreachableBuildings ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showUnreachableBuildings = !showUnreachableBuildings;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showUnreachableBuildings));
            putValue(Action.SMALL_ICON, showUnreachableBuildings ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private final class RenderCenterVisitShapes extends AbstractAction {
        public RenderCenterVisitShapes() {
            super("center visit shapes");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showCenterVisitShapes));
            putValue(Action.SMALL_ICON, showCenterVisitShapes ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showCenterVisitShapes = !showCenterVisitShapes;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showCenterVisitShapes));
            putValue(Action.SMALL_ICON, showCenterVisitShapes ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }
}
