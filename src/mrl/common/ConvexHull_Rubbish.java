package mrl.common;

import com.poths.rna.data.Point;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.Wall;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: sajjad salehi
 * Date: 12/15/11
 * Time: 5:41 PM
 */

public class ConvexHull_Rubbish {
    public java.awt.Point FIRST_POINT;
    public java.awt.Point SECOND_POINT;
    public java.awt.Point CONVEX_POINT;
    public java.awt.Point OTHER_POINT1;
    public java.awt.Point OTHER_POINT2;
    public Set<Point2D> CONVEX_INTERSECT_POINTS;
    public Set<Line2D> CONVEX_INTERSECT_LINES;
    public Polygon DIRECTION_POLYGON;

    com.poths.rna.data.ConvexHull convexHull = new com.poths.rna.data.ConvexHull();
    List<MrlBuilding> edgeBuildings = new ArrayList<MrlBuilding>();

    public void addPoint(double x, double y) {
        convexHull.addPoint(new com.poths.rna.data.Point(x, y));
    }

    private void addPoint(Point pt) {
        convexHull.addPoint(pt);
    }

    private void addPoints(List<Point> points) {
        for (Point pnt : points)
            addPoint((int) pnt.getX(), (int) pnt.getY());
    }

    private void addPoints(int[] points) {
        for (int i = 0; i < points.length - 1; i += 2) {
            addPoint(points[i], points[i + 1]);
        }
    }

    public void addBuilding(MrlBuilding building) {
//        if (convexHull.getPointList().size() <= 3) {
//            List<Wall> walls = (List<Wall>) building.getWalls();
//            for (Wall e : walls) {
//                addPoint(e.x1, e.y1);
//                addPoint(e.x2, e.y2);
//            }
//        } else if (building.getID() != null) {
//            addPoint(new Point(building.getSelfBuilding().getX(), building.getSelfBuilding().getY()));
//        }
        int[] apexes = building.getSelfBuilding().getApexList();
        for (int i = 0; i < apexes.length; i += 2) {
            addPoint(apexes[i], apexes[i + 1]);
        }

    }

    public void addMrlBuildings(List<MrlBuilding> buildings) {
        for (MrlBuilding building : buildings)
            addBuilding(building);
    }

    public void updateEdgeBuildings(List<MrlBuilding> burningList) {

        List<java.awt.geom.Line2D> edges = getEdges();
        if (burningList.size() <= 3) {
            for (MrlBuilding bld : burningList)
                if (!edgeBuildings.contains(bld))
                    edgeBuildings.add(bld);
            return;
        } else {
            edgeBuildings.clear();
            for (MrlBuilding b : burningList) {
                for (java.awt.geom.Line2D edge : edges) {
                    if (b.intersectToLine2D(edge)) {
                        if (!edgeBuildings.contains(b) && b.isBurning()) {
                            edgeBuildings.add(b);
                            break;
                        }
                    }
                }
            }
        }
    }

    public List<java.awt.geom.Line2D> getEdges() {
        List<java.awt.geom.Line2D> edges = new ArrayList<java.awt.geom.Line2D>();
        for (int i = 0; i < convexHull.getPointList().size() - 1; i++) {
            Point s = convexHull.getPointList().get(i);
            java.awt.geom.Point2D start = new java.awt.geom.Point2D.Double(s.getX(), s.getY());
            s = convexHull.getPointList().get(i + 1);
            java.awt.geom.Point2D end = new java.awt.geom.Point2D.Double(s.getX(), s.getY());
            edges.add(new Line2D.Double(start, end));
        }

        return edges;
    }

    public boolean removeBuilding(MrlBuilding b, MrlFireBrigadeWorld world) {
        Point point = new Point(b.getSelfBuilding().getX(), b.getSelfBuilding().getY());
        List<Point> convexPoints = convexHull.getPointList();
        List<Point> addee = new ArrayList<Point>();
        if (edgeBuildings.contains(b)) {
            edgeBuildings.remove(b);
        }
        if (convexPoints.contains(point)) {
            convexPoints.remove(point);
            convexHull = new com.poths.rna.data.ConvexHull();
            for (EntityID buildingId : b.getNeighbourFireBuildings()) {
                MrlBuilding building = world.getMrlBuilding(buildingId);
                if (building.isBurning())
                    addee.add(new Point(building.getSelfBuilding().getX(), building.getSelfBuilding().getY()));
            }
            convexPoints.addAll(addee);
            addPoints(convexPoints);
            return true;
        }
        List<Point> points = new ArrayList<Point>();
        for (Wall wall : b.getWalls()) {
            points.add(new Point(wall.b.getX(), wall.b.getY()));
        }
        List<Point> removee = new ArrayList<Point>();
        boolean returner = false;
        for (Point pt : points) {
            if (convexPoints.contains(pt)) {
                removee.add(pt);
                returner = true;
            }
        }
        if (removee.size() == 0) {
            return false;
        }
        for (EntityID buildingId : b.getNeighbourFireBuildings()) {
            MrlBuilding building = world.getMrlBuilding(buildingId);
            if (building.isBurning())
                addee.add(new Point(building.getSelfBuilding().getX(), building.getSelfBuilding().getY()));
        }
        convexPoints.removeAll(removee);
        convexPoints.addAll(addee);
        convexHull = new com.poths.rna.data.ConvexHull();
        addPoints(convexPoints);
        return returner;
    }

    public double perimeter() {
        double perim = 0;
        Polygon p = getConvexPolygon();
        for (int i = 0; i < getConvexPolygon().xpoints.length - 1; i++) {
            perim += java.awt.Point.distance(p.xpoints[i], p.ypoints[i], p.xpoints[i + 1], p.ypoints[i + 1]);
        }
        return perim;
    }

    public static Rectangle castBuildingToPolygon(MrlBuilding building) {
        List<java.awt.Point> points = new ArrayList<java.awt.Point>();
        for (Wall wall : building.getAllWalls()) {
            points.add(wall.a);
            points.add(wall.b);
        }
        if (points.size() < 3) {
            return null;
        } else {
            int xPoints2[] = new int[points.size()];
            int yPoints2[] = new int[points.size()];

            for (int i = 0; i < points.size(); i++) {
                xPoints2[i] = (int) points.get(i).getX();
                yPoints2[i] = (int) points.get(i).getY();
            }
            return new Polygon(xPoints2, yPoints2, points.size()).getBounds();
        }
    }

    public boolean intersectConvexHull(ConvexHull_Rubbish hull) {
        if (this.getConvexPolygon().intersects(hull.getConvexPolygon().getBounds2D()))
            return true;
        return false;

    }

    public void printHull() {
        System.out.println("###########convex#########");
        System.out.println("nodes: ");
        for (Point pnt : convexHull.getPointList())
            System.out.println(pnt.getX() + "," + pnt.getY());
        System.out.println("#########################");
        System.out.println("_______edge buildings_______");
        for (MrlBuilding building : edgeBuildings) {
            System.out.println(building.getID());
        }
        System.out.println("____________________________");
    }

    public Polygon getConvexPolygon() {
        List<Point> pointList = convexHull.getPointList();
        int xPoints2[] = new int[pointList.size()];
        int yPoints2[] = new int[pointList.size()];

        for (int i = 0; i < pointList.size(); i++) {
            xPoints2[i] = (int) pointList.get(i).getX();
            yPoints2[i] = (int) pointList.get(i).getY();
        }

        return new Polygon(xPoints2, yPoints2, pointList.size());
    }

    public Area getArea() {
        if (convexHull.getPointList().isEmpty())
            return null;
        else {
            return new Area(getConvexPolygon());
        }
    }

    public List<java.awt.Point> getPoints() {
        List<java.awt.Point> points = new ArrayList<java.awt.Point>();
        for (Point pt : convexHull.getPointList()) {
            points.add(new java.awt.Point((int) pt.getX(), (int) pt.getY()));
        }
        return points;
    }

    public List<MrlBuilding> getEdgeBuildings() {
        return edgeBuildings;
    }

    @Override
    public String toString() {
        String str = "MRL ConvexHull_Rubbish ";
        for (Point point : convexHull.getPointList()) {
            str += " " + point.toString();
        }
        return str;
    }

}
