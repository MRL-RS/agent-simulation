/*
package mrl.firebrigade.CLStrategy;

import javolution.util.FastSet;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.MrlFireBrigade;
import mrl.platoon.State;
import mrl.mrlPersonal.viewer.layers.MrlPreRoutingPartitionsLayer;
import mrl.world.MrlWorld;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.routing.path.Path;
import mrl.world.routing.pathPlanner.IPathPlanner;
import mrl.world.routing.pathPlanner.PathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

*/
/**
 * @author Erfan Jazeb Nikoo
 *//*

public class FBCLStrategy {


    private MrlWorld world;
    private MrlFireBrigade me;
    private int numOfFireBrigade;
    private int numOfPartitionPart;
    private boolean flag0 = true;
    private PathPlanner pathPlanner;
    private List<Pair<EntityID, Integer>> fireBrigadeTeamList = new ArrayList<Pair<EntityID, Integer>>();
    private List<StandardEntity> centerOfPart = new ArrayList<StandardEntity>();
    private Map<Integer, ArrayList<EntityID>> halfOfFireBrigade = new HashMap<Integer, ArrayList<EntityID>>();
    private Map<Integer, ArrayList<Human>> halfOfFB = new HashMap<Integer, ArrayList<Human>>();
    private Map<Integer, ArrayList<Path>> zonePaths = new HashMap<Integer, ArrayList<Path>>();
    private Map<Integer, ArrayList<MrlZone>> listOfZones = new HashMap<Integer, ArrayList<MrlZone>>();
    private Map<Integer, ArrayList<MrlZone>> sortedZoneList = new HashMap<Integer, ArrayList<MrlZone>>();
    private Map<Integer, ArrayList<MrlZone>> zoneListOfPart = new HashMap<Integer, ArrayList<MrlZone>>();
    private Map<Integer, Integer> numOfZone = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> numOfFB = new HashMap<Integer, Integer>();
    private Map<Integer, Set<Building>> burningBuilding = new HashMap<Integer, Set<Building>>();
    private Map<Integer, Integer> number = new HashMap<Integer, Integer>();
    private Map<Integer, ArrayList<MrlZone>> halfOfZones = new HashMap<Integer, ArrayList<MrlZone>>();
    private Map<Integer, ArrayList<Road>> zoneFireExplorerRoads = new HashMap<Integer, ArrayList<Road>>();
    private Map<Integer, Road> myCurrentTarget = new HashMap<Integer, Road>();
    private Map<Integer, Set<Road>> targets = new HashMap<Integer, Set<Road>>();
    private Map<Integer, ArrayList<Road>> tempTargets = new HashMap<Integer, ArrayList<Road>>();
    private Map<Integer, Map<EntityID, Integer>> tempListOfTargets = new HashMap<Integer, Map<EntityID, Integer>>();
    private Map<Integer, Map<Integer, EntityID>> backupListOfTargets = new HashMap<Integer, Map<Integer, EntityID>>();


    public FBCLStrategy(MrlWorld world, MrlFireBrigade fireBrigade, IPathPlanner pathPlanner) {
        this.world = world;
        me = fireBrigade;
        this.pathPlanner = (PathPlanner) pathPlanner;
        numOfFireBrigade = world.getFireBrigades().size();
        initializeVariable();
    }

    */
/**
 * Make new every global map list and array list.
 *//*


    private void initializeVariable() {

        if (numOfFireBrigade < 6) {
            numOfPartitionPart = 1;
        } else if (numOfFireBrigade < 8 && numOfFireBrigade > 5) {
            numOfPartitionPart = 2;
        } else {
            numOfPartitionPart = 4;
        }

        for (int i = 0; i < numOfPartitionPart; i++) {
            halfOfZones.put(i, new ArrayList<MrlZone>());
            targets.put(i, new FastSet<Road>());
            tempTargets.put(i, new ArrayList<Road>());
            burningBuilding.put(i, new FastSet<Building>());
            zoneFireExplorerRoads.put(i, new ArrayList<Road>());
            sortedZoneList.put(i, new ArrayList<MrlZone>());
            halfOfFireBrigade.put(i, new ArrayList<EntityID>());
            halfOfFB.put(i, new ArrayList<Human>());
            zonePaths.put(i, new ArrayList<Path>());
            listOfZones.put(i, new ArrayList<MrlZone>());
            zoneListOfPart.put(i, new ArrayList<MrlZone>());
            tempListOfTargets.put(i, new HashMap<EntityID, Integer>());
            backupListOfTargets.put(i, new HashMap<Integer, EntityID>());
            number.put(i, 0);
            myCurrentTarget.put(i, null);
        }
    }

    */
/**
 * @throws mrl.common.CommandException -> For move function
 * @throws mrl.common.TimeOutException ->
 *//*


    public void execute() throws CommandException, TimeOutException {

        int tryCount = 0;
        if (flag0) {
            fillTargets();
            for (int i = 0; i < numOfPartitionPart; i++) {
                divideFireBrigade(new Rectangle(divideMap(i)), i);
            }
            flag0 = false;
        }

        for (int i = 0; i < numOfPartitionPart; i++) {
            if (myCurrentTarget.get(i) != null) {
                for (int j = 0; j < numOfFB.get(i); j++) {
                    if (me.getID().equals(halfOfFB.get(i).get(j).getID()) && myCurrentTarget.get(i) != null) {
                        if (!fireExtinguish(i)) {
                            if (halfOfFB.get(i).get(j).getPosition().equals(myCurrentTarget.get(i).getID())) {
                                number.put(i, number.get(i) + 1);
                            }
                        }
                    }
                }

                if (!checkPassably(myCurrentTarget.get(i)) || world.getPlatoonAgent().isStuck()) {
                    do {
                        if (tryCount == backupListOfTargets.get(i).size()) {
                            break;
                        }
                        number.put(i, number.get(i) + 1);
                        checkNumberEnd(i);
                        tryCount++;
                    }
                    while (!checkPassably((Area) world.getEntity(backupListOfTargets.get(i).get(number.get(i)))));
                }

            }
            checkNumberEnd(i);
            myCurrentTarget.put(i, new Road((Road) world.getEntity(backupListOfTargets.get(i).get(number.get(i)))));

            for (int j = 0; j < numOfFB.get(i); j++) {
                if (me.getID().equals(halfOfFB.get(i).get(j).getID())) {
                    me.move(myCurrentTarget.get(i), 0, false);
                }
            }
        }
    }

    */
/**
 * @param num ->
 * @return ->
 * @throws mrl.common.CommandException ->
 * @throws mrl.common.TimeOutException ->
 *//*


    private boolean fireExtinguish(int num) throws CommandException, TimeOutException {

        Map<Integer, Set<Building>> fireBrigadeSeenBuilding = new HashMap<Integer, Set<Building>>();
        boolean flag;

        fireBrigadeSeenBuilding.put(num, new FastSet<Building>());

        burningBuilding.get(num).clear();

        for (StandardEntity building : world.getBuildings()) {
            fireBrigadeSeenBuilding.get(num).add((Building) building);
        }

        for (Building building : fireBrigadeSeenBuilding.get(num)) {
            if (building.isFierynessDefined() && building.isTemperatureDefined()) {
                if (building.getFieryness() > 0 && building.getFieryness() < 4 || building.getTemperature() > 0) {
                    burningBuilding.get(num).add(building);
                }
            }
        }

        if (!burningBuilding.get(num).isEmpty()) {
            me.setCenterDirection();
            me.ourPreciousAct();
            me.setAgentState(State.SEARCHING);
            me.legacyHeatTracerSearchManager.execute();
//            me.fireSearcher.execute();
            me.simpleSearchManager.execute();
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }

    */
/**
 * @param target ->
 * @return ->
 *//*


    private boolean checkPassably(Area target) {
        return !pathPlanner.planMove(target, MRLConstants.IN_TARGET, false).isEmpty();
    }

    */
/**
 * @param num ->
 *//*


    private void checkNumberEnd(int num) {
        if (backupListOfTargets.get(num).size() <= number.get(num) || number.get(num) == null) {
            number.put(num, 0);
        }
    }

    */
/**
 * Divide map to two same rectangle.
 *
 * @param num -> Index of part.
 * @return Rectangle -> Half of map.
 *//*


    private Rectangle divideMap(int num) {

        int mapHeight = (int) world.getBounds().getHeight();
        int mapWidth = (int) world.getBounds().getWidth();
        int minX = (int) world.getBounds().getMinX();
        int minY = (int) world.getBounds().getMinY();
        int maxX = (int) world.getBounds().getMaxX();
        int maxY = (int) world.getBounds().getMaxY();
        Map<Integer, Rectangle> part = new HashMap<Integer, Rectangle>();

        if (numOfFireBrigade < 6) {
            part.put(0, new Rectangle(minX, minY, mapWidth, mapHeight));
        } else if (numOfFireBrigade < 8 && numOfFireBrigade > 5) {
            part.put(0, new Rectangle(minX, minY, mapWidth / 2, mapHeight));
            part.put(1, new Rectangle(maxX / 2, minY, mapWidth / 2, mapHeight));
        } else {
            part.put(0, new Rectangle(minX, minY, mapWidth / 2, mapHeight / 2));
            part.put(1, new Rectangle(minX, maxY / 2, mapWidth / 2, mapHeight / 2));
            part.put(2, new Rectangle(maxX / 2, minY, mapWidth / 2, mapHeight / 2));
            part.put(3, new Rectangle(maxX / 2, maxY / 2, mapWidth / 2, mapHeight / 2));
        }

        MrlPreRoutingPartitionsLayer.rectangles.addAll(part.values());
        return part.get(num);
    }

    */
/**
 * Divide fire brigade to two same group. Sort of half of fire brigade agent with distance to center of first Part and add to first list and another fire brigade agents add to second list.
 *
 * @param partOfMap -> Part of map which one of fire brigade wants to do search and rescue.
 * @param num       -> Index of fire brigades.
 * @global numOfFireBrigadeAgent ->Number of fire brigade agents.
 * @global fireBrigadeTeamList -> Temporary list of fire brigade for change and sort them.
 * @global halfOfFireBrigade -> This is a map list of fire brigade.In this map list, key is fire brigade index and value is half of fire brigade agents.
 *//*


    private void divideFireBrigade(Rectangle partOfMap, int num) {

        Point center1;
        Human human;
        int counter;
        ArrayList<Pair<EntityID, Integer>> fireBrigadeList1 = new ArrayList<Pair<EntityID, Integer>>();


        if (num == 0) {
            counter = 0;
            center1 = new Point((int) partOfMap.getCenterX(), (int) partOfMap.getCenterY());

            for (StandardEntity fireBrigadeAgent : world.getFireBrigades()) {
                human = (Human) fireBrigadeAgent;
                fireBrigadeList1.add(new Pair<EntityID, Integer>(human.getID(), (int) center1.distance(human.getX(), human.getY())));
                counter++;
            }

            Collections.sort(fireBrigadeList1, ConstantComparators.DISTANCE_VALUE_COMPARATOR);

            fireBrigadeTeamList.clear();
            fireBrigadeTeamList.addAll(fireBrigadeList1);

            for (int i = 0; i < (numOfFireBrigade / numOfPartitionPart); i++) {
                fireBrigadeTeamList.remove(0);
            }

            halfOfFireBrigade.get(0).clear();
            for (int i = 0; i < (numOfFireBrigade / numOfPartitionPart); i++) {
                halfOfFireBrigade.get(0).add(fireBrigadeList1.get(i).first());
            }
            counter = 0;
            for (EntityID entityID : halfOfFireBrigade.get(0)) {
                for (StandardEntity standardEntity : world.getFireBrigades()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfFB.get(0).add(counter, (Human) standardEntity);
                        counter++;
                    }
                }
            }
            numOfFB.put(num, halfOfFB.get(num).size());

        } else if (num == 1) {
            halfOfFireBrigade.get(1).clear();
            for (int i = 0; i < fireBrigadeTeamList.size() / 3; i++) {
                halfOfFireBrigade.get(1).add(fireBrigadeTeamList.get(i).first());
            }

            for (int i = 0; i < halfOfFireBrigade.get(num).size(); i++) {
                fireBrigadeTeamList.remove(0);
            }

            counter = 0;
            for (EntityID entityID : halfOfFireBrigade.get(1)) {
                for (StandardEntity standardEntity : world.getFireBrigades()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfFB.get(1).add(counter, (Human) standardEntity);
                        counter++;
                    }
                }
            }
            numOfFB.put(num, halfOfFB.get(num).size());
        } else if (num == 2) {
            halfOfFireBrigade.get(2).clear();
            for (int i = 0; i < fireBrigadeTeamList.size() / 2; i++) {
                halfOfFireBrigade.get(2).add(fireBrigadeTeamList.get(i).first());
            }

            for (int i = 0; i < halfOfFireBrigade.get(num).size(); i++) {
                fireBrigadeTeamList.remove(0);
            }

            counter = 0;
            for (EntityID entityID : halfOfFireBrigade.get(2)) {
                for (StandardEntity standardEntity : world.getFireBrigades()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfFB.get(2).add(counter, (Human) standardEntity);
                        counter++;
                    }
                }
            }
            numOfFB.put(num, halfOfFB.get(num).size());
        } else if (num == 3) {
            halfOfFireBrigade.get(3).clear();
            for (int i = 0; i < fireBrigadeTeamList.size(); i++) {
                halfOfFireBrigade.get(3).add(fireBrigadeTeamList.get(i).first());
            }

            for (int i = 0; i < halfOfFireBrigade.get(num).size(); i++) {
                fireBrigadeTeamList.remove(0);
            }

            counter = 0;
            for (EntityID entityID : halfOfFireBrigade.get(3)) {
                for (StandardEntity standardEntity : world.getFireBrigades()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfFB.get(3).add(counter, (Human) standardEntity);
                        counter++;
                    }
                }
            }
            numOfFB.put(num, halfOfFB.get(num).size());
        }
    }

    */
/**
 * Choose nearest road to center of part of map.
 *
 * @param partOfMap -> Part of map which one of fire brigade wants to do search and rescue.
 * @return rendezvous -> return nearest standard entity to center of part of map.
 *//*


    private StandardEntity rendezvous(Rectangle partOfMap) {

        Point center;
        Road road;
        double distance;
        double temp = Double.MAX_VALUE;
        StandardEntity nearestRoad = null;

        center = new Point((int) partOfMap.getCenterX(), (int) partOfMap.getCenterY());

        for (StandardEntity standardEntity : world.getRoads()) {
            road = (Road) standardEntity;
            distance = center.distance(road.getX(), road.getY());
            if (distance < temp) {
                temp = distance;
                nearestRoad = road;
            }
        }

        return nearestRoad;
    }

    */
/**
 * Divide zones and add all of them to two array list in one map list.
 *
 * @param partOfMap -> Part of map which one of fire brigade wants to do search and rescue.
 * @param num       -> Index of fire brigade.
 * @return ArrayList<MrlZone> -> List of half of zones in map.
 *//*


    private ArrayList<MrlZone> partOfMapZones(Rectangle partOfMap, int num) {

        for (MrlZone zone : world.getZones()) {
            if (partOfMap.contains(zone.getCenter())) {
                zoneListOfPart.get(num).add(zone);
            }
        }
        return zoneListOfPart.get(num);
    }

    */
/**
 * Choose nearest zone to center of part of map as first zone and calculate next zone with neighbour zone algorithm.
 *
 * @param num -> Index of fire brigade.
 *//*


    private void zoneSortCalc(int num) {

        Map<Integer, Road> centerOfPartRoad = new HashMap<Integer, Road>();
        centerOfPartRoad.put(num, new Road((Road) centerOfPart.get(num)));
        Map<Integer, Point> point = new HashMap<Integer, Point>();
        point.put(num, new Point(centerOfPartRoad.get(num).getX(), centerOfPartRoad.get(num).getY()));
        Map<Integer, MrlZone> bestZone = new HashMap<Integer, MrlZone>();
        double temp = Double.MAX_VALUE;

        for (int i = 0; i < numOfZone.get(num); i++) {
            for (MrlZone zone : listOfZones.get(num)) {
                if (point.get(num).distance(zone.getCenter()) < temp) {
                    temp = point.get(num).distance(zone.getCenter());
                    bestZone.put(num, zone);
                }
            }
            listOfZones.get(num).remove(bestZone.get(num));
            point.put(num, new Point(bestZone.get(num).getCenter()));
            temp = Double.MAX_VALUE;
            sortedZoneList.get(num).add(bestZone.get(num));
        }
    }

    */
/**
 * This function take the path of zone and return a neighbour path in that zone.
 *
 * @param firstPath -> ‌Beginning path for calculate next path in same zone.
 * @param num       -> Index of fire brigade.
 * @return Path -> Neighbour path with first path in same zone.
 *//*


    private Path getZoneNeighbourPath(Path firstPath, int num) {

        Map<Integer, ArrayList<Path>> neighbours = new HashMap<Integer, ArrayList<Path>>();
        neighbours.put(num, new ArrayList<Path>());

        zonePaths.get(num).remove(firstPath);
        for (Path path : zonePaths.get(num)) {
            for (Path p : firstPath.getNeighboursByEdge()) {
                if (path.getId().getValue() == p.getId().getValue()) {
                    neighbours.get(num).add(path);
                }
            }
        }
        if (neighbours.get(num).isEmpty()) {
            neighbours.get(num).addAll(zonePaths.get(num));
        }
        return neighbours.get(num).get(0);

    }

    */
/**
 * @param zone -> Zone of part of map which we need that fire explorer roads.
 * @param num  -> Index of fire brigade.
 * @return ArrayList<Road> -> list of sorted fire explorer roads in zone.
 *//*


    private ArrayList<Road> getZoneFireExplorerRoads(MrlZone zone, int num) {

        Map<Integer, ArrayList<Path>> neighbourPaths = new HashMap<Integer, ArrayList<Path>>();
        Map<Integer, ArrayList<Road>> fireExplorerRoads = new HashMap<Integer, ArrayList<Road>>();
        neighbourPaths.put(num, new ArrayList<Path>());
        fireExplorerRoads.put(num, new ArrayList<Road>());
        Path tempPath = zone.getPaths().get(0);


        zonePaths.get(num).clear();
        zonePaths.get(num).addAll(zone.getPaths());
        neighbourPaths.get(num).add(0, zone.getPaths().get(0));
        for (int i = 0; i < (zone.getPaths().size() - 1); i++) {
            tempPath = getZoneNeighbourPath(tempPath, num);
            neighbourPaths.get(num).add(tempPath);
        }
        fireExplorerRoads.get(num).clear();
        for (int i = 0; i < neighbourPaths.get(num).size(); i += 2) {
            fireExplorerRoads.get(num).add(neighbourPaths.get(num).get(i).getMiddleRoad());
        }

        return fireExplorerRoads.get(num);
    }

    */
/**
 * Fill every variable with functions.
 *//*


    private void fillTargets() {

        int counter;

        if (numOfFireBrigade < 6) {
            numOfPartitionPart = 1;
        } else if (numOfFireBrigade < 8 && numOfFireBrigade > 5) {
            numOfPartitionPart = 2;
        } else {
            numOfPartitionPart = 4;
        }

        for (int i = 0; i < numOfPartitionPart; i++) {
            centerOfPart.add(rendezvous(new Rectangle(divideMap(i))));
        }


        for (int i = 0; i < numOfPartitionPart; i++) {
            halfOfZones.get(i).addAll(partOfMapZones(new Rectangle(divideMap(i)), i));
            listOfZones.get(i).addAll(halfOfZones.get(i));
            numOfZone.put(i, listOfZones.get(i).size());
        }


        for (int i = 0; i < numOfPartitionPart; i++) {
            zoneSortCalc(i);
        }

        for (int i = 0; i < numOfPartitionPart; i++) {
            counter = 0;
            for (int j = 0; j < sortedZoneList.get(i).size(); j++) {
                zoneFireExplorerRoads.get(i).addAll(getZoneFireExplorerRoads(sortedZoneList.get(i).get(j), i));
            }
            targets.get(i).addAll(zoneFireExplorerRoads.get(i));
            tempTargets.get(i).addAll(targets.get(i));

            for (Road road : targets.get(i)) {
                tempListOfTargets.get(i).put(road.getID(), counter);
                backupListOfTargets.get(i).put(counter, road.getID());
                counter++;
            }
        }

    }
}

*/
