package mrl.mrlPersonal.viewer.layers;

import mrl.partitioning.Partition;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User: pooyad
 * Date: 5/8/11
 * Time: 4:09 AM
 */
public class MrlRendezvousLayer extends MrlAreaLayer<Road> {

    private static final Color COLOUR = Color.ORANGE;
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private boolean visible;

    public static List<Partition> partitions = new ArrayList<Partition>();

    /**
     * Construct an area view layer.
     */
    public MrlRendezvousLayer() {
        super(Road.class);
    }


    @Override
    public Shape render(Road area, Graphics2D g, ScreenTransform t) {
        Polygon shape_Road;
        Shape s = new Polygon();
        g.setColor(Color.RED);
        int npoints = 0;
        Road road;
        for (Partition partition : partitions) {
            for (EntityID rendezvous : partition.getRendezvous()) {
//                for (Road road : rendezvous.getRoadList()) {
                shape_Road = new Polygon();
                road = (Road) world.getEntity(rendezvous);
                npoints = road.getApexList().length;
                for (int i = 0; i < npoints; i += 2) {
                    shape_Road.addPoint(t.xToScreen(road.getApexList()[i]), t.yToScreen(road.getApexList()[i + 1]));
                }
                g.fill(shape_Road);
//                }
            }
        }
        return s;
    }

    @Override
    protected void paintShape(Road road, Polygon shape, Graphics2D g) {
        if (partitions.contains(road)) {
            drawShape(shape, g);
        }
    }

    private void drawShape(Polygon shape, Graphics2D g) {
        g.setColor(COLOUR);
        g.fill(shape);
    }

    @Override
    public String getName() {
        return "Rendezvous";
    }
}
