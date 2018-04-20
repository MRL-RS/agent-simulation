package mrl.firebrigade.sterategy;

import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.clustering.FireCluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/19/13
 * Time: 6:21 PM
 * Author: Mostafa Movahedi
 */
public class FireClusterConditionBasedTargetSelector {
    private MrlFireBrigadeWorld world;
    private MrlFireBrigadeDirectionManager directionManager;
    private FireBrigadeUtilities fireBrigadeUtilities;
    double distanceNormalizer;
    FireClusterConditionBasedActionStrategy strategy;

    public FireClusterConditionBasedTargetSelector(MrlFireBrigadeWorld world, FireClusterConditionBasedActionStrategy strategy, MrlFireBrigadeDirectionManager directionManager, FireBrigadeUtilities fireBrigadeUtilities) {
        this.world = world;
        this.directionManager = directionManager;
        this.fireBrigadeUtilities = fireBrigadeUtilities;
//        distanceNormalizer = world.getMapDiameter()/50;
        distanceNormalizer = MRLConstants.MEAN_VELOCITY_OF_MOVING;
        this.strategy = strategy;
    }

    /**
     * select a target based on fire cluster condition
     *
     * @return MrlBuilding target
     */
    public MrlBuilding selectTarget() {
//        FireCluster targetCluster = (FireCluster) world.getFireClusterManager().findSmallestCluster();
        FireCluster targetCluster = (FireCluster) world.getFireClusterManager().findNearestCluster(world.getSelfLocation());
        if (targetCluster == null) {
            return null;
        }
        SortedSet<Pair<EntityID, Double>> sortedBuildings = null;
        sortedBuildings = calculateValueForEdgeControllableCondition(targetCluster);
        /*fireBrigadeUtilities.calcClusterCondition(targetCluster);
        switch (targetCluster.getCondition()) {
            case smallControllable:
                sortedBuildings = calculateValueForSmallControllableCondition(targetCluster);
                break;
            case largeControllable:
                sortedBuildings = calculateValueForLargeControllableCondition(targetCluster);
                break;
            case edgeControllable:
                sortedBuildings = calculateValueForEdgeControllableCondition(targetCluster);
                break;
            case unControllable:
                sortedBuildings = calculateValueForUnControllableCondition(targetCluster);
                break;
        }*/
        //SortedSet<Pair<EntityID, Double>> tenTopBuildings = fireBrigadeUtilities.selectTop(10, sortedBuildings, ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        //tenTopBuildings = fireBrigadeUtilities.oldReRankBuildings(tenTopBuildings);

        MrlPersonalData.VIEWER_DATA.setBuildingValues(world.getSelf().getID(), world.getMrlBuildings());

        if (sortedBuildings.size() > 0) {
            strategy.lastTarget = strategy.target;
            strategy.target = world.getMrlBuilding(sortedBuildings.first().first());
            return strategy.target;
        } else {
            return null;
        }
    }

    private SortedSet<Pair<EntityID, Double>> calculateValueForSmallControllableCondition(FireCluster fireCluster) {
        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        for (StandardEntity next : borderEntities) {
            MrlBuilding building = world.getMrlBuilding(next.getID());
            if (building.getEstimatedFieryness() == 0) continue;
            double distance = world.getDistance(world.getSelf().getID(), building.getID()) / distanceNormalizer;
            building.BUILDING_VALUE = distance + building.getEstimatedFieryness() + building.getAdvantageRatio();
            sortedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
        }
        return sortedBuildings;
    }

    private SortedSet<Pair<EntityID, Double>> calculateValueForLargeControllableCondition(FireCluster fireCluster) {
        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();
        Point mapCenter = new Point(world.getCenterOfMap().getX(), world.getCenterOfMap().getY());
//        List<MrlBuilding> directionBuildings = fireCluster.findBuildingsInDirectionOf(mapCenter, false, false);
        List<MrlBuilding> directionBuildings = fireCluster.getBuildingsInDirection();
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        borderEntities.removeAll(directionBuildings);
        borderEntities.removeAll(world.getBorderBuildings());
        if (directionBuildings.isEmpty()) {
            calculateValueForOtherBuildings(sortedBuildings, borderEntities, mapCenter, distanceNormalizer);
        }
        calculateValueForHighValueBuildings(sortedBuildings, directionBuildings, mapCenter, distanceNormalizer);
        return sortedBuildings;
    }

    private SortedSet<Pair<EntityID, Double>> calculateValueForEdgeControllableCondition(FireCluster fireCluster) {
        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        List<MrlBuilding> directionBuildings;
//        Point targetPoint = directionManager.findBestValueTarget(fireCluster, world.getCivilianClusterManager().getClusters());
        Point targetPoint = directionManager.findFarthestPointOfMap(fireCluster);
//        directionBuildings = fireCluster.findBuildingsInDirectionOf(targetPoint, true, false);
        directionBuildings = fireCluster.getBuildingsInDirection();
        borderEntities.removeAll(directionBuildings);
        if (directionBuildings.isEmpty()) {
            calculateValueForOtherBuildings(sortedBuildings, borderEntities, targetPoint, distanceNormalizer);
        }
        calculateValueForHighValueBuildings(sortedBuildings, directionBuildings, targetPoint, distanceNormalizer);
        return sortedBuildings;
    }

    private SortedSet<Pair<EntityID, Double>> calculateValueForUnControllableCondition(FireCluster fireCluster) {
        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        List<MrlBuilding> directionBuildings;
//        Point targetPoint = directionManager.findBestValueTarget(fireCluster, world.getCivilianClusterManager().getClusters());
        Point targetPoint = directionManager.findFarthestPointOfMap(fireCluster);
//        directionBuildings = fireCluster.findBuildingsInDirectionOf(targetPoint, true, false);
        directionBuildings = fireCluster.getBuildingsInDirection();
        calculateValueForHighValueBuildings(sortedBuildings, directionBuildings, targetPoint, distanceNormalizer);
        return sortedBuildings;
    }

    private void calculateValueForHighValueBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<MrlBuilding> highValueBuildings, Point targetPoint, double distanceNormalizer) {
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
            if (strategy.lastTarget != null) {
                b.BUILDING_VALUE += world.getDistance(strategy.lastTarget.getID(), b.getID()) / MRLConstants.MEAN_VELOCITY_OF_MOVING;
            }
            if (b == strategy.lastTarget) {
                b.BUILDING_VALUE *= 0.7;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
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
            if (strategy.lastTarget != null) {
                b.BUILDING_VALUE += world.getDistance(strategy.lastTarget.getID(), b.getID()) / MRLConstants.MEAN_VELOCITY_OF_MOVING;
            }
            b.BUILDING_VALUE += 1000;
            if (b == strategy.lastTarget) {
                b.BUILDING_VALUE *= 0.7;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }
}
