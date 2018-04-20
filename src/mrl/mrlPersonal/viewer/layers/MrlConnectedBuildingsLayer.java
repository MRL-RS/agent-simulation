package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.StaticViewProperties;
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
public class MrlConnectedBuildingsLayer extends MrlAreaLayer {

    //Areas that a specific area is visible from them
    public static Map<EntityID, List<EntityID>> CONNECTED_BUILDINGS = Collections.synchronizedMap(new HashMap<EntityID, List<EntityID>>());

    private static final Color CONNECTED_BUILDING_COLOR = new Color(221, 214, 2, 130);

    /**
     * Construct an area view layer.
     */
    public MrlConnectedBuildingsLayer() {
        super(Area.class);
    }

    private boolean connectedBuildingsInfo;
    private RenderConnectedBuildingsAction connectedBuildingsAction;

    @Override
    public void initialise(Config config) {
        connectedBuildingsInfo = true;
        connectedBuildingsAction = new RenderConnectedBuildingsAction();
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(connectedBuildingsAction));
        return result;
    }


    @Override
    public Shape render(StandardEntity standardEntity, Graphics2D graphics2D, ScreenTransform screenTransform) {
        if (StaticViewProperties.selectedObject != null && standardEntity.getID().equals(StaticViewProperties.selectedObject.getID())) {
            List<EntityID> connectedBuildings = null;
            try {
                if (StaticViewProperties.selectedObject != null) {
                    if (CONNECTED_BUILDINGS != null) {
                        connectedBuildings = CONNECTED_BUILDINGS.get(StaticViewProperties.selectedObject.getID());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (connectedBuildingsInfo && connectedBuildings != null) {
                paintAreas(graphics2D, screenTransform, connectedBuildings, CONNECTED_BUILDING_COLOR);
            }

        }
        return null;


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


    @Override
    public String getName() {
        return "Connected Buildings";
    }

    public void setConnectedBuildingsInfo(boolean connectedBuildingsInfo) {
        this.connectedBuildingsInfo = connectedBuildingsInfo;
        connectedBuildingsAction.update();
    }

    private final class RenderConnectedBuildingsAction extends AbstractAction {
        public RenderConnectedBuildingsAction() {
            super("Connected Buildings");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setConnectedBuildingsInfo(!connectedBuildingsInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(connectedBuildingsInfo));
            putValue(Action.SMALL_ICON, connectedBuildingsInfo ? Icons.TICK : Icons.CROSS);
        }
    }
}
