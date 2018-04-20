package mrl.partition;


import mrl.common.MRLConstants;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by  P.D.G.
 * User: pooyaD
 * Date: Jan 20, 2010
 * Time: 8:46:52 PM
 */
public class DefaultRendezvous implements MRLConstants {

    private Road road;
    private MrlWorld world;

    public DefaultRendezvous(MrlWorld world, EntityID roadID, int priority, boolean hasGonto, int... partitionsId) {
        this.road = (Road) world.getEntity(roadID);
        this.world = world;
//        roadList = new ArrayList<Road>();
//        partitions = new ArrayList<Integer>();

//        this.hasGoneto = hasGonto;

//        this.priority = priority;

//        for (int i : partitionsId) {
//            partitions.add(i);
//        }

//        roadList.add(road);


    }

    public DefaultRendezvous(MrlWorld world, Point p, int priority, boolean hasGonto, int... partitionsId) {
/*
        this.x = p.x;
        this.y = p.y;
        this.world = world;
        boolean found = false;
        int circle = 15000;
        roadList = new ArrayList<Road>();
        partitions = new ArrayList<Integer>();

        this.hasGoneto = hasGonto;

        this.priority = priority;

        for (int i : partitionsId) {
            partitions.add(i);
        }


        //todo: we  should check that the road which would be returned is not in a deadlock

        while (!found) {//if the nodeList does not exists on current radius search for nodeList in a bigger circle
            road = (Road) world.getRoads().iterator().next();
            int minDistanceToXY = Integer.MAX_VALUE;
            for (Object obj : world.getRoads()) {
                Road road = (Road) obj;
                int distanceToXY = Util.distance(road.getX(), road.getY(), x, y);
                if (distanceToXY < circle) {
                    if (minDistanceToXY > distanceToXY) {
                        if (isInCyclicPath(road))
                            minDistanceToXY = distanceToXY;
                        this.road = road;
                        found = true;
                    }
                }
            }
            circle *= 2;
        }

        roadList.add(this.road);
        for (Object obj : world.getRoads()) {
            Road road = (Road) obj;
            boolean appropriate = true;
            for (Road road2 : roadList) {
                if (Util.distance(road.getX(), road.getY(), road2.getX(), road2.getY()) >= world.getConfig().getIntValue(PERCEPTION_KEY)) { // VISIBLE_DISTANCE
                    appropriate = false;
                    break;
                }
            }
            if (appropriate)
                roadList.add(road);

        }
*/
    }

    public DefaultRendezvous(List<mrl.partitioning.Partition> partitionList, MrlWorld w) {
//        roads = new ArrayList<Road>();
        world = w;
        choosePoint(partitionList);

    }

    private void choosePoint(List<mrl.partitioning.Partition> partitions) {
        int size;
        List<Point2D> p = new ArrayList<Point2D>();
        boolean isFindPath = false;

        for (mrl.partitioning.Partition partition : partitions) {

            for (mrl.partitioning.Partition partition1 : partitions) {
                if (partition.getId() == partition1.getId()) {
                    continue;
                }
                size = 0;
                p.clear();
                for (int i = 0; i < partition.getPolygon().npoints; i++) {
                    for (int j = 0; j < partition1.getPolygon().npoints; j++) {
                        if ((partition.getPolygon().xpoints[i] == partition1.getPolygon().xpoints[j]) && (partition.getPolygon().ypoints[i] == partition1.getPolygon().ypoints[j])) {
                            p.add(new Point2D.Double(partition.getPolygon().xpoints[i], partition.getPolygon().ypoints[i]));
                            size++;
                        }
                    }
                }

                if (size == 2) {
                    isFindPath = findWithPath(partitions, p.get(0), p.get(1));

                    if (!isFindPath) {
                        findsWithRoad(p.get(0), p.get(1));
                    }
                }

            }
        }
    }

    private boolean findWithPath(List<mrl.partitioning.Partition> partitionList, Point2D p1, Point2D p2) {
        List<EntityID> pathEntity = new ArrayList<EntityID>();
        List<Road> roadList = new ArrayList<Road>();
        HashSet<EntityID> entrances = new HashSet<EntityID>();
        HashSet<Road> roadHash = new HashSet<Road>();

        Point2D center;
        int dis;
        Road r = null;
        boolean findPath = false;

        center = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);

        for (mrl.partitioning.Partition partition : partitionList) {
            for (Path e : partition.getPaths()) {
                for (mrl.partitioning.Partition partition1 : partitionList) {
                    if (partition.getId() == partition1.getId()) {
                        continue;
                    }
                    if (partition1.getPaths().contains(e)) {
                        pathEntity.add(e.getId());
                        findPath = true;
                    }
                }
            }
        }

        //remove entrance from road collection
        for (MrlBuilding mb : world.getMrlBuildings()) {
            for (Entrance e : mb.getEntrances()) {
                entrances.add(e.getID());
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
        roadList.add(r);
//        roads.add(r);

        return findPath;
    }

    /**
     * @param p1
     * @param p2
     */
    private void findsWithRoad(Point2D p1, Point2D p2) {
        Point2D center;
        int dis;
        Road r = null;
        HashSet<EntityID> entrances = new HashSet<EntityID>();
        HashSet<Road> roadHash = new HashSet<Road>();

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
//        roadList.add(r);
//        roads.add(r);
    }


    public final ArrayList<DefaultRendezvous> createDefaulRendezvous(Partitions partitions) {
        ArrayList<DefaultRendezvous> UNKNOWNMAP_REND;

        Partition p0, p2;
        p0 = partitions.get(0);
        p2 = partitions.get(2);


        UNKNOWNMAP_REND = new ArrayList<DefaultRendezvous>(
                Arrays.asList(
                        new DefaultRendezvous(world, new Point(p0.getWidth(), p0.getHeight() / 2), 0, false, 0, 1),
                        new DefaultRendezvous(world, new Point(p0.getWidth() * 2, p0.getHeight() / 2), 1, false, 1, 2),
                        new DefaultRendezvous(world, new Point(p2.getPolygon().xpoints[0] + p2.getWidth() / 2, p2.getHeight()), 0, false, 2, 3),
                        new DefaultRendezvous(world, new Point(p0.getWidth() * 2, (int) (p0.getHeight() + 0.5 * p0.getHeight())), 1, false, 3, 4),
                        new DefaultRendezvous(world, new Point(p0.getWidth(), (int) (p0.getHeight() + 0.5 * p0.getHeight())), 0, false, 4, 5),
                        new DefaultRendezvous(world, new Point(p0.getWidth() / 2, p0.getHeight()), 1, false, 5, 0)
                )
        );


        return UNKNOWNMAP_REND;
    }


    private boolean isInCyclicPath(Road road) {

        return road != null;
    }


}
