package mrl.mrlPersonal.viewer.layers;

import mrl.firebrigade.tools.MrlRay;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/13/13
 * Time: 4:54 PM
 */
public class MrlAreaVisibilityLayer extends MrlAreaLayer {

    //Areas that a specific area is visible from them
    public static Map<EntityID, Set<EntityID>> VISIBLE_FROM_AREAS = Collections.synchronizedMap(new HashMap<EntityID, Set<EntityID>>());

    //Areas that are visible from specific area
    public static Map<EntityID, List<EntityID>> OBSERVABLE_AREAS = Collections.synchronizedMap(new HashMap<EntityID, List<EntityID>>());

    //Lines of sight for each area
    public static Map<EntityID, List<MrlRay>> LINE_OF_SIGHT_RAYS = Collections.synchronizedMap(new HashMap<EntityID, List<MrlRay>>());


    private static final Color VISIBLE_FROM_COLOR = new Color(176, 0, 168, 128);
    private static final Color OBSERVABLE_COLOR = new Color(0, 176, 84, 128);
    private static final Color LINE_OF_SIGHT_COLOR = new Color(3, 99, 164, 149);

    /**
     * Construct an area view layer.
     */
    public MrlAreaVisibilityLayer() {
        super(Area.class);
    }

    private boolean lineOfSightInfo;
    private RenderLineOfSightAction lineOfSightAction;

    private boolean visibleFromInfo;
    private RenderVisibleFromAction visibleFromAction;

    private boolean observableAreasInfo;
    private RenderObservableAreasAction observableAreasAction;


    @Override
    public void initialise(Config config) {
        visibleFromInfo = true;
        visibleFromAction = new RenderVisibleFromAction();

        observableAreasInfo = true;
        observableAreasAction = new RenderObservableAreasAction();

        lineOfSightInfo = false;
        lineOfSightAction = new RenderLineOfSightAction();
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();

        result.add(new JMenuItem(visibleFromAction));
        result.add(new JMenuItem(observableAreasAction));
//        result.add(new JMenuItem(lineOfSightAction));

        return result;
    }


    @Override
    public Shape render(StandardEntity standardEntity, Graphics2D graphics2D, ScreenTransform screenTransform) {


        if (StaticViewProperties.selectedObject != null && standardEntity.getID().equals(StaticViewProperties.selectedObject.getID())) {
            Set<EntityID> visibleForms = null;
            List<EntityID> observableAreas = null;
            List<MrlRay> lineOfSights = null;
            try {
                if (StaticViewProperties.selectedObject != null) {
                    if (VISIBLE_FROM_AREAS != null) {
                        visibleForms = VISIBLE_FROM_AREAS.get(StaticViewProperties.selectedObject.getID());
                    }
                    if (OBSERVABLE_AREAS != null) {
                        observableAreas = OBSERVABLE_AREAS.get(StaticViewProperties.selectedObject.getID());
                    }
                    if (LINE_OF_SIGHT_RAYS != null) {
                        lineOfSights = LINE_OF_SIGHT_RAYS.get(StaticViewProperties.selectedObject.getID());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (visibleFromInfo) {
                paintAreas(graphics2D, screenTransform, visibleForms, VISIBLE_FROM_COLOR);
            }
            if (observableAreasInfo) {
                paintAreas(graphics2D, screenTransform, observableAreas, OBSERVABLE_COLOR);
            }
            if (lineOfSightInfo) {
                paintLineOfSights(graphics2D, screenTransform, lineOfSights, LINE_OF_SIGHT_COLOR);
            }

        }
        return null;


    }

    private void paintLineOfSights(Graphics2D graphics2D, ScreenTransform s, List<MrlRay> lineOfSights, Color color) {

        int x, y;
        Point2D head;
        Point2D tail;
        Pair<Integer, Integer> headOnScreen;
        Pair<Integer, Integer> tailOnScreen;
        graphics2D.setColor(color);
        if (lineOfSights != null) {
            for (MrlRay ray : lineOfSights) {

                head = ray.getRay().getOrigin();
                tail = ray.getRay().getEndPoint();

                headOnScreen = new Pair<Integer, Integer>(s.xToScreen(head.getX()), s.yToScreen(head.getY()));
                tailOnScreen = new Pair<Integer, Integer>(s.xToScreen(tail.getX()), s.yToScreen(tail.getY()));

                graphics2D.drawLine(headOnScreen.first(), headOnScreen.second(), tailOnScreen.first(), tailOnScreen.second());

            }
        }

    }

    private boolean paintAreas(Graphics2D graphics2D, ScreenTransform screenTransform, List<EntityID> areaIDs, Color color) {
        graphics2D.setColor(color);
        if (areaIDs != null) {
            Area area;
            List<Edge> edges;
            for (EntityID id : areaIDs) {
                area = (Area) world.getEntity(id);
                edges = area.getEdges();
                if (edges.isEmpty()) {
                    return true;
                }
                int count = edges.size();
                int[] xs = new int[count];
                int[] ys = new int[count];
                int i = 0;
                for (Edge e : edges) {
                    xs[i] = screenTransform.xToScreen(e.getStartX());
                    ys[i] = screenTransform.yToScreen(e.getStartY());
                    ++i;
                }
                Polygon shape = new Polygon(xs, ys, count);
                graphics2D.fill(shape);
            }
        }
        return false;
    }

    private boolean paintAreas(Graphics2D graphics2D, ScreenTransform screenTransform, Set<EntityID> areaIDs, Color color) {
        graphics2D.setColor(color);
        if (areaIDs != null) {
            Area area;
            List<Edge> edges;
            for (EntityID id : areaIDs) {
                area = (Area) world.getEntity(id);
                edges = area.getEdges();
                if (edges.isEmpty()) {
                    return true;
                }
                int count = edges.size();
                int[] xs = new int[count];
                int[] ys = new int[count];
                int i = 0;
                for (Edge e : edges) {
                    xs[i] = screenTransform.xToScreen(e.getStartX());
                    ys[i] = screenTransform.yToScreen(e.getStartY());
                    ++i;
                }
                Polygon shape = new Polygon(xs, ys, count);
                graphics2D.fill(shape);
            }
        }
        return false;
    }


    @Override
    public String getName() {
        return "Area Visibility";
    }

    public void setVisibleFromInfo(boolean visibleFromInfo) {
        this.visibleFromInfo = visibleFromInfo;
        visibleFromAction.update();
    }

    public void setObservableAreasInfo(boolean observableAreasInfo) {
        this.observableAreasInfo = observableAreasInfo;
        observableAreasAction.update();
    }

    public void setLineOfSight(boolean lineOfSight) {
        this.lineOfSightInfo = lineOfSight;
        lineOfSightAction.update();
    }


    private final class RenderVisibleFromAction extends AbstractAction {
        public RenderVisibleFromAction() {
            super("Visible From");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisibleFromInfo(!visibleFromInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(visibleFromInfo));
            putValue(Action.SMALL_ICON, visibleFromInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class RenderObservableAreasAction extends AbstractAction {
        public RenderObservableAreasAction() {
            super("Observable Areas");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setObservableAreasInfo(!observableAreasInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(observableAreasInfo));
            putValue(Action.SMALL_ICON, observableAreasInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class RenderLineOfSightAction extends AbstractAction {
        public RenderLineOfSightAction() {
            super("Line Of Sight");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setLineOfSight(!lineOfSightInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(lineOfSightInfo));
            putValue(Action.SMALL_ICON, lineOfSightInfo ? Icons.TICK : Icons.CROSS);
        }
    }


}
