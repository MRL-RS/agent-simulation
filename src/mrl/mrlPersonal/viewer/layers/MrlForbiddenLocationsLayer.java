package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/13/13
 * Time: 4:54 PM
 */
public class MrlForbiddenLocationsLayer extends MrlAreaLayer {

    //Forbidden locations for fireBrigade to stand for extinguish
    public static Map<EntityID, List<EntityID>> FORBIDDEN_LOCATIONS = new HashMap<EntityID, List<EntityID>>();


    private static final Color FORBIDDEN_LOCATIONS_COLOR = new Color(4, 4, 4, 178);

    /**
     * Construct an area view layer.
     */
    public MrlForbiddenLocationsLayer() {
        super(Area.class);
    }

    @Override
    public void initialise(Config config) {
    }

    @Override
    protected void paintShape(Area a, Polygon shape, Graphics2D g) {
        if (StaticViewProperties.selectedObject != null) {

            List<EntityID> forbiddenLocations = null;
            try {
                forbiddenLocations = Collections.synchronizedList(FORBIDDEN_LOCATIONS.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
            }

            if (forbiddenLocations != null && forbiddenLocations.contains(a.getID())) {
                g.setColor(FORBIDDEN_LOCATIONS_COLOR);
                g.fill(shape);
            }
        }
    }

    /*@Override
    public Shape render(StandardEntity standardEntity, Graphics2D graphics2D, ScreenTransform screenTransform) {
        if (StaticViewProperties.selectedObject != null && standardEntity.getID().equals(StaticViewProperties.selectedObject.getID())) {
            List<EntityID> forbiddenLocations = null;
            try {
                if (StaticViewProperties.selectedObject != null) {
                    forbiddenLocations = Collections.synchronizedList(FORBIDDEN_LOCATIONS.get(StaticViewProperties.selectedObject.getID()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            paintAreas(graphics2D, screenTransform, forbiddenLocations, FORBIDDEN_LOCATIONS_COLOR);
        }
        return null;
    }*/

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
        return "Forbidden Locations";
    }
}
