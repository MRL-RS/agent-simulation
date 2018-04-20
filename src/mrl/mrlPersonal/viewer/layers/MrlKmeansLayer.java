package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/12/12
 * Time: 10:03 AM
 */
public class MrlKmeansLayer extends StandardViewLayer {
    private static final List<Color> colors = new ArrayList<Color>();
    private static final Stroke STROKE = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static Map<EntityID, List<Point>> CENTER_POINTS = Collections.synchronizedMap(new FastMap<EntityID, List<Point>>());
    public static Map<EntityID, List<List<Point>>> COMMON_POINTS = Collections.synchronizedMap(new FastMap<EntityID, List<List<Point>>>());


    public MrlKmeansLayer() {
        super();
    }

    @Override
    public String getName() {
        return "K-means";
    }


    @Override
    public void initialise(Config config) {
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {

        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        if (StaticViewProperties.selectedObject != null) {
            List<Point> centers = null;
            List<List<Point>> points = null;

            try {
                centers = Collections.synchronizedList(this.CENTER_POINTS.get(StaticViewProperties.selectedObject.getID()));
                points = Collections.synchronizedList(this.COMMON_POINTS.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
            }

            if (centers == null || points == null) {
                return list;
            }

            g.setStroke(STROKE);

            for (List<Point> ps : points) {
                Random random = new Random(ps.hashCode() * MrlViewer.randomValue);
                Color color = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
                g.setColor(color);
                for (Point point : ps) {
                    int x = (transform.xToScreen(point.getX()));
                    int y = (transform.yToScreen(point.getY()));
                    g.fillOval(x, y, 5, 5);
                }
            }
            paintCenters(g, transform, centers);
        }
        return list;
    }

    public void paintCenters(Graphics2D g, ScreenTransform transform, List<Point> centers) {
//        System.out.println("number of center points in viewer:" + KMeansCenters.size());
        g.setStroke(STROKE);
        g.setColor(Color.GREEN);
        ScreenTransform t = transform;
        for (Point point : centers) {
            int x = t.xToScreen(point.getX());
            int y = t.yToScreen(point.getY());
            g.fillOval(x, y, 15, 15);
        }

    }

}