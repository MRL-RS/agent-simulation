package mrl.ambulance.targetSelector;

import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 2/21/13
 *         Time: 1:49 PM
 */

/**
 * This implementation uses different distance parameters (such as distance form refuge to target, distance from agent to target and distance from partitions to target)to select best target and uses possible filters such as dead time filters
 */
public class DistanceBasedTargetSelector extends TargetSelector {

    //Map of previously found victimsID to its AmbulanceTarget object
    private Map<EntityID, AmbulanceTarget> targetsMap;
    private double threshold = Double.MAX_VALUE;// A threshold for selecting victim
    private int rescueRange;

    public DistanceBasedTargetSelector(MrlWorld world, AmbulanceConditionChecker conditionChecker, AmbulanceUtilities ambulanceUtilities, Partition myPartition) {
        super(world, conditionChecker, ambulanceUtilities);
        targetsMap = new FastMap<EntityID, AmbulanceTarget>();
        initializeRescueRange();
        setWorkingPartition();
        setWorkingPartitions();
    }


    private void initializeRescueRange() {
        if (world.isMapSmall()) {
            rescueRange = world.getViewDistance() * 2;
        } else if (world.isMapMedium()) {
            rescueRange = world.getViewDistance() * 4;
        } else { //if map is huge
            rescueRange = world.getViewDistance() * 6;
        }
        MrlPersonalData.VIEWER_DATA.setRescueRange(rescueRange);
    }

    /**
     * Finds best target between specified possible targetsMap
     *
     * @param victims targetsMap to search between them
     * @return best target to select
     */
    @Override
    public StandardEntity nextTarget(Set<StandardEntity> victims) {


        //TODO @Pooya: Manage unreachable victims
        //TODO @Pooya: Review good humans determination
        //TODO @Pooya: Review value determination for victims
        targetsMap.clear();

        if (previousTarget != null && !victims.contains(world.getEntity(previousTarget.getVictimID()))) {
            previousTarget = null;
        }

        setWorkingPartition();

        setWorkingPartitions();

        refreshTargetsMap(victims, targetsMap);

        calculateDecisionParameters(victims, targetsMap);

        calculateVictimsCostValue();


        AmbulanceTarget bestTarget = null;
        bestTarget = findBestVictim(targetsMap);

        //considering inertia for the current target to prevent loop in target selection
        if (previousTarget != null && victims.contains(world.getEntity(previousTarget.getVictimID()))) {
            if (bestTarget != null && !bestTarget.getVictimID().equals(previousTarget.getVictimID())) {
                Human bestHuman = (Human) world.getEntity(bestTarget.getVictimID());
                Human previousHuman = (Human) world.getEntity(previousTarget.getVictimID());

                pathPlanner.planMove((Area) world.getSelfPosition(), (Area) world.getEntity(bestHuman.getPosition()), 0, false);
                int bestHumanCost = pathPlanner.getPathCost();
                pathPlanner.planMove((Area) world.getSelfPosition(), (Area) world.getEntity(previousHuman.getPosition()), 0, false);
                int previousHumanCost = pathPlanner.getPathCost();
                if (previousHumanCost < bestHumanCost) {
                    bestTarget = previousTarget;
                }
            }
        }

        previousTarget = bestTarget;

        if (bestTarget != null) {
            return world.getEntity(bestTarget.getVictimID());
        } else {
            return null;
        }

    }

    private void refreshTargetsMap(Set<StandardEntity> victims, Map<EntityID, AmbulanceTarget> targetsMap) {
        List<EntityID> toRemoves = new ArrayList<EntityID>();
        for (EntityID targetID : targetsMap.keySet()) {
            if (!victims.contains(world.getEntity(targetID))) {
                toRemoves.add(targetID);
            }
        }
        for (EntityID entityID : toRemoves) {
            targetsMap.remove(entityID);
        }
    }

    private AmbulanceTarget findBestVictim(Map<EntityID, AmbulanceTarget> targetsMap) {
        AmbulanceTarget bestTarget = null;
        if (targetsMap != null && !targetsMap.isEmpty()) {

            double minValue = Double.MAX_VALUE;
            for (AmbulanceTarget target : targetsMap.values()) {
                if (target.getDistanceToMe() <= rescueRange || myBasePartition.contains(target.getPositionID(), world) || Util.distance(myBasePartition.getPolygon(), world.getEntity(target.getPositionID()).getLocation(world)) < rescueRange) {
//                    if (target.getCost() < minValue && target.getCost() < threshold) {
                    if (target.getDistanceToMe() < minValue) {
//                        minValue = target.getCost();
                        minValue = target.getDistanceToMe();
                        bestTarget = target;
                    }
                }
            }

        }

        return bestTarget;
    }

    private void calculateVictimsCostValue() {

        if (targetsMap != null && !targetsMap.isEmpty()) {
            double cost = 0;
            double rw = .9;//refuge Weight
            double pdw = 2.7;//my Partition Distance Weight
            double mdw = 1.5;//My Distance Weight
            double vsw = 1.5; //Victim Situation Weight

            for (AmbulanceTarget target : targetsMap.values()) {
                cost = 1 + rw * target.getDistanceToRefuge() / MRLConstants.MEAN_VELOCITY_OF_MOVING
                        + pdw * target.getDistanceToPartition() / MRLConstants.MEAN_VELOCITY_OF_MOVING
                        + mdw * target.getDistanceToMe() / MRLConstants.MEAN_VELOCITY_OF_MOVING
                        - vsw * target.getVictimSituation();
                target.setCost(cost);
            }

        }

    }

    /**
     * Calculates different parameters for a target such as distance of the target to nearest refuge, distance of target to
     * this agent partitions, distance of this agent to the target and situations of the target based on its BRD and DMG
     *
     * @param victims    victims to calculate their parameters
     * @param targetsMap map of previously found targets
     */
    private void calculateDecisionParameters(Set<StandardEntity> victims, Map<EntityID, AmbulanceTarget> targetsMap) {

        AmbulanceTarget target;
        Human human;
        if (victims != null && !victims.isEmpty()) {
            for (StandardEntity victim : victims) {
                target = targetsMap.get(victim.getID());
                human = (Human) world.getEntity(victim.getID());
                if (target == null) {
                    //creating a new AmbulanceTarget object
                    target = new AmbulanceTarget(victim.getID());

                    //set target position
                    target.setPositionID(human.getPosition());

                    //euclidean distance from this victim to the nearest refuge
                    target.setDistanceToRefuge(world.getDistance(human.getPosition(), world.findNearestRefuge(human.getPosition())));

                    //euclidean distance from this victim to the center of my partitions
                    if (myBasePartition == null) {
                        System.out.println("My base partition is null");
                    }
                    if (myBasePartition.getCenter() == null) {
                        System.out.println("My partition has no center");
                    }
                    target.setDistanceToPartition(Util.distance(human.getLocation(world), myBasePartition.getCenter()));
                }
                //euclidean distance from this victim to the me
                target.setDistanceToMe(computingDistance(human));

                target.setVictimSituation(calculateVictimProfitability(human));

                targetsMap.put(victim.getID(), target);
            }
        }

    }

    /**
     * This method computes distance based on euclidean distance.
     * <br>
     * <br>
     * <b>Note:</b> Based on human importance, the distance may be changed to lower value
     *
     * @param human the human to calculate its distance to me.
     * @return euclidean distance from human to me
     */
    private int computingDistance(Human human) {

        double coefficient = 1;
        if (human instanceof AmbulanceTeam) {
            coefficient = 0.90;
        } else if (human instanceof PoliceForce) {
            coefficient = 0.95;
        } else if (human instanceof FireBrigade) {
            coefficient = 0.97;
        } else {//human is instance of Civilian
            coefficient = 1;
        }


        return (int) (coefficient * Util.distance(human.getPosition(world).getLocation(world), world.getSelfLocation()));
    }

    /**
     * calculates victim profitability
     *
     * @param human target human (kossher)
     * @return
     */
    private double calculateVictimProfitability(Human human) {

        int ttd = (int) Math.ceil(human.getHP() / (double) human.getDamage() * 0.8); //a pessimistic time to death

        double profitability = 100 / (double) ((human.getBuriedness() * ttd) + 1);

        return profitability;
    }

}
