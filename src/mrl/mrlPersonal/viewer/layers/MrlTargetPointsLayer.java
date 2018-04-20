package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import math.geom2d.conic.Circle2D;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 *
 */
public class MrlTargetPointsLayer extends MrlAreaLayer<Building> {
    private boolean visible;
    public static Map<EntityID, java.util.List<Pair<Point, Double>>> TARGET_POINTS = new FastMap<EntityID, java.util.List<Pair<Point, Double>>>();
    private static final Color valueColor = Color.WHITE;

    /**
     * Construct an area view layer.
     */
    public MrlTargetPointsLayer() {
        super(Building.class);
    }

    @Override
    public void initialise(Config config) {
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        return new ArrayList<JMenuItem>();
    }


    @Override
    public Shape render(Building building, Graphics2D g, ScreenTransform t) {
        if (StaticViewProperties.selectedObject != null) {
            if (TARGET_POINTS.get(StaticViewProperties.selectedObject.getID()) != null) {
                java.util.List<Pair<Point, Double>> targetPoints = Collections.synchronizedList(TARGET_POINTS.get(StaticViewProperties.selectedObject.getID()));
                g.setColor(valueColor);
                int x;
                int y;
                for (Pair<Point, Double> pair : targetPoints) {
                    Point2D point = pair.first();
                    x = t.xToScreen(point.getX());
                    y = t.yToScreen(point.getY());
                    /*Stroke s = new BasicStroke(3);
                    g.setStroke(s);*/

                    g.drawString(String.valueOf(pair.second()), x, y);
                    Circle2D circle = new Circle2D(new math.geom2d.Point2D(x, y), 70d);
                    circle.draw(g);
//                    g.drawOval(x - 50, y - 50, 100, 100);
                }
            }
        }
        return null;
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
        return "Target Points";
    }
}

