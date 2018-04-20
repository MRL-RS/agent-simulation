package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.common.clustering.CivilianCluster;
import mrl.common.clustering.Cluster;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Vahid Hooshangi
 */
public class MrlCivilianClusterLayer extends StandardViewLayer {
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private boolean visible;
    public static final Map<EntityID, java.util.List<CivilianCluster>> CONVEX_HULLS_MAP = new FastMap<EntityID, java.util.List<CivilianCluster>>();
    private static final Color ConColour = Color.GREEN;
    public static Map<EntityID, StandardEntity> BUILDINGS = new FastMap<EntityID, StandardEntity>();//TODO :just for test

    @Override
    public void initialise(Config config) {
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        return new ArrayList<JMenuItem>();
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {
        for (StandardEntity entity : world.getEntitiesOfType(StandardEntityURN.BUILDING)) {
            BUILDINGS.put(entity.getID(), entity);
        }


        java.util.List<Integer> xs = new ArrayList<Integer>();
        java.util.List<Integer> ys = new ArrayList<Integer>();
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();


        java.util.List<CivilianCluster> clusterList = new ArrayList<CivilianCluster>();
        if (StaticViewProperties.selectedObject != null) {
            try {
                clusterList = Collections.synchronizedList(CONVEX_HULLS_MAP.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
            }
        }


        Polygon polygon;
        for (Cluster cluster : clusterList) {
            polygon = cluster.getConvexHullObject().getConvexPolygon();
            xs.clear();
            ys.clear();

            for (int x : polygon.xpoints) {
                xs.add(t.xToScreen(x));
            }
            for (int y : polygon.ypoints) {
                ys.add(t.yToScreen(y));
            }

            Polygon poly = new Polygon(listToArray(xs), listToArray(ys), polygon.npoints);

            g.setColor(ConColour);
            g.setStroke(STROKE);
            g.draw(poly);
            if (cluster instanceof CivilianCluster) {
                g.setColor(Color.WHITE);
                //g.drawString(String.valueOf(((CivilianCluster) cluster).getFinalValue()), (int) poly.getBounds().getX(), (int) poly.getBounds().getY());
                g.drawString(String.valueOf(cluster.getValue()), (int) poly.getBounds().getX(), (int) poly.getBounds().getY());//Mostafa
            }
        }
        return list;
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
        return "Civilian Cluster";
    }
}

