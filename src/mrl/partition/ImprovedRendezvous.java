package mrl.partition;


import mrl.common.MRLConstants;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by  P.D.G and Vahid Hooshangi.
 */
public class ImprovedRendezvous implements MRLConstants {

    private MrlWorld world;
    private Rendezvous rendezvous; //TODO
    List<Rendezvous> rendezvouses;

    private Map<Pair<Integer, Integer>, Rendezvous> partitionRendezvousMap;

    public ImprovedRendezvous(List<mrl.partitioning.Partition> partitions, MrlWorld w) {
//        partitionRendezvousMap = new FastMap<Pair<Integer, Integer>, Rendezvous>();
//        rendezvouses = new ArrayList<Rendezvous>();
//        world = w;
//        Pair<Integer, Integer> p;
//        Pair<Integer, Integer> p2;
//        for (Partition partition : partitions) {
//            for (Pair<math.geom2d.line.Line2D,Partition> neighbour : partition.getNeighboursByEdge()) {
//                p = new Pair<Integer, Integer>(partition.getId(), neighbour.second().getId());
//                p2 = new Pair<Integer, Integer>(neighbour.second().getId(), partition.getId());
//                if (!(partitionRendezvousMap.keySet().contains(p) || partitionRendezvousMap.keySet().contains(p2))) {
//                    choosePoint(partition, neighbour.second());
//                }
//            }
//        }

    }


    /**
     * @param partitions
     * @param partition1
     */
    private void choosePoint(Partition partitions, Partition partition1) {
        int size;
        List<Point2D> p = new ArrayList<Point2D>();
        boolean isFindPath = false;

        size = 0;
        for (int i = 0; i < partitions.getPolygon().npoints; i++) {
            for (int j = 0; j < partition1.getPolygon().npoints; j++) {
                if ((partitions.getPolygon().xpoints[i] == partition1.getPolygon().xpoints[j]) && (partitions.getPolygon().ypoints[i] == partition1.getPolygon().ypoints[j])) {
                    p.add(new Point2D.Double(partitions.getPolygon().xpoints[i], partitions.getPolygon().ypoints[i]));
                    size++;
                    if (size == 2) {
                        break;
                    }
                }
            }
        }

        isFindPath = findWithPath(partitions, partition1, p.get(0), p.get(1));

        if (!isFindPath) {
            findsWithRoad(partitions, partition1, p.get(0), p.get(1));
        }

    }

    /**
     * find a path that there is in neighbours partitions
     *
     * @param partition
     * @param p1
     * @param p2
     * @return if find a path that there is in neighbours partition return true, then return false
     */
    private boolean findWithPath(Partition partition, Partition neighbourPartition, Point2D p1, Point2D p2) {
        List<EntityID> pathEntity = new ArrayList<EntityID>();
        List<Road> roadList = new ArrayList<Road>();
        HashSet<EntityID> entrances = new HashSet<EntityID>();
        HashSet<Road> roadHash = new HashSet<Road>();
        List<Road> roadRendez = new ArrayList<Road>();

        Point2D center;
        int dis;
        Road r = null;
        boolean findPath = false;

        center = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);

        for (Path e : partition.getPaths()) {
            for (Path p : neighbourPartition.getPaths()) {
                if (p.contains(e.getEndOfPath()) || p.contains(e.getHeadOfPath()) || p.contains(e.getMiddleRoad())) {
                    pathEntity.add(p.getId());
                    findPath = true;
                }
            }
        }

        //remove entrance from road collection
        for (MrlBuilding mb : world.getMrlBuildings()) {
            for (Entrance e : mb.getEntrances()) {
                entrances.add(e.getID());
            }
        }

        for (EntityID e : pathEntity)
            roadList.addAll(world.getPath(e));

        for (Road ro : roadList) {
            if (!entrances.contains(ro.getID())) {
                roadHash.add(ro);
            }
        }


        dis = Integer.MAX_VALUE;

        for (Road rs : roadHash) {
            if (center.distance(rs.getX(), rs.getY()) < dis) {
                dis = (int) center.distance(rs.getX(), rs.getY());
                r = rs;
            }
        }

        roadRendez.add(r);
        setRendezvous(partition.getId().getValue(), neighbourPartition.getId().getValue(), roadRendez);

        return findPath;
    }

    private void setRendezvous(int id, int id1, List<Road> roadList) {

        List<Integer> partitionIDList = new ArrayList<Integer>();
        partitionIDList.add(id);
        partitionIDList.add(id1);
        rendezvous = new Rendezvous(roadList, partitionIDList);

        partitionRendezvousMap.put(new Pair<Integer, Integer>(id, id1), rendezvous);


    }

    /**
     * find the nearest road to the center of polygon shape lines.
     *
     * @param p1
     * @param p2
     */
    private void findsWithRoad(Partition partition, Partition partition1, Point2D p1, Point2D p2) {
        Point2D center;
        int dis;
        Road r = null;
        HashSet<EntityID> entrances = new HashSet<EntityID>();
        HashSet<Road> roadHash = new HashSet<Road>();
        List<Road> roadList = new ArrayList<Road>();

        //remove entrance from road collection
        for (MrlBuilding mb : world.getMrlBuildings()) {
            for (Entrance e : mb.getEntrances()) {
                entrances.add(e.getID());
            }
        }

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
        roadList.clear();
        roadList.addAll(roadHash);
        setRendezvous(partition.getId().getValue(), partition1.getId().getValue(), roadList);
    }

    private boolean isInCyclicPath(Road road) {

        return road != null;
    }


    public Map<Pair<Integer, Integer>, Rendezvous> getPartitionRendezvousMap() {
        return partitionRendezvousMap;
    }


}
