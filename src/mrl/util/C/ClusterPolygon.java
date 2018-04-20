package mrl.util.C;

import mrl.common.ConvexHull_Rubbish;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Vahid Hooshangi
 */
public class ClusterPolygon {
    private List<Pnt> vertices;
    private Polygon polygon;
    private int id;
    private ArrayList<Building> buildings;
    private ArrayList<Road> roads;
    private MrlWorld world;
    private double totalArea;
    private Polygon mainClusterShape;
    private List<Point2D> intersectPoint;
    private Building building;

    public ClusterPolygon(List<Pnt> vertices, MrlWorld world, Polygon mainPolygon, Building building) {
        this.vertices = vertices;
        this.buildings = new ArrayList<Building>();
        this.roads = new ArrayList<Road>();
        this.world = world;
        this.totalArea = 0.0;
        this.intersectPoint = new ArrayList<Point2D>();
        this.mainClusterShape = mainPolygon;
        this.building = building;
        makePolygon();
        clusterBuildings();
        clusterRoad();
        calculateArea();
    }

    /**
     * make cluster Polygon
     */
    private void makePolygon() {
        Polygon p;
        polygon = new Polygon();
        int size = vertices.size();
        int[] xs = new int[size];
        int[] ys = new int[size];
        for (int i = 0; i < size; i++) {
            xs[i] = (int) vertices.get(i).coord(0);
            ys[i] = (int) vertices.get(i).coord(1);
        }
        p = new Polygon(xs, ys, size);
        polygon = new Polygon(xs, ys, vertices.size());

        try {
            Line2D.Double line = null;
            for (int i = 0; i < size; i++) {
                if (i + 1 < size) {
                    line = new Line2D.Double(p.xpoints[i], p.ypoints[i], p.xpoints[i + 1], p.ypoints[i + 1]);
                } else {
                    line = new Line2D.Double(p.xpoints[i], p.ypoints[i], p.xpoints[0], p.ypoints[0]);
                }


                if (getIntersections(mainClusterShape, line).size() > 0) {
                    intersectPoint.addAll(getIntersections(mainClusterShape, line));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
//            System.out.println(e);
        }

        reMakePolygonShape();

    }

    /**
     * make new shape polygon
     */
    private void reMakePolygonShape() {
        ConvexHull_Rubbish convexHull;

        List<Point> newPoints = new ArrayList<Point>();
        List<Point> mShapePoints = new ArrayList<Point>();

        for (int i = 0; i < mainClusterShape.npoints; i++) {
            mShapePoints.add(new Point(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i]));
        }

        int size = mainClusterShape.npoints;
        Line2D line = null;

        for (int i = 0; i < mainClusterShape.npoints; i++) {
            mShapePoints.add(new Point(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i]));
            if (i + 1 < size) {
                line = new Line2D.Double(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i], mainClusterShape.xpoints[i + 1], mainClusterShape.ypoints[i + 1]);
            } else {
                line = new Line2D.Double(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i], mainClusterShape.xpoints[0], mainClusterShape.ypoints[0]);
            }
        }

        for (int j = 0; j < polygon.npoints; j++) {
            if (polygon.xpoints[j] <= mainClusterShape.xpoints[2] && polygon.xpoints[j] >= mainClusterShape.xpoints[0]
                    && polygon.ypoints[j] <= mainClusterShape.ypoints[3] && polygon.ypoints[j] >= mainClusterShape.ypoints[1]) {
                newPoints.add(new Point(polygon.xpoints[j], polygon.ypoints[j]));
            }
        }

        for (Point2D point2D : intersectPoint) {
            newPoints.add(new Point((int) point2D.getX(), (int) point2D.getY()));
        }

        for (int i = 0; i < mainClusterShape.npoints; i++) {
            if (polygon.contains(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i])) {
                Point p = new Point(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i]);
                newPoints.add(p);
                mShapePoints.remove(p);
            }
        }


        convexHull = new ConvexHull_Rubbish();

        for (Point point : newPoints) {
            convexHull.addPoint(point.x, point.y);
        }

//        setPolygon(convexHull.convex());  TODO: VAHIIIIIIIIIIIIIIIIIIIIIIIID
        newPoints.clear();
    }

    /**
     * * find the cluster buildings
     */
    private void clusterBuildings() {
        for (StandardEntity entity : world.getBuildings()) {
            if (entity instanceof Building) {
                if (polygon.contains(((Building) entity).getX(), ((Building) entity).getY())) {
                    this.buildings.add((Building) entity);
                }
            }
        }
    }


    /**
     * find the cluster roads
     */
    private void clusterRoad() {
        for (StandardEntity entity : world.getRoads()) {
            if (entity instanceof Road) {
                if (polygon.contains(((Road) entity).getX(), ((Road) entity).getY())) {
                    this.roads.add((Road) entity);
                }
            }
        }
    }


    /**
     * this function calculate Cluster Total area
     */
    private void calculateArea() {
        for (Building building : buildings) {
            totalArea += building.getTotalArea();
        }
    }

    /**
     * @param poly main cluster Polygon
     * @param line sub cluster polygon lines
     * @return Set of intersect Point
     * @throws Exception e
     */
    private Set<Point2D> getIntersections(final Polygon poly, final Line2D.Double line) throws Exception {

        final PathIterator polyIt = poly.getPathIterator(null); //Getting an iterator along the polygon path
        final double[] coords = new double[6]; //Double array with length 6 needed by iterator
        final double[] firstCoords = new double[2]; //First point (needed for closing polygon path)
        final double[] lastCoords = new double[2]; //Previously visited point
        final Set<Point2D> intersections = new HashSet<Point2D>(); //List to hold found intersections
        polyIt.currentSegment(firstCoords); //Getting the first coordinate pair
        lastCoords[0] = firstCoords[0]; //Priming the previous coordinate pair
        lastCoords[1] = firstCoords[1];
        polyIt.next();
        while (!polyIt.isDone()) {
            final int type = polyIt.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_LINETO: {
                    final Line2D.Double currentLine = new Line2D.Double(lastCoords[0], lastCoords[1], coords[0], coords[1]);
                    if (currentLine.intersectsLine(line)) {
                        intersections.add(getIntersection(currentLine, line));
                    }
                    lastCoords[0] = coords[0];
                    lastCoords[1] = coords[1];
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    final Line2D.Double currentLine = new Line2D.Double(coords[0], coords[1], firstCoords[0], firstCoords[1]);
                    if (currentLine.intersectsLine(line)) {
                        intersections.add(getIntersection(currentLine, line));
                    }
                    break;
                }
                default: {
                    throw new Exception("Unsupported PathIterator segment type.");
                }
            }
            polyIt.next();
        }
        return intersections;

    }

    /**
     * this function calculate the intersect point of polygon and line
     *
     * @param line1 main cluster polygon line
     * @param line2 sub cluster polygon line
     * @return the intersection of two lines point
     */
    private Point2D getIntersection(final Line2D.Double line1, final Line2D.Double line2) {

        final double x1, y1, x2, y2, x3, y3, x4, y4;
        x1 = line1.x1;
        y1 = line1.y1;
        x2 = line1.x2;
        y2 = line1.y2;
        x3 = line2.x1;
        y3 = line2.y1;
        x4 = line2.x2;
        y4 = line2.y2;
        final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
        final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

        return new Point2D.Double(x, y);

    }


    /**
     * set cluster ID
     *
     * @param id cluster id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * get cluster polygon
     *
     * @return get the cluster polygon
     */
    public Polygon getPolygon() {
        return polygon;
    }

    /**
     * get cluster building
     *
     * @return cluster buildings
     */
    public ArrayList<Building> getBuildings() {
        return buildings;
    }

    /**
     * get cluster total area
     *
     * @return get the cluster totalArea
     */
    public double getTotalArea() {
        return totalArea;
    }

    /**
     * get cluster vertices
     *
     * @return get the cluster vertices
     */
    public List<Pnt> getVertices() {
        return vertices;
    }

    /**
     * get cluster id
     *
     * @return get the cluster id
     */
    public int getId() {
        return id;
    }


    public Building getBuilding() {
        return building;
    }

    /**
     * get map shape
     *
     * @return get the map shape
     */
    public Polygon getMainClusterShape() {
        return mainClusterShape;
    }

    /**
     * @return get the intersect point
     */
    public List<Point2D> getIntersectPoint() {
        return intersectPoint;
    }

    /**
     * @param polygon set cluster polygon
     */
    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }
}
