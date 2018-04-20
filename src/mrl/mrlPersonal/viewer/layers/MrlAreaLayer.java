package mrl.mrlPersonal.viewer.layers;

import math.geom2d.conic.Circle2D;
import mrl.common.Util;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.view.StandardEntityViewLayer;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Set;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 13, 2010
 * Time: 3:37:10 PM
 */
public abstract class MrlAreaLayer<E extends Area> extends StandardEntityViewLayer<E> {

    public boolean showBIds;
    //    private Action showBIdsAction;
    public boolean showRIds;
    //    private Action showRIdsAction;
    public boolean showRealFieriness;

    public boolean showEdgeOpenParts = false;

    public boolean showRoadScale;
    protected boolean showCenterVisitShapes;

    public static Set<Polygon> ROADSCALE = null;
    public static List<Line2D> LINES = null;
    //    MrlRoad mrlRoad;
//    Pair<MrlRoad, MrlRoad> mrlPair;
    ScreenTransform transform;


    /**
     * Construct an area view layer.
     *
     * @param clazz The subclass of Area this can render.
     */
    protected MrlAreaLayer(Class<E> clazz) {
        super(clazz);
    }

    @Override
    public Shape render(E area, Graphics2D g, ScreenTransform t) {
        transform = t;
        if (area instanceof Road) {
            Road r = (Road) area;
            Point2D p1 = Util.getMiddle(r.getEdges().get(0).getStart(), r.getEdges().get(0).getEnd());
            Point2D p2 = Util.getMiddle(r.getEdges().get(r.getEdges().size() / 2).getStart(), r.getEdges().get(r.getEdges().size() / 2).getEnd());

//            mrlRoad = new MrlRoad(r,world);
//            mrlPair = mrlRoad.splitRoad(new rescuecore2.misc.geometry.Line2D(p1, p2));
//            mrlPair.first().createTransformedPolygon(t);
//            mrlPair.second().createTransformedPolygon(t);
        }
        List<Edge> edges = area.getEdges();
        if (edges.isEmpty()) {
            return null;
        }
        int count = edges.size();
        int[] xs = new int[count];
        int[] ys = new int[count];
        int i = 0;
        for (Edge e : edges) {
            xs[i] = t.xToScreen(e.getStartX());
            ys[i] = t.yToScreen(e.getStartY());
            ++i;
        }
        Polygon shape = new Polygon(xs, ys, count);
        paintShape(area, shape, g);
        if (area.equals(StaticViewProperties.selectedObject)) {
            Circle2D circle2D = new Circle2D(t.xToScreen(area.getX()), t.yToScreen(area.getY()), 18d);
            circle2D.draw(g);
        }

        for (Edge edge : edges) {
            paintEdge(edge, g, t);
        }
        if (((area instanceof Building) && showBIds) || ((area instanceof Road) && showRIds)) {
            drawInfo(g, t, String.valueOf(area.getID().getValue()), getLocation(area), area.getClass());
        }
        if (showRealFieriness && (area instanceof Building) && MrlBurningBuildingLayer.BURNING_BUILDINGS.contains(area.getID())) {
            drawBInfo(g, t, String.valueOf(((Building) area).getFieryness()), getLocation(area), area.getClass());
        }

        if (showRoadScale) {
            drawRoadScale(g, t);
        }

        return shape;
    }

    private void drawRoadScale(Graphics2D g, ScreenTransform t) {
        if (ROADSCALE == null) {
            return;
        }

        g.setColor(Color.GREEN);
        try {
            int xs[];
            int ys[];
            for (Polygon polygon : ROADSCALE) {
                xs = new int[polygon.npoints];
                ys = new int[polygon.npoints];
                for (int i = 0; i < polygon.npoints; i++) {
                    xs[i] = t.xToScreen(polygon.xpoints[i]);
                    ys[i] = t.yToScreen(polygon.ypoints[i]);
                }

                g.drawPolygon(xs, ys, polygon.npoints);
            }
        } catch (Exception e) {

        }

        if (LINES == null) {
            return;
        }
        g.setColor(Color.ORANGE);

        for (Line2D line2D : LINES) {
            g.drawLine(t.xToScreen(line2D.getX1()), t.yToScreen(line2D.getY1()), t.xToScreen(line2D.getX2()), t.yToScreen(line2D.getY2()));
        }

    }


    /**
     * Paint an individual edge.
     *
     * @param e The edge to paint.
     * @param g The graphics to paint on.
     * @param t The screen transform.
     */
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
    }

    /**
     * Paint the overall shape.
     *
     * @param area The area.
     * @param p    The overall polygon.
     * @param g    The graphics to paint on.
     */
    protected void paintShape(E area, Polygon p, Graphics2D g) {
    }

    private void drawInfo(Graphics2D g, ScreenTransform t, String strInfo, Pair<Integer, Integer> location, Class clazz) {
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.first());
            y = t.yToScreen(location.second());
            if (clazz.equals(Road.class)) {
                g.setColor(Color.CYAN.darker().darker());
            } else {
                g.setColor(Color.MAGENTA.brighter().brighter());
            }
            g.drawString(strInfo, x - 15, y + 4);
        }
    }

    private void drawBInfo(Graphics2D g, ScreenTransform t, String strInfo, Pair<Integer, Integer> location, Class clazz) {
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.first());
            y = t.yToScreen(location.second());
//            if (strInfo.equals("1") ||strInfo.equals("2") || strInfo.equals("3")) {
//                g.setColor(Color.BLACK);
//            } else {
//                g.setColor(Color.RED);
//            }
            g.setColor(Color.GREEN);
            g.drawString(strInfo, x - 15, y + 4);
        }
    }

    protected Pair<Integer, Integer> getLocation(Area area) {
        return area.getLocation(world);
    }
}