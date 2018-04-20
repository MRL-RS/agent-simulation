package mrl.partitioning;

import javolution.util.FastMap;
import javolution.util.FastSet;
import math.geom2d.Point2D;
import math.geom2d.line.Line2D;
import math.geom2d.polygon.Polygon2DUtils;
import math.geom2d.polygon.SimplePolygon2D;
import mrl.common.ConvexHull;
import mrl.common.Util;
import mrl.partitioning.segmentation.EntityCluster;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import mrl.world.object.Route;
import mrl.world.routing.path.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Partition class used to divide world to smaller parts
 *
 * @author Pooya Deldar Gohardani
 * @author Siavash
 * @version 1.0
 */
public class Partition {
    private static final Log logger = LogFactory.getLog(Partition.class);

    private List<MrlBuilding> buildings = new ArrayList<MrlBuilding>();
    private List<Building> buildingEntities = new ArrayList<Building>();
    private Set<EntityID> buildingIDs = new FastSet<EntityID>();
    private List<EntityID> burningBuildings = new ArrayList<EntityID>();
    private List<EntityID> blockedAgents = new ArrayList<EntityID>();
    private List<EntityID> buriedAgents = new ArrayList<EntityID>();
    private Set<StandardEntity> victims = new FastSet<StandardEntity>();
    private List<EntityID> unVisitedBuilding = new ArrayList<EntityID>();
    private List<Refuge> refuges = new ArrayList<Refuge>();
    private Set<HashSet<Building>> fireClusters = new HashSet<HashSet<Building>>();
    private Set<Path> paths = new FastSet<Path>();
    private Set<Road> roads = new HashSet<Road>();
    private Set<EntityID> roadIDs = new FastSet<EntityID>();
    private List<EntityID> rendezvous = new ArrayList<EntityID>();
    private List<Partition> subPartitions = new ArrayList<Partition>();
    private List<Pair<Line2D, Partition>> neighboursByEdge = new ArrayList<Pair<Line2D, Partition>>();
    private Set<EntityID> neighbours;
    private Set<EntityID> refugePathsToClearInPartition;

    //A map of Position to Entity
    private Map<StandardEntity, List<StandardEntity>> entityPositionMap;


    private double value;
    private double size;
    private int numberOfNeededPFs;
    private int numberOfNeededATs;
    private boolean isDead;
    private boolean isDone;

    private MrlWorld world;

    private Polygon polygon;
    private List<Line2D> boundaryLines;
    private Pair<Integer, Integer> center;
    private StandardEntity centerEntity;
    private EntityID id;
    private Map<EntityID, Route> neighbourRoutes;


    public Partition(MrlWorld world, Polygon polygon) {
        this.world = world;
        this.polygon = polygon;
        computeBoundaryLines();
        computeCenter();
        fillProperties();
        generateId();
        computeSize();

    }

    public Partition(MrlWorld world, EntityCluster entityCluster, Map<StandardEntity, List<StandardEntity>> globalEntityPositionMap) {
        this.world = world;
        if (globalEntityPositionMap != null) {
            fillEntityPositionMap(entityCluster, globalEntityPositionMap);
        }
        fillProperties(entityCluster);
        computeConvexHull();
        generateId();
        if (paths.isEmpty()) {
            logger.debug("No path exists on partition : " + this.getId());
        }
    }

    private void fillEntityPositionMap(EntityCluster entityCluster, Map<StandardEntity, List<StandardEntity>> globalEntityPositionMap) {
        this.entityPositionMap = new FastMap<StandardEntity, List<StandardEntity>>();
        for (StandardEntity entity : entityCluster.getEntities()) {
            this.entityPositionMap.put(entity, globalEntityPositionMap.get(entity));
        }
    }


    private void fillProperties(EntityCluster entityCluster) {
        int sumOfX = 0;
        int sumOfY = 0;
        int size = entityCluster.getEntities().size();
        for (StandardEntity entity : entityCluster.getEntities()) {
            sumOfX += entity.getLocation(world).first();
            sumOfY += entity.getLocation(world).second();
            if (entity instanceof Building) {
                buildings.add(world.getMrlBuilding(entity.getID()));
                buildingEntities.add((Building) entity);
                buildingIDs.add(entity.getID());
                if (entity instanceof Refuge) {
                    refuges.add((Refuge) entity);
                }
            } else if (entity instanceof Road) {

                roads.add((Road) entity);
                roadIDs.add(entity.getID());
            }

        }

        //adding paths
        for (Path path : world.getPaths()) {
            for (Road road : path) {
                if (roads.contains(road)) {
                    paths.add(path);
                    break;
                }
            }
        }

        this.center = new Pair<Integer, Integer>(sumOfX / size, sumOfY / size);
        this.centerEntity = findCenterEntity();

    }

    private StandardEntity findCenterEntity() {
        int minDistance = Integer.MAX_VALUE;
        int tempDistance;
        StandardEntity nearestEntityToCenter = null;
        for (StandardEntity entity : roads) {
            tempDistance = Util.distance(center, entity.getLocation(world));
            if (tempDistance <= minDistance) {
                minDistance = tempDistance;
                nearestEntityToCenter = entity;
            }
        }
        return nearestEntityToCenter;
    }

    private void computeSize() {
        size = buildings.size();
    }


    /**
     * find nearest entity to the center and put its id as partition ID
     */
    private void generateId() {
        //TODO @BrainX Generate a fast and reliable ID based on some ID Generation Policy
        if (centerEntity != null) {
            id = centerEntity.getID();
        } else {
            int value = buildings.size() * center.first() + roads.size() * center.second();
            id = new EntityID(value);
        }
    }

    private void computeBoundaryLines() {
        boundaryLines = new ArrayList<Line2D>();
        for (int i = 0; i < polygon.npoints; i++) {
            if (i + 1 < polygon.npoints) {
                boundaryLines.add(new Line2D(polygon.xpoints[i], polygon.ypoints[i], polygon.xpoints[i + 1], polygon.ypoints[i + 1]));
            } else {
                boundaryLines.add(new Line2D(polygon.xpoints[i], polygon.ypoints[i], polygon.xpoints[0], polygon.ypoints[0]));
            }
        }

    }

    /**
     * computes and sets the center position of the partition
     */
    private void computeCenter() {
        int sumX = 0;
        int sumY = 0;
        for (int i = 0; i < polygon.npoints; i++) {
            sumX += polygon.xpoints[i];
            sumY += polygon.ypoints[i];
        }
        center = new Pair<Integer, Integer>(sumX / polygon.npoints, sumY / polygon.npoints);
    }

    /**
     * fills Building, Road, refuges and path lists
     */
    private void fillProperties() {

        //find the range to get entities of the world in this range
        int rang;
        Rectangle2D rectangle = polygon.getBounds2D();
        if (rectangle.getWidth() > rectangle.getHeight()) {
            rang = (int) rectangle.getWidth();
        } else {
            rang = (int) rectangle.getHeight();
        }

        Building building;
        // check entities just in rang of this polygon boundary
        for (StandardEntity standardEntity : world.getObjectsInRange(this.center.first(), this.center.second(), rang)) {

            // check if this entity exists in this polygon
            if (!this.contains(standardEntity, world)) {
                continue;
            }
            //add buildings
            if (standardEntity instanceof Building) {
                building = (Building) world.getEntity(standardEntity.getID());
                MrlBuilding mrlBuilding = world.getMrlBuilding(standardEntity.getID());
                this.buildings.add(mrlBuilding);
                this.buildingEntities.add((Building) standardEntity);
                this.buildingIDs.add(standardEntity.getID());

                //The following instructions add path of Buildings which placed in partitions but entrances of them are not placed.
                MrlRoad mrlRoad;
                for (Entrance entrance : mrlBuilding.getEntrances()) {
                    mrlRoad = world.getMrlRoad(entrance.getNeighbour().getID());
                    paths.addAll(mrlRoad.getPaths());
                }

                //add refuges
                if (building instanceof Refuge) {
                    refuges.add((Refuge) building);
                }
                // add roads
            } else if (standardEntity instanceof Road) {
                this.roads.add((Road) standardEntity);
                this.roadIDs.add(standardEntity.getID());
            }

        }

        for (Path path : world.getPaths()) {
//            if (containsByXY(path) /*&& isMostlyInIt(path)*/) {
            if (isIn(path.getMiddleRoad().getX(), path.getMiddleRoad().getY())) {
                paths.add(path);
            }

            if (isIn(path.getHeadOfPath().getX(), path.getHeadOfPath().getY())) {
                paths.add(path);
            }
        }

        this.centerEntity = findCenterEntity();

//        }

    }


    public List<MrlBuilding> getBuildings() {
        return buildings;
    }


    public List<Building> getBuildingEntities() {
        return buildingEntities;
    }

    public void setBuildings(List<MrlBuilding> buildings) {
        this.buildings = buildings;
    }

    public List<EntityID> getBurningBuildings() {
        return burningBuildings;
    }

    public void setBurningBuildings(List<EntityID> burningBuildings) {
        this.burningBuildings = new ArrayList<EntityID>(burningBuildings);
    }

    public Set<EntityID> getBuildingIDs() {
        return buildingIDs;
    }

    public List<EntityID> getUnVisitedBuilding() {
        return unVisitedBuilding;
    }

    public void setUnVisitedBuilding(List<EntityID> unVisitedBuilding) {
        this.unVisitedBuilding = new ArrayList<EntityID>(unVisitedBuilding);
    }

    public List<Refuge> getRefuges() {
        return refuges;
    }

    public void setRefuges(List<Refuge> refuges) {
        this.refuges = refuges;
    }

    public Set<HashSet<Building>> getFireClusters() {
        return fireClusters;
    }

    public void setFireClusters(Set<HashSet<Building>> fireClusters) {
        this.fireClusters = fireClusters;
    }

    public Set<Path> getPaths() {
        return paths;
    }

    public void setPaths(Set<Path> paths) {
        this.paths = paths;
    }

    public Set<Road> getRoads() {
        return roads;
    }

    public Set<EntityID> getRoadIDs() {
        return roadIDs;
    }

    public void setRoads(List<Road> roads) {
        this.roads.clear();
        this.roads.addAll(roads);
    }

    public List<EntityID> getRendezvous() {
        return rendezvous;
    }

    public List<Partition> getSubPartitions() {
        return subPartitions;
    }

    public void setSubPartitions(List<Partition> subPartitions) {
        this.subPartitions = new ArrayList<Partition>();
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getNumberOfNeededPFs() {
        return numberOfNeededPFs;
    }

    public void setNumberOfNeededPFs(int numberOfNeededPFs) {
        this.numberOfNeededPFs = numberOfNeededPFs;
    }

    public int getNumberOfNeededATs() {
        return numberOfNeededATs;
    }

    public void setNumberOfNeededATs(int numberOfNeededATs) {
        this.numberOfNeededATs = numberOfNeededATs;
    }


    public int findNumberOfNeededAgents(StandardEntity agentEntity) {

        if (agentEntity instanceof PoliceForce) {
            return numberOfNeededPFs;
        } else if (agentEntity instanceof AmbulanceTeam) {
            return numberOfNeededATs;
        }
        return 0;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public void addNeighbours(Line2D commonLine, Partition partition) {
        this.neighboursByEdge.add(new Pair<Line2D, Partition>(commonLine, partition));
    }

    public List<Pair<Line2D, Partition>> getNeighboursByEdge() {
        return neighboursByEdge;
    }

    public Pair<Integer, Integer> getCenter() {
        return center;
    }

    public void setCenter(Pair<Integer, Integer> center) {
        this.center = center;
    }

    public List<Line2D> getBoundaryLines() {
        return boundaryLines;
    }

    public EntityID getId() {
        return id;
    }


    public List<EntityID> getBlockedAgents() {
        return blockedAgents;
    }

    public void setBlockedAgents(List<EntityID> blockedAgents) {
        this.blockedAgents = new ArrayList<EntityID>(blockedAgents);
    }

    public List<EntityID> getBuriedAgents() {
        return buriedAgents;
    }

    public void setBuriedAgents(List<EntityID> buriedAgents) {
        this.buriedAgents = new ArrayList<EntityID>(buriedAgents);
    }

    public Set<StandardEntity> getVictims() {
        return victims;
    }

    public void setVictims(Set<StandardEntity> victims) {
        this.victims = victims;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Partition)) {
            return false;
        }
        Partition partition = (Partition) obj;
        return (this.id.equals(partition.getId()));
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static Comparator<Partition> PARTITION_NEEDED_PF_COMPARATOR = new Comparator<Partition>() { //decrease
        public int compare(Partition p1, Partition p2) {

            if (p1.getNumberOfNeededPFs() < p2.getNumberOfNeededPFs())
                return 1;
            if (p1.getNumberOfNeededPFs() == p2.getNumberOfNeededPFs())
                return 0;

            return -1;
        }
    };

    public static Comparator<Partition> PARTITION_NEEDED_AT_COMPARATOR = new Comparator<Partition>() { //decrease
        public int compare(Partition p1, Partition p2) {

            if (p1.getNumberOfNeededATs() < p2.getNumberOfNeededATs())
                return 1;
            if (p1.getNumberOfNeededATs() == p2.getNumberOfNeededATs())
                return 0;

            return -1;
        }
    };


    public static Comparator<Partition> PARTITION_VALUE_COMPARATOR = new Comparator<Partition>() {
        public int compare(Partition p1, Partition p2) {

            if (p1.getValue() < p2.getValue())
                return 1;
            if (p1.getValue() == p2.getValue())
                return 0;
            return -1;
        }
    };
    public static Comparator<Partition> PARTITION_SIZE_COMPARATOR_DESCENDING = new Comparator<Partition>() {
        public int compare(Partition p1, Partition p2) {

            if (p1.getSize() < p2.getSize())
                return 1;
            if (p1.getSize() == p2.getSize())
                return 0;

            return -1;
        }
    };

    public static Comparator<Partition> PARTITION_SIZE_COMPARATOR_ASCENDING = new Comparator<Partition>() {
        public int compare(Partition p1, Partition p2) {

            if (p1.getSize() > p2.getSize())
                return 1;
            if (p1.getSize() == p2.getSize())
                return 0;

            return -1;
        }
    };


    public static Comparator<Partition> PARTITION_DENSITY_COMPARATOR_ASCENDING = new Comparator<Partition>() {
        public int compare(Partition p1, Partition p2) {

            if ((double) p1.getSize() / p1.getNumberOfNeededPFs() > (double) p2.getSize() / p1.getNumberOfNeededPFs())
                return 1;
            if ((double) p1.getSize() / p1.getNumberOfNeededPFs() == (double) p2.getSize() / p1.getNumberOfNeededPFs())
                return 0;

            return -1;
        }
    };

    public static Comparator<Partition> PARTITION_ID_COMPARATOR = new Comparator<Partition>() {
        public int compare(Partition p1, Partition p2) {

            if (p1.getId().getValue() < p2.getId().getValue())
                return 1;
            if (p1.getId().getValue() == p2.getId().getValue())
                return 0;

            return -1;
        }
    };


    /**
     * eating another partition, means it will add all properties of another partition to itself
     *
     * @param partition partition to eat
     */

    public void eat(Partition partition) {
        buildings.addAll(partition.getBuildings());
        buildingEntities.addAll(partition.getBuildingEntities());
        unVisitedBuilding.addAll(partition.getUnVisitedBuilding());
        refuges.addAll(partition.getRefuges());
        fireClusters.addAll(partition.getFireClusters());
        paths.addAll(partition.getPaths());
        roads.addAll(partition.getRoads());

        blockedAgents.addAll(partition.getBlockedAgents());
        buriedAgents.addAll(partition.getBuriedAgents());
        burningBuildings.addAll(partition.getBurningBuildings());


//        rendezvous.addAll(partition.rendezvous);
        for (EntityID id : partition.getRendezvous()) {
            if (this.getRendezvous().contains(id)) {
                rendezvous.remove(id);
                continue;
            }
            rendezvous.add(id);
        }
//        subPartitions.addAll(partition.getSubPartitions());
        numberOfNeededPFs += partition.getNumberOfNeededPFs();
        size += partition.getSize();


//        ConvexHull convexHull = new ConvexHull();
//        for (int i = 0; i < polygon.npoints; i++) {
//            convexHull.addPoint(polygon.xpoints[i], polygon.ypoints[i]);
//        }
//
//        for (int i = 0; i < partition.getPolygon().npoints; i++) {
//            convexHull.addPoint(partition.getPolygon().xpoints[i], partition.getPolygon().ypoints[i]);
//        }
//
//        polygon = convexHull.convex();
//
//
//

        SimplePolygon2D p = new SimplePolygon2D();
        for (int i = 0; i < polygon.npoints; i++) {
            p.addVertex(new Point2D(polygon.xpoints[i], polygon.ypoints[i]));
        }

        SimplePolygon2D p2 = new SimplePolygon2D();
        for (int i = 0; i < partition.getPolygon().npoints; i++) {
            p2.addVertex(new Point2D(partition.getPolygon().xpoints[i], partition.getPolygon().ypoints[i]));
        }

        Polygon po = new Polygon();
        for (Point2D point2D : Polygon2DUtils.union(p.complement(), p2.complement()).getVertices()) {
            po.addPoint((int) point2D.getX(), (int) point2D.getY());
        }


        polygon = po;
        computeCenter();

        //TODO set neighboursByEdge
        Line2D commonLine;
        List<Pair<Line2D, Partition>> toRemove = new ArrayList<Pair<Line2D, Partition>>();
        List<Pair<Line2D, Partition>> toChange = new ArrayList<Pair<Line2D, Partition>>();
        for (Pair<Line2D, Partition> neighbourPartition : partition.getNeighboursByEdge()) {
            if (neighbourPartition.second().equals(this)) {
                toRemove.add(neighbourPartition);
            } else {
                toChange.add(neighbourPartition);
            }
        }

        partition.getNeighboursByEdge().removeAll(toRemove);
        toRemove.clear();

        for (Pair<Line2D, Partition> neighbourPartition : toChange) {
            neighbourPartition.second().refreshNeighbour(partition, this);
        }


        for (Pair<Line2D, Partition> neighbourPartition : neighboursByEdge) {
            if (neighbourPartition.second().equals(partition)) {
                toRemove.add(neighbourPartition);
            }
        }

        neighboursByEdge.removeAll(toRemove);
        neighboursByEdge.addAll(partition.getNeighboursByEdge());


        //TODO remake polygon
/*
       Coordinate[] coordinates = new Coordinate[polygon.npoints + partition.getPolygon().npoints];

        for (int i = 0; i < polygon.npoints; i++) {
            coordinates[i] = new Coordinate(polygon.xpoints[i], polygon.ypoints[i]);
        }
        int j=polygon.npoints;
        for (int i = 0; i < partition.getPolygon().npoints; i++) {
            coordinates[j] = new Coordinate(partition.getPolygon().xpoints[i], partition.getPolygon().ypoints[i]);
            j++;
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        LinearRing linearRing = geometryFactory.createLinearRing(coordinates);

        Polygon newPolygon=new Polygon();
        for (Coordinate coordinate:linearRing.getCoordinates()){
            newPolygon.addPoint((int)coordinate.x,(int)coordinate.y);
        }
        polygon=newPolygon;

*/

        //TODO find new Center

    }


    /**
     * change old neighbour partition with new partition
     *
     * @param oldPartition the partition which should be changed
     * @param newPartition the partition which is the new one
     */
    private void refreshNeighbour(Partition oldPartition, Partition newPartition) {
        List<Pair<Line2D, Partition>> toRemove = new ArrayList<Pair<Line2D, Partition>>();
        List<Pair<Line2D, Partition>> toAdd = new ArrayList<Pair<Line2D, Partition>>();
        for (Pair<Line2D, Partition> neighbour : neighboursByEdge) {
            if (neighbour.second().equals(oldPartition)) {
                toRemove.add(neighbour);
                toAdd.add(new Pair<Line2D, Partition>(neighbour.first(), newPartition));
            }
        }

        neighboursByEdge.removeAll(toRemove);
        neighboursByEdge.addAll(toAdd);
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

    public boolean contains(EntityID entityID, MrlWorld world) {
        return polygon.contains(world.getEntity(entityID).getLocation(world).first(), world.getEntity(entityID).getLocation(world).second());
    }

    public boolean containsInEntities(EntityID entityID) {
        return (buildingIDs.contains(entityID) || roadIDs.contains(entityID));

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

    public int getDistanceToCenter(int x, int y) {

        return Util.distance(x, y, center.first(), center.second());
    }


    public int getDistanceToCenter(StandardEntity standardEntity) {
        return getDistanceToCenter(standardEntity.getLocation(world).first(), standardEntity.getLocation(world).second());
    }


    public void computeConvexHull() {
        ConvexHull convexHull = new ConvexHull();
        for (MrlBuilding mrlBuilding : buildings) {
            for (int i = 0; i < mrlBuilding.getSelfBuilding().getApexList().length; i += 2) {
                convexHull.addPoint(mrlBuilding.getSelfBuilding().getApexList()[i],
                        mrlBuilding.getSelfBuilding().getApexList()[i + 1]);
            }
        }
        for (Road road : roads) {
            for (int i = 0; i < road.getApexList().length; i += 2) {
                convexHull.addPoint(road.getApexList()[i], road.getApexList()[i + 1]);
            }
        }

        this.polygon = convexHull.convex();

    }


    /**
     * Determines whether this partition is lost(on fire building more than a determined percentage)
     *
     * @return true is there are on fire building more than a determined percentage
     */
    public boolean isDead() {
        return isDead;
    }

    /**
     * Determines whether all task in this partition is done or not
     *
     * @return true if there is no task i this partition
     */
    public boolean isDone() {
        return isDone;
    }

    public void setDead(boolean dead) {
//        if(dead){
//            System.out.println("zdcsadcasv");
//        }
        isDead = dead;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public StandardEntity getCenterEntity() {
        return centerEntity;
    }

    public Set<EntityID> getRefugePathsToClearInPartition() {
        return refugePathsToClearInPartition;
    }

    public void setRefugePathsToClearInPartition(Set<EntityID> refugePathsToClearInPartition) {
        this.refugePathsToClearInPartition = new FastSet<EntityID>(refugePathsToClearInPartition);
    }


    /**
     * Gets a map of each position(position of an entity) in this partition to its entity <br/>
     * <br/>
     * <b>Note:</b> This method should be used when our partitions is constructed based on some specific entities,<br/>
     * such as FB agents, AT agents and Refuges
     *
     * @return map of entity positions to their entities
     */
    public Map<StandardEntity, List<StandardEntity>> getEntityPositionMap() {
        return entityPositionMap;
    }

    public void setNeighbours(Set<EntityID> neighboursByEdge) {
        this.neighbours = neighboursByEdge;
    }

    public Set<EntityID> getNeighbours() {
        return neighbours;
    }

    public void setNeighbourRoutes(Map<EntityID, Route> neighbourRoutes) {
        this.neighbourRoutes = neighbourRoutes;
    }

    public Map<EntityID, Route> getNeighbourRoutes() {
        return neighbourRoutes;
    }
}