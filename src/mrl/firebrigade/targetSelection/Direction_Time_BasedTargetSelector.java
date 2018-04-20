package mrl.firebrigade.targetSelection;

import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.clustering.Cluster;
import mrl.common.clustering.FireCluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.helper.PropertyHelper;
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
 * Date: 3/11/13
 * Time: 6:37 PM
 * Author: Mostafa Movahedi
 */


/**
 * 1- Initial direction determined based on farthest point of map from center of nearest cluster.<br/>
 * 2- FBs separates in two parts and try to put off fire cluster in two direction.<br/>
 */
public class Direction_Time_BasedTargetSelector extends DefaultFireBrigadeTargetSelector {
    public Direction_Time_BasedTargetSelector(MrlFireBrigadeWorld world) {
        super(world);
        distanceNormalizer = MRLConstants.MEAN_VELOCITY_OF_MOVING;
        propertyHelper = world.getHelper(PropertyHelper.class);
    }

    private double distanceNormalizer;
    private PropertyHelper propertyHelper;

    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {
        FireBrigadeTarget fireBrigadeTarget = null;

        if (targetCluster != null) {
            SortedSet<Pair<EntityID, Double>> sortedBuildings;
            sortedBuildings = calculateValue((FireCluster) targetCluster);
            sortedBuildings = fireBrigadeUtilities.reRankBuildings(sortedBuildings);

            MrlPersonalData.VIEWER_DATA.setBuildingValues(world.getSelf().getID(), world.getMrlBuildings());

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
//        Point targetPoint = directionManager.findBestDirectionPoint(fireCluster,world.getFireClusterManager().getClusters());

        //TODO @Mostafa&Pooya: rewrite the whole method; you can use enums to for it
//        inDirectionBuildings = fireCluster.findBuildingsInDirectionOf(targetPoint, true, false);
        inDirectionBuildings = fireCluster.getBuildingsInDirection();

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
            b.BUILDING_VALUE += timeCost(b);
            b.BUILDING_VALUE += 1000;
            if (lastTarget != null && b.getID().equals(lastTarget.getID())) {
                b.BUILDING_VALUE *= 0.7;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }

    private int timeCost(MrlBuilding b) {
        if (b.getIgnitionTime() != -1 && world.getTime() - b.getIgnitionTime() > 30) {
            return 2000;
        }
        return 0;
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
            b.BUILDING_VALUE += timeCost(b);
            if (lastTarget != null && b.getID().equals(lastTarget.getID())) {
                b.BUILDING_VALUE *= 0.7;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }

}
