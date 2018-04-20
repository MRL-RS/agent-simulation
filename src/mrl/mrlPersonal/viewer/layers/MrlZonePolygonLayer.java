package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.object.mrlZoneEntity.MrlZones;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * Created by Mostafa.
 * User: roohola
 * Date: 6/4/11
 * Time: 9:00 PM
 */
public class MrlZonePolygonLayer extends StandardViewLayer {
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private boolean visible;
    protected Random random;

    public static MrlZones ZONES = null;

    @Override
    public void initialise(Config config) {
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        return result;
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {

        g.setStroke(STROKE);
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        int id;
        Color COLOUR;
        if (ZONES == null) {
            return list;
        }
        for (MrlZone zone : ZONES) {
            id = zone.getId();
            random = new Random(id * id * MrlViewer.randomValue);
            COLOUR = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
            g.setColor(COLOUR);

            int count = zone.getPolygon().npoints;
            int[] xs = new int[count];
            int[] ys = new int[count];
            int xp = 0;
            int yp = 0;
            for (int i = 0; i < count; ++i) {
                xs[i] = t.xToScreen(zone.getPolygon().xpoints[i]);
                ys[i] = t.yToScreen(zone.getPolygon().ypoints[i]);

                xp += t.xToScreen(zone.getPolygon().xpoints[i]);
                yp += t.yToScreen(zone.getPolygon().ypoints[i]);
            }

            Polygon shape = new Polygon(xs, ys, count);
            g.drawString(String.valueOf(zone.getId()), xp / count, yp / count);
            g.draw(shape);

//            Circle2D circle2D=new Circle2D(t.xToScreen
//                     (zone.getCenter().getX()),t.yToScreen(zone.getCenter().getY()),10d);
//
//             circle2D.draw(g);


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
        return "Zone Polygon";
    }
}