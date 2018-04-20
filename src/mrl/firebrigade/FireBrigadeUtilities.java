package mrl.firebrigade;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.common.clustering.FireCluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.simulator.WaterCoolingEstimator;
import mrl.helper.HumanHelper;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.pathPlanner.IPathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/19/13
 * Time: 12:45 AM
 * Author: Mostafa Movahedi
 */
public class FireBrigadeUtilities {
    private MrlFireBrigadeWorld world;
    private Human selfHuman;
    private IPathPlanner pathPlanner;
    private static final int EXTINGUISH_DISTANCE_THRESHOLD = 5000;
    private Set<StandardEntity> readyFireBrigades;
    private HumanHelper humanHelper;


    public FireBrigadeUtilities(MrlFireBrigadeWorld world) {
        this.world = world;
        this.selfHuman = world.getSelfHuman();
        this.pathPlanner = world.getPlatoonAgent().getPathPlanner();
        this.humanHelper = world.getHelper(HumanHelper.class);
        readyFireBrigades = new FastSet<StandardEntity>();

    }

    public void calcClusterCondition(MrlFireBrigadeWorld world, FireCluster fireCluster) {
        double effectiveWater = calcEffectiveWaterPerCycle(world, fireCluster.getCenter());
        int neededWater = fireCluster.calcNeededWaterToExtinguish() / 10;
        double totalEffectiveWater = effectiveWater * (world.getFireBrigades().size() / 2);
        if (neededWater < totalEffectiveWater) {
            fireCluster.setCondition(FireCluster.Condition.largeControllable);
        } else {
            fireCluster.setCondition(FireCluster.Condition.edgeControllable);
        }

        MrlPersonalData.VIEWER_DATA.setFireClusterCondition(world.getSelf().getID(), fireCluster);
    }

    private static double calcEffectiveWaterPerCycle(MrlFireBrigadeWorld world, Point targetPoint) {
        int waterQuantity = world.getMaxWater();
        int maxPower = world.getMaxPower();
        int refillRate = world.getWaterRefillRate();
        int waterQuantityPerRefillRate = waterQuantity / refillRate;
        double waterQuantityPerMaxPower = waterQuantity / maxPower;
        return maxPower * (waterQuantityPerMaxPower / (waterQuantityPerMaxPower + waterQuantityPerRefillRate + (Util.findDistanceToNearest(world, world.getRefuges(), targetPoint) / MRLConstants.MEAN_VELOCITY_OF_MOVING)));
    }

    public static int waterNeededToExtinguish(MrlBuilding building) {
        return WaterCoolingEstimator.getWaterNeeded(building.getSelfBuilding().getGroundArea(), building.getSelfBuilding().getFloors(),
                building.getSelfBuilding().getBuildingCode(), building.getEstimatedTemperature(), 20);
    }

    public static int calculateWaterPower(MrlFireBrigadeWorld world, MrlBuilding building) {
        return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), Math.min(world.getMaxPower(), Math.max(500, waterNeededToExtinguish(building))));
    }


    //selects some of top elements of the input list
    public SortedSet<Pair<EntityID, Double>> selectTop(int number, SortedSet<Pair<EntityID, Double>> inputList, Comparator<Pair<EntityID, Double>> comparator) {
        /*List outPut = new ArrayList();
        for (int i = 0; i < number; i++) {
            outPut.add(inputList.remove(0));
        }*/

        SortedSet<Pair<EntityID, Double>> outPut = new TreeSet<Pair<EntityID, Double>>(comparator);
        Pair<EntityID, Double> temp;
        for (int i = 0; i < number; i++) {
            if (!inputList.isEmpty()) {
                temp = inputList.first();
                inputList.remove(temp);
                outPut.add(temp);
            }
        }

        return outPut;
    }

    //This function looks in final list of buildings, if there is a building that seems to be unreachable, its value multiplies to 100, else its value multiplies to the estimated time to get there. Remember that we want to minimize the cost of extinguishing buildings.
    public SortedSet<Pair<EntityID, Double>> oldReRankBuildings(SortedSet<Pair<EntityID, Double>> finalBuildings) {
        MrlBuilding building;
        SortedSet<Pair<EntityID, Double>> rankedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        for (int i = 0; i < finalBuildings.size(); i++) {
            building = world.getMrlBuilding(finalBuildings.first().first());
            finalBuildings.remove(finalBuildings.first());
            if (pathPlanner.planMove((Area) world.getSelfPosition(), building.getSelfBuilding(), world.getMaxExtinguishDistance(), false).size() != 0) {
                building.BUILDING_VALUE *= pathPlanner.getPathCost() / (MRLConstants.MEAN_VELOCITY_OF_MOVING);
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            } else {
                building.BUILDING_VALUE = Integer.MAX_VALUE;

//                System.out.println("Agent " + world.getSelf().getID() + " found an unreachable fiery building: " + building.getID());
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            }
        }
        return rankedBuildings;
    }

    public SortedSet<Pair<EntityID, Double>> reRankBuildings(SortedSet<Pair<EntityID, Double>> finalBuildings) {
        MrlBuilding building;
        SortedSet<Pair<EntityID, Double>> rankedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        Set<MrlBuilding> buildingsInMyExtinguishRange = getBuildingsInMyExtinguishRange(world);
        boolean isInMyExtinguishDistance;

        int i = 0;
        int AStarCount = 10;
        if (world.isMapMedium()) AStarCount = 5;
        if (world.isMapHuge()) AStarCount = 3;
        for (Pair<EntityID, Double> next : finalBuildings) {
            building = world.getMrlBuilding(next.first());
            if (i >= AStarCount) {
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
                continue;
            }

            isInMyExtinguishDistance = buildingsInMyExtinguishRange.contains(building);
//            EntityID location = Util.getNearest(world, world.getAreaIDsInExtinguishRange(building.getID()), self.getID());
            EntityID location = Util.getNearest(world, building.getExtinguishableFromAreas(), world.getSelf().getID());

            if (isInMyExtinguishDistance || isReachable(world, pathPlanner, location)) {
                //building.BUILDING_VALUE *= pathPlanner.getPathCost() / (MRLConstants.MEAN_VELOCITY_OF_MOVING);
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            } else {
                building.BUILDING_VALUE *= 10;
//                building.BUILDING_VALUE *= 10000;
//                System.out.println("Agent " + world.getSelf().getID() + " found an unreachable fiery building: " + building.getID());
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            }
        }
        return rankedBuildings;
    }

    public SortedSet<Pair<EntityID, Double>> newReRankBuildings(SortedSet<Pair<EntityID, Double>> finalBuildings, List<MrlBuilding> directionBuildings) {
        MrlBuilding mrlBuilding;
        SortedSet<Pair<EntityID, Double>> rankedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        Map<MrlBuilding, Boolean> buildingsReachablity = calcBuildingsReachability(world, pathPlanner, directionBuildings);
        Set<MrlBuilding> buildingsInMyExtinguishRange = getBuildingsInMyExtinguishRange(world);
        for (Pair<EntityID, Double> next : finalBuildings) {
            mrlBuilding = world.getMrlBuilding(next.first());
            if (directionBuildings.contains(mrlBuilding)) {
                Boolean reachable = buildingsReachablity.get(mrlBuilding) || buildingsInMyExtinguishRange.contains(mrlBuilding);
                if (reachable) {
                    rankedBuildings.add(new Pair<EntityID, Double>(mrlBuilding.getID(), mrlBuilding.BUILDING_VALUE));
                } else {
                    mrlBuilding.BUILDING_VALUE *= 10000;
                    rankedBuildings.add(new Pair<EntityID, Double>(mrlBuilding.getID(), mrlBuilding.BUILDING_VALUE));
                }
            } else {
                rankedBuildings.add(new Pair<EntityID, Double>(mrlBuilding.getID(), mrlBuilding.BUILDING_VALUE));
            }
        }
        return rankedBuildings;
    }

    public static Map<MrlBuilding, Boolean> calcBuildingsReachability(MrlFireBrigadeWorld world, IPathPlanner pathPlanner, List<MrlBuilding> buildings) {
        Map<EntityID, List<MrlBuilding>> mutualExtinguishLocation = findMutualExtinguishLocation(buildings);
        Map<MrlBuilding, Boolean> buildingsReachability = new FastMap<MrlBuilding, Boolean>();

        for (EntityID next : mutualExtinguishLocation.keySet()) {
            boolean isReachable = isReachable(world, pathPlanner, next);
            for (MrlBuilding b : mutualExtinguishLocation.get(next)) {
                if (buildingsReachability.containsKey(b)) {
                    buildingsReachability.put(b, isReachable || buildingsReachability.get(b));
                } else {
                    buildingsReachability.put(b, isReachable);
                }
            }
        }
        return buildingsReachability;
    }

    private static boolean isReachable(MrlFireBrigadeWorld world, IPathPlanner pathPlanner, EntityID location) {
        int size = pathPlanner.planMove((Area) world.getSelfPosition(), (Area) world.getEntity(location), MRLConstants.IN_TARGET, false).size();
        double timeToArrive = pathPlanner.getPathCost() / MRLConstants.MEAN_VELOCITY_OF_MOVING;
        double normalizedDirectDistance = world.getDistance(world.getSelfPosition().getID(), location) / MRLConstants.MEAN_VELOCITY_OF_MOVING;
        return size != 0 && timeToArrive < 3 * normalizedDirectDistance/* && !world.getPlatoonAgent().getUnReachablePositions().contains(world.getEntity(location))*/;
    }

    public static Map<EntityID, List<MrlBuilding>> findMutualExtinguishLocation(List<MrlBuilding> fieryBuildings) {
        throw new NotImplementedException();
    }

    public static List<EntityID> findAreaIDsInExtinguishRange(MrlFireBrigadeWorld world, EntityID source) {
        List<EntityID> result = new ArrayList<EntityID>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Area && world.getDistance(next.getID(), source) < maxExtinguishDistance) {
                result.add(next.getID());
            }
        }
        return result;
    }

    public List<Area> getAreasInExtinguishRange(EntityID source) {
        List<Area> result = new ArrayList<Area>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Area && world.getDistance(next.getID(), source) <= maxExtinguishDistance) {
                result.add((Area) next);
            }
        }
        return result;
    }

    public static Set<MrlBuilding> getBuildingsInMyExtinguishRange(MrlFireBrigadeWorld world) {
        Set<MrlBuilding> result = new FastSet<MrlBuilding>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(world.getSelf().getID(), (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                MrlBuilding building = world.getMrlBuilding(next.getID());
                if (world.getDistance(next.getID(), world.getSelf().getID()) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }

    public static List<MrlBuilding> findBuildingsInExtinguishRangeOf(MrlFireBrigadeWorld world, EntityID source) {
        List<MrlBuilding> result = new ArrayList<MrlBuilding>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                MrlBuilding building = world.getMrlBuilding(next.getID());
                if (world.getDistance(next.getID(), source) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }

    public static void refreshFireEstimator(MrlWorld world) {
        for (StandardEntity entity : world.getBuildings()) {
            Building building = (Building) entity;
            int fieryness = building.isFierynessDefined() ? building.getFieryness() : 0;
            int temperature = building.isTemperatureDefined() ? building.getTemperature() : 0;
            MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());

           /* //age estimator mige khamoosh shode vali man ghablan didam fieryness 1 boode, hamoon estimator doroste
            if (building.isFierynessDefined()) {
                if (mrlBuilding.getEstimatedFieryness() > 4 && building.getFieryness() == 1) {
                    continue;
                }
            }*/

            //age estimator mige khamoosh sode yani khamoosh shode
            if (mrlBuilding.getEstimatedFieryness() > 4) {
                continue;
            }


            mrlBuilding.setEnergy(temperature * mrlBuilding.getCapacity());
            switch (fieryness) {
                case 0:
                    mrlBuilding.setFuel(mrlBuilding.getInitialFuel());
                    if (mrlBuilding.getEstimatedTemperature() >= mrlBuilding.getIgnitionPoint()) {
                        mrlBuilding.setEnergy(mrlBuilding.getIgnitionPoint() / 2);
                    }
                    break;
                case 1:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.75));
                    } else if (mrlBuilding.getFuel() == mrlBuilding.getInitialFuel()) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.90));
                    }
                    break;

                case 2:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.33
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.50));
                    }
                    break;

                case 3:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.01
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.33) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.15));
                    }
                    break;

                case 8:
                    mrlBuilding.setFuel(0);
                    break;
            }
        }
    }

    public void updateReadyFireBrigades() {
        for (FireBrigade fireBrigade : world.getFireBrigadeList()) {
            if (fireBrigade.getID().equals(world.getSelf().getID())) {      // This "fireBrigade" is actually "me"
                if (world.getSelfPosition() instanceof Building) {
                    if (world.getSelfHuman().getBuriedness() == 0            // Means that I'm not buried!
                            && world.getTime() >= 2) {                          // exactly at the second cycle, ambulance Team agents might get buried.
                        readyFireBrigades.add(fireBrigade);
                    } else {
                        // Too soon to decide if I will be buried (at time:2)
                    }
                } else { // I am on the road!
                    readyFireBrigades.add(fireBrigade);
                }
            } else { // This "fireBrigade" is someone else (not me)
                if (humanHelper.getAgentState(fireBrigade.getID()) == null) { // I have no idea in what state this fireBrigade is.
                    //TODO @BrainX Is there someway better to access others' states?
                    if (fireBrigade.isBuriednessDefined() && fireBrigade.getBuriedness() == 0) { // I have information about this fireBrigade's buriedness and it's not buried.
                        readyFireBrigades.add(fireBrigade);
                    } else if (fireBrigade.getPosition(world) instanceof Road) { // If I don't know if this fireBrigade is buried, I consider it healthy if it's on the road.
                        readyFireBrigades.add(fireBrigade);
                    }
                } else { // I know this fireBrigade's state
                    if (humanHelper.isAgentStateHealthy(fireBrigade.getID())) {
                        readyFireBrigades.add(fireBrigade);
                    }
                }
            }
        }

    }

    public Set<StandardEntity> getReadyFireBrigades() {
        return readyFireBrigades;
    }

    public MrlBuilding findSmallestBuilding(List<MrlBuilding> buildings) {
        int minArea = Integer.MAX_VALUE;
        MrlBuilding smallestBuilding = null;
        for (MrlBuilding building : buildings) {
            if (building.getSelfBuilding().getTotalArea() < minArea) {
                minArea = building.getSelfBuilding().getTotalArea();
                smallestBuilding = building;
            }
        }
        return smallestBuilding;

    }

    public MrlBuilding findNewestIgnitedBuilding(List<MrlBuilding> buildings) {
        int minTime = Integer.MAX_VALUE;
        int tempTime;
        MrlBuilding smallestBuilding = null;
        for (MrlBuilding building : buildings) {
            tempTime = world.getTime() - building.getIgnitionTime();
            if (tempTime < minTime) {
                minTime = tempTime;
                smallestBuilding = building;
            }
        }
        return smallestBuilding;

    }
}
