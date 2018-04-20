package mrl.police.moa;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/27/12
 * Time: 9:42 PM
 */

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.common.Util;
import mrl.helper.HumanHelper;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.task.PoliceActionStyle;
import mrl.task.Task;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * This class prepares and calculates every needed information
 */
public class PoliceForceUtilities {
    private MrlWorld world;

    private Set<StandardEntity> healthyPoliceForces;
    private List<StandardEntity> healthyCoworkers;

    protected Set<EntityID> doneTargets;

    //    private Config config;
    public static int CLEAR_DISTANCE;
    private HumanHelper humanHelper;

    public PoliceForceUtilities(MrlWorld world, Config config) {
        this.world = world;
        this.healthyPoliceForces = new FastSet<StandardEntity>();
        this.healthyCoworkers = new ArrayList<StandardEntity>();

//        this.config = config;
        CLEAR_DISTANCE = config.getIntValue(MrlPlatoonAgent.MAX_CLEAR_DISTANCE_KEY);
        this.doneTargets = new FastSet<EntityID>();

        humanHelper = world.getHelper(HumanHelper.class);

    }


    /**
     * Takes an entity and gets its importance based on predefined importance values
     *
     * @param entity   the entity to get its importance
     * @param position position id of the target
     * @param isBuried is target buried or not
     * @return the importance of selected entity
     */
    private Pair<Importance, Integer> getImportance(StandardEntity entity, EntityID position, boolean isBuried) {
        //TODO: @Pooya  Rewrite the whole method

        Building building;
        Importance importanceType;
        int importanceValue = 0;
        int inViewDistanceSum = 1000 - 10 * world.getTime();

        if (inViewDistanceSum < 0) {
            inViewDistanceSum = 0;
        }

        StandardEntity pos;
        Human human = null;

        if (entity instanceof Human) {
            human = (Human) entity;
            pos = human.getPosition(world);
        } else {
            pos = entity;
        }

        if (world.getChanges().contains(entity.getID()) && Util.distance(world.getSelfLocation(), pos.getLocation(world)) <= world.getViewDistance()) {
            importanceValue += inViewDistanceSum;
        }


        if (!(entity instanceof Building) && entity instanceof Civilian && world.getEntity(position) instanceof Building && !isBuried) {
            building = (Building) world.getEntity(position);
            if (building.isFierynessDefined() && building.getFieryness() == 0) {
                Human h = (Human) entity;
                if (h.isDamageDefined() && h.getDamage() == 0) {
                    importanceType = Importance.BUILDING_WITH_HEALTHY_HUMAN;
                    importanceValue += importanceType.getImportance();
                } else if (h.isDamageDefined() && h.getDamage() != 0) {
                    importanceType = Importance.BUILDING_WITH_DAMAGED_CIVILIAN;
                    importanceValue += importanceType.getImportance();
                } else {// undefined properties
                    importanceType = Importance.DEFAULT;
                    importanceValue = importanceType.getImportance();
                }
                return new Pair<Importance, Integer>(importanceType, importanceValue);
            }
        }

        //checking Refuge Building
        if (entity instanceof Refuge) {
            importanceType = Importance.REFUGE_ENTRANCE;
            importanceValue += importanceType.getImportance();
            return new Pair<Importance, Integer>(importanceType, importanceValue);

            //checking Fiery Building
        } else if (entity instanceof Building) {
            building = (Building) entity;

            if (!building.isFierynessDefined() || building.getFieryness() == 0) {
                int i = 0;
                for (Human h : world.getMrlBuilding(entity.getID()).getCivilians()) {
                    if (h.isBuriednessDefined() && h.getBuriedness() == 0) {
                        i++;
                    }
                }
                importanceType = Importance.BUILDING_WITH_HEALTHY_HUMAN;
                importanceValue += importanceType.getImportance() * i;
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else if (building.isFierynessDefined()) {
                if (building.getFieryness() == 1) {
                    importanceType = Importance.FIERY_BUILDING_1;
                    importanceValue += importanceType.getImportance();
                    return new Pair<Importance, Integer>(importanceType, importanceValue);
                } else if (building.getFieryness() == 2) {
                    importanceType = Importance.FIERY_BUILDING_2;
                    importanceValue += importanceType.getImportance();
                    return new Pair<Importance, Integer>(importanceType, importanceValue);
                } else if (building.getFieryness() == 3 || (building.isTemperatureDefined() && building.getTemperature() > 0)) {
                    importanceType = Importance.FIERY_BUILDING_3;
                    importanceValue += importanceType.getImportance();
                    return new Pair<Importance, Integer>(importanceType, importanceValue);
                }
            }

        } else if (entity instanceof Road) {
            importanceType = Importance.DEFAULT;
            importanceValue = importanceType.getImportance();
            return new Pair<Importance, Integer>(importanceType, importanceValue);
        }

        // checking Fire Brigade
        if (entity instanceof FireBrigade) {
            if (isBuried(human)) {
                importanceType = Importance.BURIED_FIRE_BRIGADE;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = Importance.BLOCKED_FIRE_BRIGADE;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            }

            //checking Police Force
        } else if (entity instanceof PoliceForce) {
            importanceType = Importance.BURIED_POLICE_FORCE;
            importanceValue += importanceType.getImportance();
            return new Pair<Importance, Integer>(importanceType, importanceValue);


            //checking Ambulance Team
        } else if (entity instanceof AmbulanceTeam) {
            if (isBuried(human)) {
                importanceType = Importance.BURIED_AMBULANCE_TEAM;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = Importance.BLOCKED_AMBULANCE_TEAM;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            }

            //checking Civilian
        } else if (entity instanceof Civilian) {
            if (isBuried(human)) {
                importanceType = Importance.BUILDING_WITH_DAMAGED_CIVILIAN;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = Importance.DEFAULT;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            }

        }

        importanceType = Importance.DEFAULT;
        return new Pair<Importance, Integer>(importanceType, importanceType.getImportance());


    }

    private boolean isBuried(Human human) {
        return human.isBuriednessDefined() && human.getBuriedness() > 0;
    }


    /**
     * Estimated time to clear the target
     *
     * @param task the task which should estimate its doing time
     * @return time to free
     */

    public int timeToFree(Task task) {
        //TODO:
        throw new NotImplementedException();
    }


    /**
     * get me as platoon agent
     *
     * @return MrlPlatoonAgent object of me
     */
    public MrlPlatoonAgent getMyself() {
        return world.getPlatoonAgent();
    }

    /**
     * It is a data structure for keeping a Map of bidder and their bids for each specified target
     *
     * @return bids for each target
     */
    public Map<EntityID, Map<EntityID, Bid>> getTargetBidsMap() {
        return world.getTargetBidsMap();
    }


    /**
     * gets available agents in my working partition
     *
     * @return list of available agents
     */
    public List<StandardEntity> getAvailableAgents() {
        //TODO: change it to its real purpose
        List<StandardEntity> agents = new ArrayList<StandardEntity>(world.getPoliceForces());
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        agents.removeAll(humanHelper.getBuriedAgents());
        return agents;


    }

    /**
     * gets nearest road to this entity, so if it is a road, returns itself otherwise returns nearest road to it
     *
     * @param id    the id of the selected entity
     * @param range range of the distance of the requested road to target
     * @return a road entity
     */
    public Road getNearestRoad(EntityID id, int range) {

        StandardEntity entity = world.getEntity(id);
        Road nearestRoad = null;
        if (entity instanceof Road) {
            nearestRoad = (Road) entity;
        } else if (entity instanceof Building) {
            StandardEntity nearestEntity = null;
            int minDistance = Integer.MAX_VALUE;
            int tempDistance;
            Building building = (Building) entity;


            for (StandardEntity standardEntity : world.getObjectsInRange(id, range)) {
                if (standardEntity instanceof Road) {
//                    tempDistance = Util.findDistanceTo((Area)standardEntity,building.getX(),building.getY());
                    tempDistance = world.getDistance(standardEntity.getID(), world.getSelfPosition().getID());
                    if (tempDistance < minDistance) {
                        nearestEntity = standardEntity;
                        minDistance = tempDistance;
                    }
                }
            }
            if (nearestEntity != null) {
                nearestRoad = (Road) nearestEntity;
            } else {
                // make searching range twice
                for (StandardEntity standardEntity : world.getObjectsInRange(id, range * 2)) {
                    if (standardEntity instanceof Road) {
                        tempDistance = world.getDistance(standardEntity.getID(), world.getSelfPosition().getID());
                        if (tempDistance < minDistance) {
                            nearestEntity = standardEntity;
                            minDistance = tempDistance;
                        }
                    }
                }

                if (nearestEntity != null) {
                    nearestRoad = (Road) nearestEntity;
                }

            }
        }

        return nearestRoad;
    }

    public Road getNearestRoad(Map<EntityID, Boolean> roadIDs) {
        int minDistance = Integer.MAX_VALUE;
        int temp;
        EntityID nearestRoadID = null;
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);
//        CheckAreaPassable checkAreaPassable = world.getPlatoonAgent().getPathPlanner().getAreaPassably();
//        List<EntityID> blockades;

        Road road;
        for (EntityID roadID : roadIDs.keySet()) {

//            if (world.getEntity(roadID) instanceof Road) {
//                road = (Road) world.getEntity(roadID);
//                if (roadHelper.isPassable(roadID) && road.isBlockadesDefined() && road.getBlockades().isEmpty()) {
//                    continue;
//                }
//            }
            //if this road is not cleared
//            blockades = checkAreaPassable.policeCheckPassably((Road) world.getEntity(roadID), null);
//            if (blockades != null && !blockades.isEmpty()) {
            temp = Util.distance(world.getSelfHuman().getLocation(world), world.getEntity(roadID).getLocation(world));
            if (temp < minDistance) {
                minDistance = temp;
                nearestRoadID = roadID;
            }
//            }
        }

        if (nearestRoadID != null) {
            return (Road) world.getEntity(nearestRoadID);
        }

        return null;

    }

    /**
     * Updates {@link #healthyPoliceForces}.
     */
    public void updateHealthyPoliceForceList() {
//        healthyPoliceForces.clear();

        for (PoliceForce policeForce : world.getPoliceForceList()) {
            if (policeForce.getID().equals(world.getSelf().getID())) {      // This "policeForce" is actually "me"
                if (world.getSelfPosition() instanceof Building) {
                    if (world.getSelfHuman().getBuriedness() == 0            // Means that I'm not buried!
                            && world.getTime() >= 2) {                          // exactly at the second cycle, police force agents might get buried.
                        healthyPoliceForces.add(policeForce);
                    } else {
                        // Too soon to decide if I will be buried (at time:2)
                    }
                } else { // I am on the road!
                    healthyPoliceForces.add(policeForce);
                }
            } else { // This "policeForce" is someone else (not me)
                if (humanHelper.getAgentState(policeForce.getID()) == null) { // I have no idea in what state this policeForce is.
                    //TODO @BrainX Is there someway better to access others' states?
                    if (policeForce.isBuriednessDefined() && policeForce.getBuriedness() == 0) { // I have information about this policeForce's buriedness and it's not buried.
                        healthyPoliceForces.add(policeForce);
                    } else if (policeForce.getPosition(world) instanceof Road) { // If I don't know if this policeForce is buried, I consider it healthy if it's on the road.
                        healthyPoliceForces.add(policeForce);
                    }
                } else { // I know this policeForce's state
                    if (humanHelper.isAgentStateHealthy(policeForce.getID())) {
                        healthyPoliceForces.add(policeForce);
                    }
                }
            }
        }
    }

    public void updateHealthyCoworkerList(List<Human> coworkers) {

        healthyCoworkers.clear();
        for (Human human : coworkers) {

            if (human.getID().equals(world.getSelf().getID()) && world.getSelfHuman().getBuriedness() == 0) {
                healthyCoworkers.add(human);
            } else if (!human.getID().equals(world.getSelf().getID())) {
                if (humanHelper.getAgentState(human.getID()) != null) {
                    if ((!humanHelper.getAgentState(human.getID()).equals(State.BURIED) && !humanHelper.getAgentState(human.getID()).equals(State.DEAD))) {
                        healthyCoworkers.add(human);
                    }
                } else {
                    if (human.isBuriednessDefined() && human.getBuriedness() == 0) {
                        healthyCoworkers.add(human);
                    } else if (human.getPosition(world) instanceof Road) {
                        healthyPoliceForces.add(human);
                    }
                }
            }

        }

    }

    public Set<StandardEntity> getHealthyPoliceForces() {
        return healthyPoliceForces;
    }

    public List<StandardEntity> getHealthyCoworkers() {
        return healthyCoworkers;
    }


    public Set<EntityID> getDoneTargets() {
        return doneTargets;
    }

    /**
     * is this agent arrived to the specified position by its assigned task or not
     *
     * @param myTask agent assigned task
     * @return true if arrived
     */
    public boolean isDoneTask(Task myTask) {
        return myTask != null
                && myTask.getTarget().getNearestRoadID() != null
                && world.getSelfPosition().getID().equals(myTask.getTarget().getNearestRoadID());
    }


    /**
     * Gets proper PoliceActionStyle For Each target
     *
     * @param target target to find its proper action style
     * @return proper action style
     */
    public PoliceActionStyle getActionStyle(Target target) {

        PoliceActionStyle policeActionStyle = null;

//        return PoliceActionStyle.CLEAR_TARGET;
        StandardEntity targetEntity = world.getEntity(target.getId());
        StandardEntity targetPosition = world.getEntity(target.getPositionID());

        // if target is a Human
        if (targetEntity instanceof Human) {
            if (targetPosition instanceof Road) {
                policeActionStyle = PoliceActionStyle.CLEAR_HUMAN;
            } else {
                policeActionStyle = PoliceActionStyle.CLEAR_ENTRANCE;
            }

            // if target is a road
        } else if (targetEntity instanceof Road) {
            policeActionStyle = PoliceActionStyle.CLEAR_ALL;

            // if target is building
        } else {

            if (targetPosition instanceof Refuge) {
                policeActionStyle = PoliceActionStyle.CLEAR_ENTRANCE_AND_AROUND;
            } else {
                if (target.getImportanceType().equals(Importance.FIERY_BUILDING_1)
                        || target.getImportanceType().equals(Importance.FIERY_BUILDING_2)
                        || target.getImportanceType().equals(Importance.FIERY_BUILDING_3)) {

                    policeActionStyle = PoliceActionStyle.CLEAR_AROUND;
                } else {
                    policeActionStyle = PoliceActionStyle.CLEAR_ENTRANCE;
                }
            }

        }

        if (policeActionStyle == null) {
            policeActionStyle = PoliceActionStyle.CLEAR_NORMAL;
        }

        return policeActionStyle;
    }


    public Comparator<Target> TARGET_IMPORTANCE_COMPARATOR = new Comparator<Target>() {  //Decrease
        public int compare(Target t1, Target t2) {
            double b1 = t1.getImportance();
            double b2 = t2.getImportance();
            if (b1 < b2)
                return 1;
            if (b1 == b2) {
                // if my distance to t1 is less than distance to t2
                if (t1.getDistanceToIt() > t2.getDistanceToIt()) {
                    return 1;
                } else if (t1.getDistanceToIt() == t2.getDistanceToIt()) {
                    return 0;
                }
            }

            return -1;
        }
    };


    public Map<EntityID, Boolean> getRoadsToMove(Target target) {

        Map<EntityID, Boolean> roadsToMove = new FastMap<EntityID, Boolean>();
        boolean isClear = false;

        PoliceActionStyle policeActionStyle = null;

//        return PoliceActionStyle.CLEAR_TARGET;
        StandardEntity targetEntity = world.getEntity(target.getId());
        StandardEntity targetPosition = world.getEntity(target.getPositionID());

        // if target is a Human
        if (targetEntity instanceof Human) {
            if (targetPosition instanceof Road) {
                roadsToMove.put(targetPosition.getID(), isClear);
            } else {
                MrlBuilding mrlBuilding = world.getMrlBuilding(targetPosition.getID());
                if (mrlBuilding != null && mrlBuilding.getEntrances() != null && !mrlBuilding.getEntrances().isEmpty()) {
                    roadsToMove.put(mrlBuilding.getEntrances().get(0).getNeighbour().getID(), isClear);
                }
            }

            // if target is a road
        } else if (targetEntity instanceof Road) {
            roadsToMove.put(targetPosition.getID(), isClear);

            // if target is building
        } else {

            if (targetPosition instanceof Refuge) {
                //Add all entrance roads
                MrlBuilding mrlBuilding = world.getMrlBuilding(targetPosition.getID());
                if (mrlBuilding != null && mrlBuilding.getEntrances() != null) {
                    for (Entrance entrance : mrlBuilding.getEntrances()) {
                        roadsToMove.put(entrance.getNeighbour().getID(), isClear);
                    }


                }
            } else {
                if (target.getImportanceType().equals(Importance.FIERY_BUILDING_1)
                        || target.getImportanceType().equals(Importance.FIERY_BUILDING_2)
                        || target.getImportanceType().equals(Importance.FIERY_BUILDING_3)) {

                    //a road comfortable for FBs
                    Road road = getNearestRoad(targetPosition.getID(), world.getMaxExtinguishDistance() / 2);
                    if (road != null) {
                        roadsToMove.put(road.getID(), isClear);
                    }
                    //probably, target is a building with human
                } else {
                    MrlBuilding mrlBuilding = world.getMrlBuilding(targetPosition.getID());
                    if (mrlBuilding != null && mrlBuilding.getEntrances() != null && !mrlBuilding.getEntrances().isEmpty()) {
                        List<StandardEntity> entranceEntities = new ArrayList<StandardEntity>();
                        for (Entrance entrance : mrlBuilding.getEntrances()) {
                            entranceEntities.add(entrance.getNeighbour());
                        }
                        Area nearestEntranceRoad = Util.nearestEntityTo(entranceEntities, world.getSelfLocation());
                        roadsToMove.put(nearestEntranceRoad.getID(), isClear);
                    }
                }
            }

        }

        return roadsToMove;
    }


    /**
     * This method finds a position in the specified building to move to that point to be able to see outside blockades
     * and clear them if needed; This point is the passable edge of the building with its entranceRoad
     *
     * @param building the building to find the point to move in it
     * @param road     entrance road to find the proper point based on it
     * @return a Point2D of XY in Integer of the point to move
     */
    public Point2D findMovePointInBuilding(Building building, Road road) {

        Edge commonEdge = building.getEdgeTo(road.getID());
        Point2D movePoint = null;
        if (commonEdge != null) {

            int xPoint = Math.abs(commonEdge.getStartX() + commonEdge.getEndX()) / 2;
            int yPoint = Math.abs(commonEdge.getEndY() + commonEdge.getStartY()) / 2;
            int selfX = world.getSelfLocation().first();
            int selfY = world.getSelfLocation().second();

            Point2D startPoint2D = new Point2D(selfX, selfY);
            Point2D endPoint2D = new Point2D(xPoint, yPoint);

            Line2D line2D = Util.improveLine(new Line2D(startPoint2D, endPoint2D), -500);

            movePoint = line2D.getEndPoint();
        }
        return movePoint;

    }

    public Road findNearestEntrance(List<Entrance> entrances) {
        int distance = Integer.MAX_VALUE;
        int tempDist = 0;
        int selfX = world.getSelfLocation().first();
        int selfY = world.getSelfLocation().second();
        Road bestEntrance = null;
        for (Entrance entrance : entrances) {
            tempDist = Util.distance(selfX, selfY, entrance.getNeighbour().getX(), entrance.getNeighbour().getY());
            if (tempDist < distance) {
                distance = tempDist;
                bestEntrance = entrance.getNeighbour();
            }
        }
        return bestEntrance;
    }

    public Road findEntranceInMyWay(List<Entrance> entrances, List<EntityID> plan) {
        Road bestEntrance = null;
        List<Road> myWayEntrances = new ArrayList<Road>();
        for (Entrance entrance : entrances) {
            if (plan.contains(entrance.getNeighbour().getID())) {
                myWayEntrances.add(entrance.getNeighbour());
            }
        }
        int maxIndex = -2;
        for (Road road : myWayEntrances) {
            int index = plan.indexOf(road.getID());
            if (index > maxIndex) {
                maxIndex = index;
                bestEntrance = road;
            }
        }
        return bestEntrance;
    }

}
