package mrl.partitioning.voronoi;

import mrl.world.MrlWorld;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * User: Vahid Hooshangi
 */
public class Clusters {
    private List<Point2D> refuges = new ArrayList<Point2D>();
    private List<Triangle> clustersShape = new ArrayList<Triangle>();
    private Triangulation dt;
    private double initialSize;
    private Triangle initialTriangle;
    private List<Polygon> polygons;
    private MrlWorld world;

    private Polygon mainShapeToReStructure;
    private boolean isSubCluster = false;
    private List<ClusterPolygon> clusterPolygons;

    public Clusters(List<Point2D> refuges, double mapSize, MrlWorld world, Polygon mainShape) {
        initialSize = mapSize * 30;
        this.refuges = refuges;
        polygons = new ArrayList<Polygon>();
        this.world = world;
        initialTriangle = new Triangle(
                new Pnt(-initialSize, -initialSize),
                new Pnt(initialSize, -initialSize),
                new Pnt(0, initialSize));

        dt = new Triangulation(initialTriangle);
        this.mainShapeToReStructure = mainShape;
        clusterPolygons = new ArrayList<ClusterPolygon>();
        makeCluster();
        getVoronoi();
    }

    private void makeCluster() {
        for (Point2D p : refuges) {
            addSite(new Pnt(p.getX(), p.getY()));
        }
        clustersShape.addAll(dt.triGraph.nodeSet());
    }

    public void addSite(Pnt point) {
        dt.delaunayPlace(point);
    }

    public List<Triangle> getClustersShape() {
        return clustersShape;
    }

    public void getVoronoi() {
        HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
        List<Triangle> list;
        List<Pnt> vertices;
        int i = 0;
        for (Triangle triangle : dt) {
            for (Pnt site : triangle) {
                if (done.contains(site)) {
                    continue;
                }
                done.add(site);
                list = dt.surroundingTriangles(site, triangle);
                vertices = new ArrayList<Pnt>();

                for (Triangle tri : list) {
                    Pnt pnt = tri.getCircumcenter();
                    vertices.add(pnt);
                }
                ClusterPolygon p;
                p = new ClusterPolygon(vertices, world, mainShapeToReStructure);

                clusterPolygons.add(p);
                polygons.add(p.getPolygon());
                i++;
            }
        }
    }


    public List<Polygon> getPolygons() {
        return polygons;
    }

    public List<ClusterPolygon> getClusterPolygons() {
        return clusterPolygons;
    }

    @Override
    public String toString() {
        String str = "\n";
        for (Triangle triangle : clustersShape) {
            str += triangle.toString() + "\n";
        }
        return str;
    }
}
