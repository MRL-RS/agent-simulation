package mrl.firebrigade.targetSelection;

import mrl.common.MRLConstants;
import mrl.common.clustering.Cluster;
import mrl.common.clustering.FireCluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/12/13
 * Time: 5:56 PM
 * Author: Mostafa Movahedi
 */


public class BlockadeBasedTargetSelector extends DefaultFireBrigadeTargetSelector {
    public BlockadeBasedTargetSelector(MrlFireBrigadeWorld world) {
        super(world);
        distanceNormalizer = MRLConstants.MEAN_VELOCITY_OF_MOVING;
    }

    private double distanceNormalizer;


    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {

        FireBrigadeTarget fireBrigadeTarget = null;

        if (targetCluster != null) {
            target = calculateValue((FireCluster) targetCluster);
            if (target != null) {
                lastTarget = target;
                fireBrigadeTarget = new FireBrigadeTarget(target, targetCluster);
            }
        }

        return fireBrigadeTarget;

    }


    private MrlBuilding calculateValue(FireCluster fireCluster) {

        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();
        Set<MrlBuilding> inRangeBuildings = FireBrigadeUtilities.getBuildingsInMyExtinguishRange(world);
        List<MrlBuilding> targetBuildings = new ArrayList<MrlBuilding>();
        for (MrlBuilding mrlBuilding : inRangeBuildings) {
            if (borderEntities.contains(mrlBuilding.getSelfBuilding())) {
                targetBuildings.add(mrlBuilding);
            }
        }

        MrlBuilding targetBuilding = null;
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        calculateValueForBuildingsInExtinguishRange(sortedBuildings, targetBuildings, distanceNormalizer);
        if (!sortedBuildings.isEmpty()) {
            targetBuilding = world.getMrlBuilding(sortedBuildings.first().first());
        }
        return targetBuilding;
    }

    private void calculateValueForBuildingsInExtinguishRange(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<MrlBuilding> buildingsInExtinguishRange, double distanceNormalizer) {
        for (MrlBuilding b : buildingsInExtinguishRange) {
            switch (b.getEstimatedFieryness()) {
                case 1:
                    b.BUILDING_VALUE = 1;
                    break;
                case 2:
                    b.BUILDING_VALUE = 2000;
                    break;
                case 3:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 1000 : 2000;
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
            if (lastTarget != null) {
                b.BUILDING_VALUE += world.getDistance(lastTarget.getID(), b.getID()) / MRLConstants.MEAN_VELOCITY_OF_MOVING;
            }
            if (lastTarget != null && b.getID().equals(lastTarget.getID())) {
                b.BUILDING_VALUE *= 0.7;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }


}
