/*
package mrl.firebrigade.targetSelection;

import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.common.clustering.Cluster;
import mrl.common.clustering.FireCluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.mrlPersonal.viewer.layers.MrlBuildingValuesLayer;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

*/
/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 6:37 PM
 * Author: Mostafa Movahedi
 *//*



*/
/**
 *  1- Initial direction determined based on farthest point of map from center of nearest cluster.<br/>
 *  2- FBs separates in two parts and try to put off fire cluster in two direction.<br/>
 *//*

public class ZJUBasedTargetSelector extends DefaultFireBrigadeTargetSelector {
    private static final double INITIAL_COST = 200;

    private static final double AGENT_SPEED = 40000;
    private static final double BASE_PER_MOVE_COST = 10;
    private static final double SHOULD_MOVE_COST = BASE_PER_MOVE_COST * 2.2;
    private static final double MAX_DISTANCE_COST = BASE_PER_MOVE_COST * 10;
    private static final double NOT_IN_CHANGESET_COST = BASE_PER_MOVE_COST * 1.2;

    private static final int RECENT_UPDATE_TIME_MAX = 4;

    private static final double MAX_FUEL_VALUE_FOR_IGNITION_BUILDING = 4000000;
    private static final double IGNITION_BUILDING_MAX_COST = 25;
    private static final double IGNITION_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_IGNITION_BUILDING
            / IGNITION_BUILDING_MAX_COST;
    private static final double IGNITION_BUILDING_MIN_COST = 6;

    private static final double MAX_FUEL_VALUE_FOR_ESTI_IGNITION_BUILDING = 4000000;
    private static final double ESTI_IGNITION_BUILDING_MAX_COST = 25;
    private static final double ESTI_IGNITION_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_ESTI_IGNITION_BUILDING
            / ESTI_IGNITION_BUILDING_MAX_COST;
    private static final double ESTI_IGNITION_BUILDING_MIN_AWARD = 6;

    private static final double MAX_FUEL_VALUE_FOR_HEATING_BUILDING = 4000000;
    private static final double RECENT_UPDATED_HEATING_BUILDING_MAX_COST = 78;
    private static final double RECENT_UPDATED_HEATING_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_HEATING_BUILDING
            / RECENT_UPDATED_HEATING_BUILDING_MAX_COST;
    private static final double DEFAULT_HEATING_BUILDING_MAX_COST = 65;
    private static final double DEFAULT_HEATING_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_HEATING_BUILDING
            / DEFAULT_HEATING_BUILDING_MAX_COST;
    private static final double HEATING_BUILDING_MIN_COST = 20;

    private static final double MAX_FUEL_VALUE_FOR_BURNING_BUILDING = 4000000;
    private static final double BURNING_BUILDING_MAX_COST = 80;
    private static final double BURNING_BUILDING_MIN_COST = 36;
    private static final double BURNING_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_BURNING_BUILDING
            * 2 / (BURNING_BUILDING_MAX_COST + BURNING_BUILDING_MIN_COST);

    private static final double MAX_FUEL_VALUE_FOR_INFERNO_BUILDING = 4000000;
    private static final double INFERNO_BUILDING_MAX_COST = 60;
    private static final double INFERNO_BUILDING_MIN_COST = 25;
    private static final double INFERNO_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_INFERNO_BUILDING
            * 2 / (INFERNO_BUILDING_MAX_COST + INFERNO_BUILDING_MIN_COST);

    private static final double MAX_FUEL_LEFT_VALUE_FOR_AWARD = 4000000;
    private static final double FUEL_LEFT_MAX_AWARD = 40;
    private static final double FUEL_LEFT_COEFFICIENT = FUEL_LEFT_MAX_AWARD
            / MAX_FUEL_LEFT_VALUE_FOR_AWARD;

    private static final float MAX_TEMPERATURE = 400;
    private static final float TEMPERATURE_COEFFICIENT = MAX_TEMPERATURE / 32;
    private static final int lOW_TEMPERATURE = 180;
    private static final double LOW_TEMPERATURE_AWARD = 15;

    private static final double IN_SIGHT_HEATING_BUILDING_AWARD = 25;
    private static final double JUST_NOTICED_HEATING_BUILDING_AWARD = 18;

    private static final double CONFIRM_COST = 22;
    private static final int HIT_RATE_COEFFICIENT = 18;


    public ZJUBasedTargetSelector(MrlFireBrigadeWorld world) {
        super(world);
        distanceNormalizer = MRLConstants.MEAN_VELOCITY_OF_MOVING;
    }

    private double distanceNormalizer;


    @Override
    public FireBrigadeTarget selectTarget() {
        FireBrigadeTarget fireBrigadeTarget = null;
        Cluster targetCluster = world.getFireClusterManager().findNearestCluster((world.getSelfLocation()));

        if (targetCluster != null) {
            SortedSet<Pair<EntityID, Double>> sortedBuildings;
            sortedBuildings = calculateValue((FireCluster) targetCluster);
            sortedBuildings = fireBrigadeUtilities.reRankBuildings(sortedBuildings);
            if (MRLConstants.FILL_VIEWER_DATA) {
                MrlBuildingValuesLayer.BUILDING_VALUES.put(world.getSelf().getID(), world.getMrlBuildings());
            }
            if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
                lastTarget = target;
                target = world.getMrlBuilding(sortedBuildings.first().first());
                fireBrigadeTarget = new FireBrigadeTarget(target, targetCluster);
            }
        }

        return fireBrigadeTarget;

    }


    private SortedSet<Pair<EntityID, Double>> calculateValue(FireCluster fireCluster) {

        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();

        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);

        List<MrlBuilding> inDirectionBuildings;
//        Point targetPoint = directionManager.findBestValueTarget(fireCluster, world.getCivilianClusterManager().getClusters());
        //TODO @Mostafa:Consider other fire clusters to find the farthest point
        //TODO @Mostafa&Pooya: rewrite the whole method
        Point targetPoint = directionManager.findFarthestPointOfMap(fireCluster);

        //TODO @Mostafa&Pooya: rewrite the whole method; you can use enums to for it
        inDirectionBuildings = fireCluster.findBuildingsInDirectionOf(targetPoint, true, false);

//        if (inDirectionBuildings.isEmpty()) {
            calculateValueForOtherBuildings(sortedBuildings, borderEntities, targetPoint, distanceNormalizer);
//        }
        calculateValueForInDirectionBuildings(sortedBuildings, inDirectionBuildings, targetPoint, distanceNormalizer);
        return sortedBuildings;
    }

    private void calculateValueForOtherBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, Set<StandardEntity> otherBuildings, Point targetPoint, double distanceNormalizer) {
        for (StandardEntity entity : otherBuildings) {
            MrlBuilding b = world.getMrlBuilding(entity.getID());
//            double normalizedDistance = world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / distanceNormalizer;
//            double normalizedDistance = Util.distance(targetPoint,b.getSelfBuilding().getLocation(world)) / distanceNormalizer;
            switch (b.getEstimatedFieryness()) {
                case 1:
                    b.BUILDING_VALUE = 50;
                    break;
                case 2:
                    b.BUILDING_VALUE = 110;
                    break;
                case 3:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 210 : 410 - b.getEstimatedTemperature();
                    break;
                case 0:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 190 : 210;
                    break;
                case 4:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 160 : 190;
                    break;
                case 5:
                case 6:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 200 : 240;
                    break;
                case 7:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 300 : 380;
                    break;
                default:
                case 8:         //Burnt Building
                    b.BUILDING_VALUE = 19000;
                    break;
            }
//            b.BUILDING_VALUE += normalizedDistance;
            b.BUILDING_VALUE += b.getAdvantageRatio();
            if (lastTarget != null) {
                b.BUILDING_VALUE += world.getDistance(lastTarget.getID(), b.getID()) / MRLConstants.MEAN_VELOCITY_OF_MOVING;
            }
            b.BUILDING_VALUE += 1000;
            if (lastTarget != null && b.getID().equals(lastTarget.getID())) {
                b.BUILDING_VALUE *= 0.7;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }


    private void calculateValueForInDirectionBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<MrlBuilding> highValueBuildings, Point targetPoint, double distanceNormalizer) {
        for (MrlBuilding b : highValueBuildings) {
//            double normalizedDistance = world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / distanceNormalizer;
//            double normalizedDistance = Util.distance(targetPoint,b.getSelfBuilding().getLocation(world)) / distanceNormalizer;
            switch (b.getEstimatedFieryness()) {
                case 1:
                    b.BUILDING_VALUE = 1;
                    break;
                case 2:
                    b.BUILDING_VALUE = 25;
                    break;
                case 3:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 75 : 275 - b.getEstimatedTemperature();
                    break;
                case 0:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 130 : 160;
                    break;
                case 4:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 40 : 80;
                    break;
                case 5:
                case 6:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 75 : 95;
                    break;
                case 7:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 120 : 170;
                    break;
                case 8:   //Burnt building
                default:
                    b.BUILDING_VALUE = 15000;
                    break;
            }
//            b.BUILDING_VALUE += (normalizedDistance);
            //b.BUILDING_VALUE += b.getAdvantageRatio();
            if (lastTarget != null) {
                b.BUILDING_VALUE += world.getDistance(lastTarget.getID(), b.getID()) / MRLConstants.MEAN_VELOCITY_OF_MOVING;
            }
            if (lastTarget != null && b.getID().equals(lastTarget.getID())) {
                b.BUILDING_VALUE *= 0.7;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }

    public int cost(MrlBuilding fireBuilding) {
        double cost = INITIAL_COST;

        double worldFireBuildingSituation = fsc.getWorldFireBuildingSituation();
        double coefficient = worldFireBuildingSituation > 1 ? worldFireBuildingSituation * 0.5 + 0.5 : worldFireBuildingSituation;
        double perMoveCost = BASE_PER_MOVE_COST * coefficient;
        double shouldMoveCost = SHOULD_MOVE_COST * coefficient;
        double maxDistanceCost = MAX_DISTANCE_COST * coefficient;
        double notInChangeSetCost = NOT_IN_CHANGESET_COST * coefficient;
        Building building = fireBuilding.getSelfBuilding();

        BuildingCondition condition = fireBuilding.getCondition();

        // distance and should move
        double distance = Util.distance(selfHuman.getLocation(world),building.getLocation(world));
        if (distance > world.getMaxExtinguishDistance()) {
            double timeToMove = (distance - world.getMaxExtinguishDistance()) / AGENT_SPEED;
            double distanceCost = 0;
            if (timeToMove <= 0.5) {
                distanceCost = timeToMove * perMoveCost * 0.4;
            } else if (timeToMove <= 2) {
                distanceCost = timeToMove * perMoveCost * 0.6;
            } else if (timeToMove <= 4) {
                distanceCost = timeToMove * perMoveCost * 0.8;
            } else {
                distanceCost = timeToMove * perMoveCost * 1.0;
            }
            if (distanceCost > MAX_DISTANCE_COST) {
                distanceCost = MAX_DISTANCE_COST;
            }
            cost += distanceCost + shouldMoveCost;
        }

        // currentEstiFieryness
        int currentEstiFieryness = fireBuilding.getEstimatedFieryness();
        float currentEstiFuel = fireBuilding.getFuel();
        float initFuel = fireBuilding.getInitialFuel();
        // ignition
        if (currentEstiFieryness == StandardEntityConstants.Fieryness.HEATING.ordinal()
                && world.getTime() - fireBuilding.getIgnitionTime() <= RECENT_UPDATE_TIME_MAX) {
            double award = Math.min(IGNITION_BUILDING_MAX_COST, currentEstiFuel
                    / IGNITION_BUILDING_FUEL_COEFFICIENT);
            cost -= Math.max(award, IGNITION_BUILDING_MIN_COST);
        } else if (fireBuilding.getEstiIgnitionTime() != -1
                && world.getTime() - fireBuilding.getEstiIgnitionTime() <= RECENT_UPDATE_TIME_MAX) {
            double award = Math.min(ESTI_IGNITION_BUILDING_MAX_COST, currentEstiFuel
                    / ESTI_IGNITION_BUILDING_FUEL_COEFFICIENT);
            cost -= Math.max(award, ESTI_IGNITION_BUILDING_MIN_AWARD);
        }

        // fuel
        boolean isRecentlyUpdated = (world.getTime() - condition.getTemperature() <= RECENT_UPDATE_TIME_MAX);
        if (currentEstiFieryness == StandardEntityConstants.Fieryness.HEATING.ordinal()) {
            double award = 0;
            if (isRecentlyUpdated) {
                award = Math.min(RECENT_UPDATED_HEATING_BUILDING_MAX_COST, currentEstiFuel
                        / RECENT_UPDATED_HEATING_BUILDING_FUEL_COEFFICIENT);
            } else {
                award = Math.min(DEFAULT_HEATING_BUILDING_MAX_COST, currentEstiFuel
                        / DEFAULT_HEATING_BUILDING_FUEL_COEFFICIENT);
            }
            cost -= Math.max(award, HEATING_BUILDING_MIN_COST);
        } else if (currentEstiFieryness == StandardEntityConstants.Fieryness.BURNING.ordinal()) {
            double dCost = initFuel / BURNING_BUILDING_FUEL_COEFFICIENT;
            if (dCost > BURNING_BUILDING_MAX_COST) {
                dCost = BURNING_BUILDING_MAX_COST;
            } else if (dCost < BURNING_BUILDING_MIN_COST) {
                dCost = BURNING_BUILDING_MIN_COST;
            }
            cost += dCost;
        } else if (currentEstiFieryness == StandardEntityConstants.Fieryness.INFERNO.ordinal()) {
            double dCost = initFuel / INFERNO_BUILDING_FUEL_COEFFICIENT;
            if (dCost > INFERNO_BUILDING_MAX_COST) {
                dCost = INFERNO_BUILDING_MAX_COST;
            } else if (dCost < INFERNO_BUILDING_MIN_COST) {
                dCost = INFERNO_BUILDING_MIN_COST;
            }
            cost += dCost;
        }

        // totalHits/totalRays
        cost += HIT_RATE_COEFFICIENT * fireBuilding.getHitRate();

        // Temperature
        if (currentEstiFieryness == StandardEntityConstants.Fieryness.HEATING.ordinal()) {
            double dTemperature = fireBuilding.getEstimatedTemperature()
                    - fireBuilding.getIgnitionPoint();
            cost += Math.min(MAX_TEMPERATURE, dTemperature) / TEMPERATURE_COEFFICIENT;
            if (dTemperature < lOW_TEMPERATURE) {
                cost -= LOW_TEMPERATURE_AWARD;
            }
        }

        float fuelLeft = initFuel - currentEstiFuel;
        cost -= Math.min(FUEL_LEFT_MAX_AWARD, fuelLeft * FUEL_LEFT_COEFFICIENT);

        // better to extinguish the buildings in the changeset
        if (!world.getBuildingSeen().contains(building)) {
            cost += notInChangeSetCost;
        }

        // just updated
        if (condition.getTimestamp() == world.getTime()
                && condition.getUpdateSource() == UpdateSource.ChangeSet
                && condition.getFierynessEnum() == StandardEntityConstants.Fieryness.HEATING) {
            cost -= IN_SIGHT_HEATING_BUILDING_AWARD;
        } else if (world.getTime() - condition.getTimestamp() <= 2
                && condition.getFierynessEnum() == StandardEntityConstants.Fieryness.HEATING) {
            cost -= JUST_NOTICED_HEATING_BUILDING_AWARD;
        }

        // need confirm?
        if (fcc.shouldConfirmBuildingCondition(fireBuilding)) {
            cost += CONFIRM_COST;
        }

        return (int) (cost < 0 ? 0 : cost);
    }

}
*/
