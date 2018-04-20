package mrl.partitioning.voronoi;

import mrl.common.ConvexHull;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

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

    private HashSet<EntityID> paths;

    public ClusterPolygon(List<Pnt> vertices, MrlWorld world, Polygon mainPolygon) {
        this.vertices = vertices;
        this.buildings = new ArrayList<Building>();
        this.roads = new ArrayList<Road>();
        this.world = world;
        this.totalArea = 0.0;
        this.intersectPoint = new ArrayList<Point2D>();
        this.mainClusterShape = mainPolygon;
        this.paths = new HashSet<EntityID>();
        makePolygon();
        clusterBuildings();
        clusterRoad();
        calculateArea();
//        clusterPaths();
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
            System.out.println(e);
        }
        reMakePolygonShape();
    }

    /**
     * make new shape polygon
     */
    private void reMakePolygonShape() {
        ConvexHull convexHull;

        List<Point> newPoints = new ArrayList<Point>();

        for (int j = 0; j < polygon.npoints; j++) {

            if (mainClusterShape.contains(polygon.xpoints[j], polygon.ypoints[j])) {
                newPoints.add(new Point(polygon.xpoints[j], polygon.ypoints[j]));

            }

        }


        for (Point2D point2D : getIntersectPoint()) {
            newPoints.add(new Point((int) point2D.getX(), (int) point2D.getY()));
        }

        for (int i = 0; i < mainClusterShape.npoints; i++) {
            if (polygon.contains(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i])) {
                Point point = new Point(mainClusterShape.xpoints[i], mainClusterShape.ypoints[i]);
                newPoints.add(point);
            }
        }


        convexHull = new ConvexHull();

        for (Point point : newPoints) {
            convexHull.addPoint((int) point.getX(), (int) point.getY());
        }

        setPolygon(convexHull.convex());
        newPoints.clear();
    }

    public Polygon scalePolygon(Polygon polygon) {
        int xs[] = new int[polygon.npoints];
        int ys[] = new int[polygon.npoints];
        math.geom2d.Point2D p;
        math.geom2d.Point2D p1;
        int sumX = 0;
        int sumY = 0;

        for (int i = 0; i < polygon.npoints; i++) {
            p = new math.geom2d.Point2D(polygon.xpoints[i], polygon.ypoints[i]);
            p1 = p.scale(0.5);
            sumX += p1.getX();
            sumY += p1.getY();

            xs[i] = (int) p1.getX();
            ys[i] = (int) p1.getY();
            p.clone();
        }

        Polygon poly = new Polygon(xs, ys, polygon.npoints);
        poly.translate(sumX / polygon.npoints, sumY / polygon.npoints);

        Polygon scalePolygon = new Polygon();

        for (int i = 0; i < poly.npoints; i++) {
            p = new math.geom2d.Point2D(poly.xpoints[i], poly.ypoints[i]);
            if (i + 1 < poly.npoints) {
                if (p.distance(poly.xpoints[i + 1], poly.ypoints[i + 1]) > (0.1 * world.getMapWidth())) {
                    scalePolygon.addPoint(poly.xpoints[i], poly.ypoints[i]);
                } else {
                    continue;
                }
            } else if (i + 1 == poly.npoints) {
                if (p.distance(poly.xpoints[0], poly.ypoints[0]) > (0.1 * world.getMapWidth())) {
                    scalePolygon.addPoint(poly.xpoints[i], poly.ypoints[i]);
                } else {
                    continue;
                }

            }
        }


        return scalePolygon;
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
     * find the cluster paths
     */
    private void clusterPaths() {
        List<Road> roadList = new ArrayList<Road>();
        for (StandardEntity entity : world.getRoads()) {
            if (entity instanceof Road) {
                if (polygon.contains(((Road) entity).getX(), ((Road) entity).getY())) {
                    roadList.add((Road) entity);
                }
            }
        }

        for (Road r : roadList) {
            paths.add(world.getPath(r.getID()).getId());
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

    /**
     * @return return the road path
     */
    public HashSet<EntityID> getPaths() {
        return paths;
    }
}
