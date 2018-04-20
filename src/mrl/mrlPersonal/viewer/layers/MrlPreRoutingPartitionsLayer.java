package mrl.mrlPersonal.viewer.layers;

import mrl.partition.Partition;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: Pooyad
 * Date: Jan 20, 2011
 * Time: 6:21:14 PM
 */
public class MrlPreRoutingPartitionsLayer extends StandardViewLayer {
    private static final Color COLOUR = Color.BLUE.brighter();
    private static final Color COLOUR_Rectangle = Color.RED.brighter();
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private boolean visible;

    public static ArrayList<Partition> PARTITIONS = new ArrayList<Partition>();

    public static List<Rectangle> rectangles = new ArrayList<Rectangle>();

//    public MrlPreRoutingPartitionsLayer(WorldModel world) {
//
//        this.world=world;
//    }


    @Override
    public void initialise(Config config) {
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {

        g.setColor(COLOUR);
        g.setStroke(STROKE);
        ScreenTransform t = transform;

        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        if (rectangles == null) {
            return list;
        }
        for (Partition partition : PARTITIONS) {

            int xs[] = new int[4];
            int ys[] = new int[4];

            xs[0] = t.xToScreen(partition.getPolygon().xpoints[0]);
            ys[0] = t.yToScreen(partition.getPolygon().ypoints[0]);

            xs[1] = t.xToScreen(partition.getPolygon().xpoints[1]);
            ys[1] = t.yToScreen(partition.getPolygon().ypoints[1]);

            xs[2] = t.xToScreen(partition.getPolygon().xpoints[2]);
            ys[2] = t.yToScreen(partition.getPolygon().ypoints[2]);

            xs[3] = t.xToScreen(partition.getPolygon().xpoints[3]);
            ys[3] = t.yToScreen(partition.getPolygon().ypoints[3]);


            Polygon shape = new Polygon(xs, ys, 4);
//            Polygon polygon = new Polygon();
//            tempX = t.xToScreen(tempX);
//            tempY= t.yToScreen(tempY);
//            polygon.addPoint(tempX, tempY);
//            polygon.addPoint(tempX + eachPartitionWidth, tempY);
//            polygon.addPoint(tempX + eachPartitionWidth, tempY + eachPartitionHeight);
//            polygon.addPoint(tempX, tempY + eachPartitionHeight);
//            list.add(new RenderedObject(partitions,shape));

            g.draw(shape);

        }


        if (rectangles != null) {
            g.setColor(COLOUR_Rectangle);

            for (Rectangle r : rectangles) {

                int xs[] = new int[4];
                int ys[] = new int[4];

                xs[0] = t.xToScreen(r.getX());
                ys[0] = t.yToScreen(r.getY());

                xs[1] = t.xToScreen(r.getX() + r.getWidth());
                ys[1] = t.yToScreen(r.getY());

                xs[2] = t.xToScreen(r.getX() + r.getWidth());
                ys[2] = t.yToScreen(r.getY() + r.getHeight());

                xs[3] = t.xToScreen(r.getX());
                ys[3] = t.yToScreen(r.getY() + r.getHeight());


                Polygon shape = new Polygon(xs, ys, 4);
//            Polygon polygon = new Polygon();
//            tempX = t.xToScreen(tempX);
//            tempY= t.yToScreen(tempY);
//            polygon.addPoint(tempX, tempY);
//            polygon.addPoint(tempX + eachPartitionWidth, tempY);
//            polygon.addPoint(tempX + eachPartitionWidth, tempY + eachPartitionHeight);
//            polygon.addPoint(tempX, tempY + eachPartitionHeight);
//            list.add(new RenderedObject(partitions,shape));

                g.draw(shape);

            }

        }
        return list;
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
    public String getName() {
        return "PreRouting Partitions";
    }

}
