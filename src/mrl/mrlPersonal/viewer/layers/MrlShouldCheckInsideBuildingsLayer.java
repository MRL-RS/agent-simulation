package mrl.mrlPersonal.viewer.layers;

import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: mrl
 * Date: 3/27/12
 * Time: 7:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class MrlShouldCheckInsideBuildingsLayer extends StandardViewLayer {
    private boolean visible;
    public static java.util.List<MrlBuilding> SHOULD_CHECK_INSIDE_BUILDINGS = new ArrayList<MrlBuilding>();

    @Override
    public void initialise(Config config) {
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        return new ArrayList<JMenuItem>();
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {
        Color color = Color.PINK;
        int[] xp;
        int[] yp;
        int count;
        int i;
        Polygon shape;
        Building b;

        g.setColor(color);
        Collection<MrlBuilding> mrlBuildings = null;

        try {
            mrlBuildings = SHOULD_CHECK_INSIDE_BUILDINGS;
            if (mrlBuildings != null) {
                for (MrlBuilding building : mrlBuildings) {
                    b = (Building) world.getEntity(building.getID());
                    count = b.getEdges().size();
                    xp = new int[count];
                    yp = new int[count];

                    i = 0;

                    for (Edge e : b.getEdges()) {
                        xp[i] = t.xToScreen(e.getStartX());
                        yp[i] = t.yToScreen(e.getStartY());
                        ++i;
                    }
                    shape = new Polygon(xp, yp, count);
                    g.fill(shape);
                }
            }

        } catch (Exception e) {
            //do nothing
        }
        return new ArrayList<RenderedObject>();
    }


    public static int[] listToArray(java.util.List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
            array[i] = list.get(i);
        return array;
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
        return "UnNormal Buildings";
    }
}
