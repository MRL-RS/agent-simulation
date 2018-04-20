package mrl.partitioning.voronoi;

import javolution.util.FastMap;
import javolution.util.FastSet;
import math.geom2d.line.Line2D;
import mrl.common.Util;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * @author Vahid Hooshangi
 */
public class Rendezvouz {
    private List<Road> roads;
    private MrlWorld world;

    private Map<Integer, List<EntityID>> rendezvous;
    private Set<EntityID> entrances = new FastSet<EntityID>();


    public Rendezvouz(MrlWorld w) {
        roads = new ArrayList<Road>();
        world = w;
        rendezvous = new FastMap<Integer, List<EntityID>>();
        findEntrances();

    }


    private void findEntrances() {
        //remove entrance from road collection
        for (MrlBuilding mb : world.getMrlBuildings()) {
            for (Entrance e : mb.getEntrances()) {
                entrances.add(e.getID());
            }
        }
    }


    //    private void choicePoint(List<Partition> partitions) {
//        int size;
//        List<Point2D> p = new ArrayList<Point2D>();
//
//        for (Partition partition : partitions) {
//            for (Partition partition1 : partitions) {
//                if (partition1.getId() == partition.getId()) {
//                    continue;
//                }
//                size = 0;
//                p.clear();
//                for (int i = 0; i < partition.getPolygon().npoints; i++) {
//                    for (int j = 0; j < partition1.getPolygon().npoints; j++) {
//                        if ((partition.getPolygon().xpoints[i] == partition1.getPolygon().xpoints[j]) && (partition.getPolygon().ypoints[i] == partition1.getPolygon().ypoints[j])) {
//                            p.add(new Point2D.Double(partition.getPolygon().xpoints[i], partition.getPolygon().ypoints[i]));
//                            size++;
//                        }
//                    }
//                }
//                if (size == 2) {
//                    List<EntityID> rendezvouses;
//                    Road road;
//                    road = findWithPath(partitions, p.get(0), p.get(1));
//                    if (road != null) {
//
//                        rendezvouses=rendezvous.get(partition.getId());
//                        if(rendezvouses==null){
//                            rendezvouses=new ArrayList<EntityID>();
//                        }
//                        rendezvouses.add(road.getID());
//                        rendezvous.put(partition.getId(), rendezvouses);
//
//
//                        rendezvouses=rendezvous.get(partition1.getId());
//                        if(rendezvouses==null){
//                            rendezvouses=new ArrayList<EntityID>();
//                        }
//                        rendezvouses.add(road.getID());
//                        rendezvous.put(partition1.getId(), rendezvouses);
//
//                    }
//
//                    if (road == null) {
//                        road = findsWithRoad(p.get(0), p.get(1));
//                        if (road != null) {
//                            rendezvouses=rendezvous.get(partition.getId());
//                            if(rendezvouses==null){
//                                rendezvouses=new ArrayList<EntityID>();
//                            }
//                            rendezvouses.add(road.getID());
//                            rendezvous.put(partition.getId(), rendezvouses);
//
//
//                            rendezvouses=rendezvous.get(partition1.getId());
//                            if(rendezvouses==null){
//                                rendezvouses=new ArrayList<EntityID>();
//                            }
//                            rendezvouses.add(road.getID());
//                            rendezvous.put(partition1.getId(), rendezvouses);
//                        }
//
//                    }
//                }
//            }
//        }
//    }


    private void choicePoint(List<Partition> partitions) {
        Road rendez;
        List<EntityID> rendezList = new ArrayList<EntityID>();

        for (Partition partition : partitions) {
            if (partition.getId().getValue() == 168837295) {
                System.out.println("rendez :" + partition.getNeighboursByEdge());
            }

            rendezList.clear();
            for (Pair<Line2D, Partition> pair : partition.getNeighboursByEdge()) {
                rendez = findWithPath(pair.first(), pair.second(), partition);

                if (rendez != null) {
                    rendezList.add(rendez.getID());
                } else {
                    rendez = findsWithRoad(pair.first().getPoint1(), pair.first().getPoint2());
                    if (rendez != null) {
                        rendezList.add(rendez.getID());
                    }
                }
            }

//            System.out.println("ID : " + partition.getId().getValue() + ": " + rendezList);

            partition.getRendezvous().addAll(rendezList);
        }
    }

    private Road findWithPath(Line2D first, Partition neighbourPartition, Partition partition) {

        Point lineCentre = new Point((int) (first.getPoint1().getX() + first.getPoint2().getX()) / 2, (int) (first.getPoint1().getY() + first.getPoint2().getY()) / 2);
        List<Road> middleRoads = new ArrayList<Road>();

        for (Path path : neighbourPartition.getPaths()) {
            if (partition.getPaths().contains(path)) {
//                middleRoads.add(path.getMiddleRoad());
                middleRoads.addAll(path);
            }
        }

        double minDistance = Double.MAX_VALUE;
        double dis;
        Road targetRoad = null;

        if (!middleRoads.isEmpty()) {
            for (Road road : middleRoads) {
                dis = Util.distance(road.getX(), road.getY(), (int) lineCentre.getX(), (int) lineCentre.getY());
                if (dis < minDistance) {
                    if (entrances.contains(road.getID())) {
                        for (EntityID entityID : road.getNeighbours()) {
                            if (world.getEntity(entityID) instanceof Road) {
                                targetRoad = (Road) world.getEntity(entityID);
                                minDistance = dis;
                            }
                        }
                    } else {
                        minDistance = dis;
                        targetRoad = road;
                    }
                }
            }
        }

        return targetRoad;
    }


    private Road findWithPath(List<Partition> partitions, Point2D p1, Point2D p2) {
        List<EntityID> pathEntity = new ArrayList<EntityID>();
        List<Road> roadList = new ArrayList<Road>();
        HashSet<Road> roadHash = new HashSet<Road>();

        Point2D center;
        int dis;
        Road r = null;

        center = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);

        for (Partition partition : partitions) {
            for (Path e : partition.getPaths()) {
                for (Partition partition1 : partitions) {
                    if (partition.getId() == partition1.getId()) {
                        continue;
                    }
                    if (partition1.getPaths().contains(e)) {
                        pathEntity.add(e.getId());
                    }
                }
            }
        }


        for (EntityID e : pathEntity) {

            for (int i = 0; i < world.getPath(e).size(); i++) {
                roadList.add(world.getPath(e).get(i));
            }

            for (Road ro : roadList) {
                if (!entrances.contains(ro.getID())) {
                    roadHash.add(ro);
                }
            }
        }

        dis = Integer.MAX_VALUE;

        for (Road rs : roadHash) {
            if (center.distance(rs.getX(), rs.getY()) < dis) {
                dis = (int) center.distance(rs.getX(), rs.getY());
                r = rs;
            }
        }
        return r;
//        roads.add(r);

//        return findPath;
    }

    private Road findsWithRoad(Point2D p1, Point2D p2) {
        Point2D center;
        int dis;
        Road r = null;
        HashSet<Road> roadHash = new HashSet<Road>();

        for (StandardEntity se : world.getRoads()) {
            if (se instanceof Road) {
                if (!entrances.contains(se.getID()))
                    roadHash.add((Road) se);
            }
        }

        dis = Integer.MAX_VALUE;
        center = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
        for (Road rs : roadHash) {
            if (center.distance(rs.getX(), rs.getY()) < dis) {
                dis = (int) center.distance(rs.getX(), rs.getY());
                r = rs;
            }
        }
//        road = r;
//        roads.add(r);
        return r;
    }

    public List<Road> getRoads() {
        return roads;
    }

    public Map<Integer, List<EntityID>> getRendezvous() {
        return rendezvous;
    }

    public void createRendezvous(List<Partition> partitions) {
        choicePoint(partitions);
    }
}