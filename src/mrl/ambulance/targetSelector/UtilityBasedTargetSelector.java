package mrl.ambulance.targetSelector;

import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.common.comparator.ConstantComparators;
import mrl.helper.PropertyHelper;
import mrl.mrlPersonal.viewer.layers.MrlHumanLayer;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.GaussianGenerator;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Pooya Deldar Gohardani
 * Date: 2/21/13
 * Time: 1:49 PM
 */

/**
 * This implementation uses different distance parameters (such as distance form refuge to target, distance from agent to target and distance from partitions to target)to select best target and uses possible filters such as dead time filters
 */
public class UtilityBasedTargetSelector extends TargetSelector {

    //Map of previously found victimsID to its AmbulanceTarget object
    private Map<EntityID, AmbulanceTarget> targetsMap;
    //    private double threshold = Double.MAX_VALUE;// A threshold for selecting victim
    private int rescueRange;
    PropertyHelper propertyHelper;
    private NumberGenerator<Double> noise;
    private double rescueRangeCoef = 1;
    private final int secondRescueRangeIncrementTime = 90;


    public UtilityBasedTargetSelector(MrlWorld world, AmbulanceConditionChecker conditionChecker, AmbulanceUtilities ambulanceUtilities, Partition myPartition) {
        super(world, conditionChecker, ambulanceUtilities);
        targetsMap = new FastMap<>();
        initializeRescueRange();
        setWorkingPartition();
        setWorkingPartitions();
        propertyHelper = world.getHelper(PropertyHelper.class);
        double mean = 0.1;
        double sd = 0.01;
        this.noise = new GaussianGenerator(mean, sd, world.getPlatoonAgent().getRandom());
    }


    private void initializeRescueRange() {
        if (world.isMapSmall()) {
            rescueRange = world.getViewDistance() * 2;
        } else if (world.isMapMedium()) {
            rescueRange = world.getViewDistance() * 4;
        } else { //if map is huge
            rescueRange = world.getViewDistance() * 6;
        }

        rescueRange *= rescueRangeCoef;

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
//        if (world.getTime() == secondRescueRangeRecalculationTime || world.getTime() == secondRescueRangeRecalculationTime + 1) {
//        }

        //TODO @Pooya: Manage unreachable victims
        //TODO @Pooya: Review good humans determination
        //TODO @Pooya: Review value determination for victims
        targetsMap.clear();

        if (previousTarget != null) {
            targetsMap.put(previousTarget.getVictimID(), previousTarget);
        }

        if (previousTarget != null && !victims.contains(world.getEntity(previousTarget.getVictimID()))) {
            previousTarget = null;
        }

        setWorkingPartition();

        setWorkingPartitions();

        refreshTargetsMap(victims, targetsMap);

        calculateDecisionParameters(victims, targetsMap);

        calculateVictimsValue();


        AmbulanceTarget bestTarget = null;
        bestTarget = findBestVictim(victims, targetsMap);

        //considering inertia for the current target to prevent loop in target selection
//        if (previousTarget != null && victims.contains(world.getEntity(previousTarget.getVictimID()))) {
//            if (bestTarget != null && !bestTarget.getVictimID().equals(previousTarget.getVictimID())) {
//                Human bestHuman = (Human) world.getEntity(bestTarget.getVictimID());
//                Human previousHuman = (Human) world.getEntity(previousTarget.getVictimID());
//
//                List<EntityID> bestTargetPlanMove = new ArrayList<>(pathPlanner.planMove((Area) world.getSelfPosition(), (Area) world.getEntity(bestHuman.getPosition()), 0, false));
//                int bestHumanCost = pathPlanner.getPathCost();
//                if (bestTargetPlanMove.isEmpty()) {
//                    bestHumanCost = Integer.MAX_VALUE;
//                }
//                pathPlanner.planMove((Area) world.getSelfPosition(), (Area) world.getEntity(previousHuman.getPosition()), 0, false);
//                int previousHumanCost = pathPlanner.getPathCost();
//                if (/*previousHumanCost < bestHumanCost || */previousTarget.getValue() > 10) {
//                    bestTarget = previousTarget;
//                }
//            }
//        }

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

    private AmbulanceTarget findBestVictim(Set<StandardEntity> victims, Map<EntityID, AmbulanceTarget> targetsMap) {
        AmbulanceTarget bestTarget = null;
        if (targetsMap != null && !targetsMap.isEmpty()) {
            double threshold = 10;
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            MrlHumanLayer.AMBUULANCE_TARGET_MAP.remove(world.getSelf().getID());
            MrlHumanLayer.AMBUULANCE_TARGET_MAP.put(world.getSelf().getID(), new HashMap<>());

            for (AmbulanceTarget target : targetsMap.values()) {
//                if (target.getDistanceToMe() <= rescueRange || myBasePartition.contains(target.getPositionID(), world) || Util.distance(myBasePartition.getPolygon(), world.getEntity(target.getPositionID()).getLocation(world)) < rescueRange) {


                MrlHumanLayer.AMBUULANCE_TARGET_MAP.get(world.getSelf().getID()).put(target.getVictimID(), target);

                if (!shouldRescue(target)) {
                    continue;
                }


                if (target.getValue() > threshold && target.getValue() - threshold < minValue) {
                    minValue = target.getValue() - threshold;
                    bestTarget = target;
                }
//                }
            }

            if (bestTarget == null) {
                if (world.getTime() > secondRescueRangeIncrementTime) {
                    rescueRangeCoef = 2.0;
                    initializeRescueRange();
                }
                for (AmbulanceTarget target : targetsMap.values()) {
//                    if (target.getDistanceToMe() <= rescueRange || myBasePartition.contains(target.getPositionID(), world) || Util.distance(myBasePartition.getPolygon(), world.getEntity(target.getPositionID()).getLocation(world)) < rescueRange) {
                    if (!shouldRescue(target)) {
                        if (!shouldRescue(target)) {
                            continue;
                        }
                        if (Math.abs(target.getValue() - threshold) < minValue) {
                            minValue = target.getValue() - threshold;
                            bestTarget = target;
                        }

//                    }
                    }
                }

            }

            if (previousTarget != null && previousTarget.getValue() > 10 && victims.contains(world.getEntity(previousTarget.getVictimID())) && shouldRescue(previousTarget)) {
                bestTarget = previousTarget;
            }


        }
        return bestTarget;
    }

    private boolean shouldRescue(AmbulanceTarget target) {
        if (target.getNumberOfNeededAmulances() <= 0) {
            return false;
        }
        List<EntityID> rescuingAmbulances = new ArrayList<>(target.getRescuingAmbulances());
        if (target.getNumberOfNeededAmulances() <= rescuingAmbulances.size()) {
            Partition humanPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
            if (humanPartition.containsInEntities(target.getPositionID())) {
                return true;
            } else {
//                EntityID domesticAT = null;
//                for (EntityID id : rescuingAmbulances) {
//                    if (world.getPartitionManager().findHumanPartition(world.getEntity(id, Human.class)).containsInEntities(target.getPositionID())) {
//                        domesticAT = id;
//                        break;
//                    }
//                }
//                rescuingAmbulances.remove(domesticAT);
//                if (target.getNumberOfNeededAmulances() <= rescuingAmbulances.size()) {
                Collections.sort(rescuingAmbulances, ConstantComparators.EntityID_COMPARATOR);
                if (!rescuingAmbulances.subList(0, target.getNumberOfNeededAmulances()).contains(world.getSelfHuman().getID())) {
                    return false;
                }
//                }
            }
        }
        return true;
    }


    /**
     * This method is to find each victim time to rescue which consists of time to death, time to refuge
     * and his buriedness
     */
    private void calculateVictimsValue() {

        if (targetsMap != null && !targetsMap.isEmpty()) {
            double value = 0;
//            double rw = .9;//refuge Weight
//            double pdw = 2.7;//my Partition Distance Weight
//            double mdw = 1.5;//My Distance Weight
//            double vsw = 1.5; //Victim Situation Weight
            double timeToArrive;
            double timeToRefuge;
            int buriedness;
            List<EntityID> rescuingAmbulances;
            for (AmbulanceTarget target : targetsMap.values()) {
                rescuingAmbulances = new ArrayList<>(findRescuingAmbulances(world.getEntity(target.getVictimID(), Human.class)));//Plus one because current agent will join to others.
                timeToArrive = Math.ceil(target.getDistanceToMe() / MRLConstants.MEAN_VELOCITY_OF_MOVING);
                timeToRefuge = Math.ceil(target.getDistanceToRefuge() / MRLConstants.MEAN_VELOCITY_OF_MOVING);
                buriedness = ((Human) world.getEntity(target.getVictimID())).getBuriedness();
                value = target.getTimeToDeath() - (timeToArrive + timeToRefuge + 2) - buriedness / (rescuingAmbulances.size() + 1);
                target.setValue(value);
                target.setRescuingAmbulances(rescuingAmbulances);
                int numberOfNeededAmbulances = findNumberOfNeededAmbulances(target);
                Human human;

                for (StandardEntity h : world.getHumans()) {
                    human = (Human) h;
                    if (human.getPosition().equals(target.getPositionID())
                            && !human.getID().equals(world.getSelfHuman().getID())
                            && !human.getID().equals(target.getVictimID())
                            && human.isBuriednessDefined() && human.getBuriedness() > 0
                            && human.isHPDefined() && human.getHP() > 0
                            ) {
                        numberOfNeededAmbulances++;
                    }
                }
                ++numberOfNeededAmbulances;//An addition AT needed for rescuing human for sure.
                target.setNumberOfNeededAmulances(numberOfNeededAmbulances);
            }

        }
    }


    public int findNumberOfNeededAmbulances(AmbulanceTarget target) {
        double timeToArrive = (int) Math.ceil(target.getDistanceToMe() / MRLConstants.MEAN_VELOCITY_OF_MOVING);
        double timeToRefuge = (int) Math.ceil(target.getDistanceToRefuge() / MRLConstants.MEAN_VELOCITY_OF_MOVING);
        Human human = world.getEntity(target.getVictimID(), Human.class);
        int numberOfNeeded = (int) Math.ceil((double) human.getBuriedness() / (target.getTimeToDeath() - (timeToArrive + timeToRefuge + 2) - 10));

        return numberOfNeeded;
    }

    /**
     * Retrieve number of agents who rescuing a human. This function just works with agents and human position.
     *
     * @param rescuedHuman Victim human who we want to find Ambulances rescuing it.
     * @return Number of ambulance teams currently rescuing {@code rescuedHuman}
     */
    private List<EntityID> findRescuingAmbulances(Human rescuedHuman) {
        List<EntityID> rescuingAmbulances = new ArrayList<>();
        int count = 0;
        for (AmbulanceTeam at : world.getAmbulanceTeamList()) {
            if (!at.isBuriednessDefined() || at.getBuriedness() > 0 || at.getHP() == 0) {
                continue;
            }
            if (at.getPosition().equals(rescuedHuman.getPosition())) {
                rescuingAmbulances.add(at.getID());
            }
        }
        return rescuingAmbulances;
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
                human = (Human) world.getEntity(victim.getID());
                int distanceToMe = computingDistance(human);

                if (distanceToMe <= rescueRange) {
//                    System.out.println("Rescue Range is: " + rescueRange);

                    target = targetsMap.get(victim.getID());
                    if (target == null) {
                        //creating a new AmbulanceTarget object
                        target = new AmbulanceTarget(victim.getID());
                    }
                    //set target position
                    target.setPositionID(human.getPosition());

                    if (target.getDistanceToRefuge() == Integer.MAX_VALUE) {
                        //euclidean distance from this victim to the nearest refuge
                        target.setDistanceToRefuge(world.getDistance(human.getPosition(), world.findNearestRefuge(human.getPosition())));
                    }

                    //euclidean distance from this victim to the center of my partitions
                    if (myBasePartition == null) {
                        System.out.println("My base partition is null");
                    }
                    if (myBasePartition.getCenter() == null) {
                        System.out.println("My partition has no center");
                    }
                    target.setDistanceToPartition(Util.distance(human.getLocation(world), myBasePartition.getCenter()));

//                    Integer damagePropertyTime = propertyHelper.getPropertyTime(human.getDamageProperty());
                    Pair<Double, Double> damageAndHP = estimateDamageAndHP(human);
                    target.setEstimatedHP((int) (double) damageAndHP.second());
                    target.setEstimatedDamage((int) (double) damageAndHP.first());
                    int ttd = (int) Math.ceil((damageAndHP.second() / damageAndHP.first()) * 0.8); //a pessimistic time to death
                    target.setTimeToDeath(ttd);
                    //euclidean distance from this victim to the me
                    target.setDistanceToMe(distanceToMe);

//                target.setVictimSituation(calculateVictimProfitability(human));
                    target.setRescuingAmbulances(findRescuingAmbulances(human));

                    targetsMap.put(victim.getID(), target);
                }
            }
        }

    }

    /**
     * Estimate HP and Damage of a human that sensed before.
     *
     * @param human
     * @return first -> damage & second -> HP
     */
    public Pair<Double, Double> estimateDamageAndHP(Human human) {
        propertyHelper = world.getHelper(PropertyHelper.class);
        Integer propertyTime = propertyHelper.getPropertyTime(human.getDamageProperty());

//        world.printData(" human: " + human.getID() + " time:" + propertyTime);

        double estimatedDamage = human.getDamage();
        double estimatedHP = human.getHP();

        double n = this.noise.nextValue();

        int timeElapsed = world.getTime() - propertyTime;

//        for (int i = 0; i < timeElapsed; i++) {
//            estimatedDamage = estimatedDamage + (0.00025 * estimatedDamage * estimatedDamage) + 0.01 + n;
////            estimatedDamage = estimatedDamage + (0.00025 * estimatedDamage * estimatedDamage) + 0.01 ;
//            estimatedHP -= estimatedDamage;
//        }
        double mahdiValue = 0.3008695652173913; //Line slope.

        estimatedDamage = timeElapsed * mahdiValue + human.getDamage();
        estimatedHP -= estimatedDamage * timeElapsed;

//        world.printData(human + "\t" + human.getDamage() + "\t" + estimatedDamage);

        return new Pair<>(estimatedDamage, estimatedHP);


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
