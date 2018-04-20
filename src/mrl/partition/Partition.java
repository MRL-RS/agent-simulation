package mrl.partition;

import javolution.util.FastSet;
import mrl.common.Condition;
import mrl.common.ConstantConditions;
import mrl.common.Util;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by P.D.G.
 * User: pooyad
 * Date: Dec 7, 2009
 * Time: 4:17:15 PM
 */
public class Partition implements ConstantConditions {
    MrlWorld world;

    private int id;
    public int timeStayingIn;
    protected int x;
    protected int y;
    protected Area centralArea;
    protected Pair<Integer, Integer> centerPosition;
    private int width;
    private int height;
    private Polygon polygon;

    public int numberOfNeededAgents = 0;
    public int numberOfNeededPoliceForces = 0;
    public int numberOfNeededFireBrigades = 0;
    public int numberOfNeededAmbulanceTeams = 0;

    private List<MrlBuilding> buildings = new ArrayList<MrlBuilding>();
    private List<MrlBuilding> unVisitedBuilding = new ArrayList<MrlBuilding>();
    private Set<HashSet<Building>> fireClusters = new HashSet<HashSet<Building>>();
    private Set<Path> paths = new FastSet<Path>();
    private List<Road> roads = new ArrayList<Road>();
    private List<MrlZone> zones = new ArrayList<MrlZone>();

    private List<RendezvousI> rendezvous = new ArrayList<RendezvousI>();


    public int type;


    //it says, From A to B there is a Path p by time t ==> ((A,B),(p,t))
    protected PathToPartitions pathsToOthers = new PathToPartitions();


    public Partition(int id, Polygon polygon, MrlWorld world) {
        this.world = world;
        this.id = id;

        width = polygon.xpoints[1] - polygon.xpoints[0];
        height = polygon.ypoints[3] - polygon.ypoints[0];

        for (Integer integer : polygon.xpoints) {
            x += integer;
        }
        for (Integer integer : polygon.ypoints) {
            y += integer;
        }
        x /= polygon.xpoints.length;
        y /= polygon.ypoints.length;

        this.centerPosition = new Pair<Integer, Integer>(x, y);

        this.polygon = polygon;

        fillObjects(world);
//        createRendezvous(world);

    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public Area getCentralArea() {
        return centralArea;
    }

    public void setCentralArea(Area centralArea) {
        this.centralArea = centralArea;
    }

    public Pair<Integer, Integer> getCenterPosition() {
        return centerPosition;
    }

    public void setCenterPosition(Pair<Integer, Integer> centerPosition) {
        this.centerPosition = centerPosition;
    }

    public PathToPartitions getPathsToOthers() {
        return pathsToOthers;
    }

    public void setPathsToOthers(PathToPartitions pathsToOthers) {
        this.pathsToOthers = pathsToOthers;
    }

    // type values are
    // none=0;
    // fiery=1;
    // blockade=2;
    // civilian=3;
    // fiery_Blockade=4;
    // fiery_Civilian=5;
    // blockade_Civilian=6;
    // fiery_Blockade_Civilian=7;

    public void setId(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }


    public List<Road> getRoads() {
        return roads;
    }

    public Set<Path> getPaths() {
        return paths;
    }

    public List<MrlBuilding> getBuildings() {
        return buildings;
    }

    public List<EntityID> getBuildingsID() {
        List<EntityID> ids = new ArrayList<EntityID>();
        for (MrlBuilding building : buildings) {
            ids.add(building.getSelfBuilding().getID());
        }
        return ids;
    }

    public Set<HashSet<Building>> getFireClusters() {
        return fireClusters;
    }

    public void setFireClusters(HashSet<HashSet<Building>> fireClusters) {
        this.fireClusters = fireClusters;
    }

//    public List<Path> getUnCheckedPaths() {
//        List<Path> list = new ArrayList<Path>();
//        for (Path path : paths) {
//            if (world.getUnCheckedPaths().contains(path))
//                list.add(path);
//
//        }
//
//        return list;
//    }

    public List<RendezvousI> getRendezvous() {
        return rendezvous;
    }

//    private void createRendezvous(MrlPoliceForceWorld world) {
//        Point[] points = new Point[4];
//    }

    private void fillObjects(MrlWorld world) {
        int rang;
        if (height > width) {
            rang = height;
        } else {
            rang = width;
        }

//TODO:   General Test - Pooya
//        for (MrlZone zone : world.getZones()) {
//            if (this.contains(zone.getCenter())) {
//                zones.add(zone);
//                this.buildings.addAll(zone);
//                this.unVisitedBuilding.addAll(zone);
//            }
//        }


       /* for (StandardEntity standardEntity : world.getObjectsInRange(this.centerPosition.first(), this.centerPosition.second(), rang)) {

            if (!this.contains(standardEntity, world)) {
                continue;
            }
            if (standardEntity instanceof Building) {
                this.buildings.add(world.getMrlBuilding(standardEntity.getID()));
            } else if (standardEntity instanceof Road) {
                this.roads.add((Road) standardEntity);
            }


        }*/


//        for (Path path : world.getPaths()) {
////            if (containsByXY(path) && isMostlyInIt(path)) {
//            if (isIn(path.getMiddleRoad().getX(), path.getMiddleRoad().getY())) {
//                paths.add(path);
//            }
//        }


/*
        for (Object obj : world.getBuildings()) {
            Building building = (Building) obj;
            if (contains(building)) {
                buildings.add(building);
            }
        }
        for (Object o : world.getRoads()) {
            Road road = (Road) o;
            if (contains(road)) {
                roads.add(road);
            }
        }
*/


    }

    private boolean isMostlyInIt(Path path) {
        double countIn = 0;
        double all = path.size();
        for (Road road : path) {
            if (contains(road)) {
                countIn++;
            }
        }

        return countIn / all >= 0.5;
    }

    public boolean isIn(int x, int y) {

        return polygon.contains(x, y);
    }

    public boolean contains(Pair<Integer, Integer> pair) {
        return polygon.contains(pair.first(), pair.second());
    }

    public boolean contains(Point point) {
        return polygon.contains(point.getX(), point.getY());
    }

    public boolean contains(StandardEntity standardEntity, MrlWorld world) {
        return polygon.contains(standardEntity.getLocation(world).first(), standardEntity.getLocation(world).second());
    }

    public boolean contains(MrlBuilding bd) {
        return polygon.contains(bd.getSelfBuilding().getX(), bd.getSelfBuilding().getY());
    }

    public boolean contains(Path path) {
        return paths.contains(path);
    }

    private boolean containsByXY(Path path) {


        Road headRoad = path.getHeadOfPath();
        Road tailRoad = path.getEndOfPath();

        boolean isFirstNodeInPartition = false;
        boolean isLastNodeInPartition = false;

        if (polygon.contains(headRoad.getLocation(world).first(), headRoad.getLocation(world).second())) {

            isFirstNodeInPartition = true;
        }

        if (polygon.contains(tailRoad.getLocation(world).first(), tailRoad.getLocation(world).second())) {

            isLastNodeInPartition = true;
        }

        return !(!isFirstNodeInPartition && !isLastNodeInPartition);


    }

    public boolean contains(Road road) {
        return (polygon.contains(road.getX(), road.getY()));
    }

    public boolean contains(Partitionable object) {
        return (polygon.contains(object.x(), object.y()));
    }

    public boolean contains(EntityID entityID) {
        StandardEntity standardEntity = world.getEntity(entityID);
        return (polygon.contains(standardEntity.getLocation(world).first(), standardEntity.getLocation(world).second()));
    }


    public static Comparator Partition_NumberOfAgentsComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            Partition pd1 = (Partition) o1;
            Partition pd2 = (Partition) o2;

            if (pd1.numberOfNeededAgents < pd2.numberOfNeededAgents) //decreas
                return 1;
            if (pd1.numberOfNeededAgents == pd2.numberOfNeededAgents)
                return 0;

            return -1;
        }
    };

    public static Comparator<Partition> Partition_IDComparator = new Comparator<Partition>() {
        public int compare(Partition pd1, Partition pd2) {
            if (pd1.id > pd2.id)
                return 1;
            if (pd1.id == pd2.id)
                return 0;

            return -1;
        }
    };

    public boolean isSearchFinished() {
        for (MrlBuilding building : buildings) {
            if (!building.isVisited())
                return false;
        }
        return true;
    }

    public Condition ML_OBJS_IN_PART_CND = new Condition() {
        public boolean eval(Object obj) {
            if (obj instanceof Partitionable) {
                Partitionable p = (Partitionable) obj;
                return isIn(p.x(), p.y());
            } else return false;
        }
    };

    public int getDistanceToCenter(int x, int y) {
        int xc = 0, yc = 0;
        for (int i = 0; i < this.polygon.xpoints.length; i++) {
            xc += this.polygon.xpoints[i];
            yc += this.polygon.ypoints[i];

        }

        xc = (xc / polygon.xpoints.length);
        yc = (yc / polygon.xpoints.length);

        return Util.distance(x, y, xc, yc);
    }

    public int getDistanceToCenter(StandardEntity standardEntity) {
        return getDistanceToCenter(standardEntity.getLocation(world).first(), standardEntity.getLocation(world).second());
    }

    public int getDistanceToCenter(Building building) {
        return getDistanceToCenter(building.getLocation(world).first(), building.getLocation(world).second());
    }


    public List<MrlZone> getZones() {
        return zones;
    }

    public List<MrlBuilding> getUnVisitedBuilding() {
        return unVisitedBuilding;
    }

    public void updateBuildingConditions() {
        buildings.clear();
        for (Object obj : world.getBuildings()) {
            MrlBuilding building = (MrlBuilding) obj;
            if (contains(building)) {
                buildings.add(building);
            }
        }
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isNearRendezvousTime(int time, int rendezvousCheckPeriod) {

        return (((time + 2) % rendezvousCheckPeriod == 0) || ((time + 1) % rendezvousCheckPeriod == 0) || (time % rendezvousCheckPeriod == 0)
                || ((time - 1) % rendezvousCheckPeriod == 0) || ((time - 2) % rendezvousCheckPeriod == 0));
    }

    public int getRendezvouseToGo(int time, int rendezvousCheckPeriod) {
        int nextRenTime = getRendezvousTime(time, rendezvousCheckPeriod);
        int t = nextRenTime / rendezvousCheckPeriod;

        if (t % 2 != 0) {
            return 0;
        } else {
            return 1;
        }
    }

    private int getRendezvousTime(int time, int rendezvousCheckPeriod) {

        if (time % rendezvousCheckPeriod > 5) {
            return time + (rendezvousCheckPeriod - time % rendezvousCheckPeriod);
        } else {
            return time - time % rendezvousCheckPeriod;
        }
    }


}
