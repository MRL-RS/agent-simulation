package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.world.routing.highway.Highway;
import mrl.world.routing.highway.Highways;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Random;

/**
 * Created by
 * User: mrl
 * Date: Mar 29, 2011
 * Time: 10:37:09 PM
 */
public class MrlHighwaysLayer extends MrlRoadLayer {

    private static final Stroke STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    static EntityID firstAreaId;
    protected Random random;

    // only baraye didane grid ha dar viewer.
    public static Highways ALL_HIGHWAYS = new Highways();
    private Map<EntityID, Color> roadColorMap = new FastMap<EntityID, Color>();


    public MrlHighwaysLayer() {
        super();
    }

    @Override
    public String getName() {
        return "Highways";
    }

    @Override
    public void initialise(Config config) {
    }

    @Override
    protected void paintShape(Road r, Polygon shape, Graphics2D g) {

        g.setStroke(STROKE);

        if (firstAreaId == null) {
            firstAreaId = r.getID();
        }
        Color COLOUR;

        // for inke faghat yebar draw kone.
        if (firstAreaId == r.getID()) {
            EntityID highwayId;
            try {
                for (Highway highway : ALL_HIGHWAYS) {

                    highwayId = highway.getId();
                    random = new Random(highwayId.getValue() * highwayId.getValue() * MrlViewer.randomValue);
                    COLOUR = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);

                    for (Road road : highway) {
                        roadColorMap.put(road.getID(), COLOUR);
                    }
                }
            } catch (ConcurrentModificationException ignored) {
            }
        }

        g.setColor(roadColorMap.get(r.getID()));
        g.fill(shape);

    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        return result;
    }
}
