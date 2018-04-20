package mrl.firebrigade.targetSelection;

import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.common.clustering.Cluster;
import mrl.common.clustering.FireCluster;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.util.List;
import java.util.Set;

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
public class GreedyDirectionBasedTargetSelector extends DefaultFireBrigadeTargetSelector {
    public GreedyDirectionBasedTargetSelector(MrlFireBrigadeWorld world) {
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
        MrlBuilding targetBuilding = null;
        List<MrlBuilding> inDirectionBuildings;
        //TODO @Mostafa:Consider other fire clusters to find the farthest point
        //TODO @Mostafa&Pooya: rewrite the whole method
        Point targetPoint = directionManager.findFarthestPointOfMap(fireCluster);

        //TODO @Mostafa&Pooya: rewrite the whole method; you can use enums to for it
//        inDirectionBuildings = fireCluster.findBuildingsInDirectionOf(targetPoint, true, false);
        inDirectionBuildings = fireCluster.getBuildingsInDirection();

        if (inDirectionBuildings.isEmpty()) {

            Area nearestArea = Util.nearestEntityTo(borderEntities, world.getSelfLocation());
            if (nearestArea != null) {
                targetBuilding = world.getMrlBuilding(nearestArea.getID());
            }
        } else {
            targetBuilding = Util.findNearest(inDirectionBuildings, world.getSelfPosition());
        }
        return targetBuilding;
    }

}
