package mrl.util.C;

import mrl.common.ConvexHull_Rubbish;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * User: Vahid Hooshangi
 */
public class Clusters {
    private List<StandardEntity> refuges = new ArrayList<StandardEntity>();
    private List<Triangle> clustersShape = new ArrayList<Triangle>();
    private Triangulation dt;
    private double initialSize;
    private Triangle initialTriangle;
    private List<ClusterPolygon> polygons;
    private MrlWorld world;

    private Polygon mainShape;
    private boolean isSubCluster = false;

    public Clusters(List<StandardEntity> refuges, double mapSize, MrlWorld world, Polygon mainShape, boolean isSubC) {
        initialSize = mapSize * 10;
        this.refuges = refuges;
        polygons = new ArrayList<ClusterPolygon>();
        this.world = world;
        initialTriangle = new Triangle(
                new Pnt(-initialSize, -initialSize),
                new Pnt(initialSize, -initialSize),
                new Pnt(0, initialSize));

        dt = new Triangulation(initialTriangle);
        this.mainShape = mainShape;
        isSubCluster = isSubC;
        if (!isSubCluster)
            mergeCluster();
        makeCluster();
        getVoronoi();
        reStructure();
    }

    private void makeCluster() {
        for (StandardEntity refuge : refuges) {
            addSite(new Pnt(((Refuge) refuge).getX(), ((Refuge) refuge).getY()));
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
                p = new ClusterPolygon(vertices, world, mainShape, (Refuge) refuges.get(i));
                polygons.add(p);
                i++;
            }
        }
    }

    private void reStructure() {
        int id = 0;

        ConvexHull_Rubbish convexHull;
        List<Point> newPoints = new ArrayList<Point>();


        for (ClusterPolygon cp : polygons) {


            for (int j = 0; j < cp.getPolygon().npoints; j++) {
                if (cp.getPolygon().xpoints[j] <= mainShape.xpoints[2] && cp.getPolygon().xpoints[j] >= mainShape.xpoints[0]
                        && cp.getPolygon().ypoints[j] <= mainShape.ypoints[3] && cp.getPolygon().ypoints[j] >= mainShape.ypoints[1]) {
                    newPoints.add(new Point(cp.getPolygon().xpoints[j], cp.getPolygon().ypoints[j]));
                }
            }

            for (Point2D point2D : cp.getIntersectPoint()) {
                newPoints.add(new Point((int) point2D.getX(), (int) point2D.getY()));
            }

            for (int i = 0; i < mainShape.npoints; i++) {
                if (cp.getPolygon().contains(mainShape.xpoints[i], mainShape.ypoints[i])) {
                    Point point = new Point(mainShape.xpoints[i], mainShape.ypoints[i]);
                    newPoints.add(point);
                }
            }


            convexHull = new ConvexHull_Rubbish();

            for (Point point : newPoints) {
                convexHull.addPoint((int) point.getX(), (int) point.getY());
            }

//            cp.setPolygon(convexHull.convex());   TODO: VAHIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIID
            newPoints.clear();

            cp.setId(id);
            id++;
        }

    }

    private void mergeCluster() {
        Collection<StandardEntity> collection = new ArrayList<StandardEntity>();
        List<StandardEntity> remover = new ArrayList<StandardEntity>();
        remover.addAll(refuges);
        for (StandardEntity bu : remover) {
            collection = world.getObjectsInRange(bu.getID(), 30000);
            collection.remove(world.getEntity(bu.getID()));
            for (StandardEntity sts : collection) {
                if (sts instanceof Building) {
                    refuges.remove(sts);
                }
            }
        }
    }

    public List<ClusterPolygon> getPolygons() {
        return polygons;
    }

    public Polygon getMainShape() {
        return mainShape;
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
