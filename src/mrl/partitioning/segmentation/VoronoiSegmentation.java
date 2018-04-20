package mrl.partitioning.segmentation;


import mrl.common.ConvexHull;
import mrl.partitioning.voronoi.Pnt;
import mrl.partitioning.voronoi.Triangle;
import mrl.partitioning.voronoi.Triangulation;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author: P.D.G and Vahid Hooshangi
 * Date: 2/12/12
 * Time: 5:19 PM
 */
public class VoronoiSegmentation implements ISegmentation {
    @Override
    public List<Polygon> doSegmentation(Polygon polygon, List<Point2D> points) {
        List<Polygon> polygons = new ArrayList<Polygon>();
        double initialSize = polygonSize(polygon) * 30;
        Triangle initialTriangle = new Triangle(
                new Pnt(-initialSize, -initialSize),
                new Pnt(initialSize, -initialSize),
                new Pnt(0, initialSize));
        Triangulation dt = new Triangulation(initialTriangle);
        makeCluster(points, dt);
        polygons.addAll(getVoronoi(initialTriangle, dt, polygon));

        return polygons;
    }


    private double polygonSize(Polygon polygon) {
        return Math.max(polygon.getBounds().getHeight(), polygon.getBounds().getWidth());
    }

    private void makeCluster(List<Point2D> points, Triangulation dt) {
//        List<Triangle> triangles = new ArrayList<Triangle>();
        for (Point2D p : points) {
            addSite(new Pnt(p.getX(), p.getY()), dt);
        }
//        triangles.addAll(dt.triGraph.nodeSet());
//        return triangles;
    }

    private void addSite(Pnt point, Triangulation dt) {
        dt.delaunayPlace(point);
    }

    private List<Polygon> getVoronoi(Triangle initialTriangle, Triangulation dt, Polygon polygon) {
        List<Polygon> polygons = new ArrayList<Polygon>();
        HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
        List<Triangle> list;
        List<Pnt> vertices;

        for (Triangle triangle : dt) {
            for (Pnt site : triangle) {
                if (done.contains(site)) {
                    continue;
                }
                done.add(site);
                list = dt.surroundingTriangles(site, triangle);
                vertices = new ArrayList<Pnt>();

                for (Triangle tri : list) {
                    vertices.add(tri.getCircumcenter());
                }
                polygons.add(makePolygon(vertices, polygon));
            }
        }

        return polygons;
    }

    private Polygon makePolygon(List<Pnt> vertices, Polygon polygon) {
        Polygon p;
        List<Point2D> intersectPoint = new ArrayList<Point2D>();
        int size = vertices.size();
        int[] xs = new int[size];
        int[] ys = new int[size];
        for (int i = 0; i < size; i++) {
            xs[i] = (int) vertices.get(i).coord(0);
            ys[i] = (int) vertices.get(i).coord(1);
        }
        p = new Polygon(xs, ys, size);
        try {
            Line2D.Double line = null;
            for (int i = 0; i < size; i++) {
                if (i + 1 < size) {
                    line = new Line2D.Double(p.xpoints[i], p.ypoints[i], p.xpoints[i + 1], p.ypoints[i + 1]);
                } else {
                    line = new Line2D.Double(p.xpoints[i], p.ypoints[i], p.xpoints[0], p.ypoints[0]);
                }

                if (getIntersections(polygon, line).size() > 0) {
                    intersectPoint.addAll(getIntersections(polygon, line));
                }

            }

        } catch (Exception e) {
            System.out.println(e);
        }

        return reMakePolygonShape(p, polygon, intersectPoint);
    }


    /**
     * make new shape polygon
     *
     * @param p
     * @param polygon
     * @param intersectPoint
     */
    private Polygon reMakePolygonShape(Polygon p, Polygon polygon, List<Point2D> intersectPoint) {
        ConvexHull convexHull;

        List<Point> newPoints = new ArrayList<Point>();

        for (int j = 0; j < p.npoints; j++) {
            if (polygon.contains(p.xpoints[j], p.ypoints[j])) {
                newPoints.add(new Point(p.xpoints[j], p.ypoints[j]));
            }
        }


        for (Point2D point2D : intersectPoint) {
            newPoints.add(new Point((int) point2D.getX(), (int) point2D.getY()));
        }

        for (int i = 0; i < polygon.npoints; i++) {
            if (p.contains(polygon.xpoints[i], polygon.ypoints[i])) {
                Point point = new Point(polygon.xpoints[i], polygon.ypoints[i]);
                newPoints.add(point);
            }
        }


        convexHull = new ConvexHull();

        for (Point point : newPoints) {
            convexHull.addPoint((int) point.getX(), (int) point.getY());
        }

        Polygon polygon1 = convexHull.convex();
//        newPoints.clear();

        return polygon1;
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

}
