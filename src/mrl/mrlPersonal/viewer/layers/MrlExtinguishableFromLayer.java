package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
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
 * Date: 3/8/13
 * Time: 5:18 PM
 * Author: Mostafa Movahedi
 */
public class MrlExtinguishableFromLayer extends MrlAreaLayer {

    //Areas that a specific area is visible from them
    public static Map<EntityID, List<EntityID>> EXTINGUISHABLE_FROM = Collections.synchronizedMap(new HashMap<EntityID, List<EntityID>>());
    public static Map<EntityID, List<MrlBuilding>> BUILDINGS_IN_EXTINGUISH_RANGE = Collections.synchronizedMap(new HashMap<EntityID, List<MrlBuilding>>());

    private static final Color EXTINGUISHABLE_FROM_COLOR = new Color(221, 214, 2, 130);
    private static final Color BUILDINGS_IN_EXTINGUISH_RANGE_COLOR = new Color(176, 0, 168, 130);

    /**
     * Construct an area view layer.
     */
    public MrlExtinguishableFromLayer() {
        super(Area.class);
    }

    private boolean extinguishableFromInfo;
    private RenderExtinguishableFromAction extinguishableFromAction;

    private boolean buildingsInExtinguishRangeInfo;
    private RenderBuildingsInExtinguishRangeAction buildingsInExtinguishRangeAction;

    @Override
    public void initialise(Config config) {
        extinguishableFromInfo = false;
        buildingsInExtinguishRangeInfo = true;
        extinguishableFromAction = new RenderExtinguishableFromAction();
        buildingsInExtinguishRangeAction = new RenderBuildingsInExtinguishRangeAction();
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(extinguishableFromAction));
        result.add(new JMenuItem(buildingsInExtinguishRangeAction));
        return result;
    }


    @Override
    public Shape render(StandardEntity standardEntity, Graphics2D graphics2D, ScreenTransform screenTransform) {
        if (StaticViewProperties.selectedObject != null && standardEntity.getID().equals(StaticViewProperties.selectedObject.getID())) {
            List<EntityID> extinguishableFrom = null;
            List<MrlBuilding> buildingsInExtinguishRange = null;
            try {
                if (StaticViewProperties.selectedObject != null) {
                    if (EXTINGUISHABLE_FROM != null) {
                        extinguishableFrom = EXTINGUISHABLE_FROM.get(StaticViewProperties.selectedObject.getID());
                    }
                    if (BUILDINGS_IN_EXTINGUISH_RANGE != null) {
                        buildingsInExtinguishRange = BUILDINGS_IN_EXTINGUISH_RANGE.get(StaticViewProperties.selectedObject.getID());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (extinguishableFromInfo && extinguishableFrom != null) {
                paintExtinguishableFrom(graphics2D, screenTransform, extinguishableFrom, EXTINGUISHABLE_FROM_COLOR);
            }
            if (buildingsInExtinguishRangeInfo && buildingsInExtinguishRange != null) {
                paintBuildingsInExtinguishRange(graphics2D, screenTransform, buildingsInExtinguishRange, BUILDINGS_IN_EXTINGUISH_RANGE_COLOR);
            }

        }
        return null;


    }

    private boolean paintExtinguishableFrom(Graphics2D graphics2D, ScreenTransform screenTransform, List<EntityID> areaIDs, Color color) {
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

    private boolean paintBuildingsInExtinguishRange(Graphics2D graphics2D, ScreenTransform screenTransform, List<MrlBuilding> mrlBuildings, Color color) {
        graphics2D.setColor(color);
        if (mrlBuildings != null) {
            Area area;
            List<Edge> edges;
            for (MrlBuilding mrlBuilding : mrlBuildings) {
                area = mrlBuilding.getSelfBuilding();
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
        return "Extinguishable Areas";
    }

    public void setExtinguishableFromInfo(boolean extinguishableFromInfo) {
        this.extinguishableFromInfo = extinguishableFromInfo;
        extinguishableFromAction.update();
    }

    private final class RenderExtinguishableFromAction extends AbstractAction {
        public RenderExtinguishableFromAction() {
            super("Extinguishable From");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setExtinguishableFromInfo(!extinguishableFromInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(extinguishableFromInfo));
            putValue(Action.SMALL_ICON, extinguishableFromInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setBuildingsInExtinguishRangeInfo(boolean buildingsInExtinguishRangeInfo) {
        this.buildingsInExtinguishRangeInfo = buildingsInExtinguishRangeInfo;
        buildingsInExtinguishRangeAction.update();
    }

    private final class RenderBuildingsInExtinguishRangeAction extends AbstractAction {
        public RenderBuildingsInExtinguishRangeAction() {
            super("Buildings In Extinguish Range");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setBuildingsInExtinguishRangeInfo(!buildingsInExtinguishRangeInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(buildingsInExtinguishRangeInfo));
            putValue(Action.SMALL_ICON, buildingsInExtinguishRangeInfo ? Icons.TICK : Icons.CROSS);
        }
    }
}
