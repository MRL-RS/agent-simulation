package mrl.ambulance.targetSelector;

import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 2/21/13
 *         Time: 1:49 PM
 */

/**
 * This implementation uses greedy method to select best target and ignores possible filters such as dead time filters
 */
public class SimpleGreedyTargetSelector extends TargetSelector {


    public SimpleGreedyTargetSelector(MrlWorld world, AmbulanceConditionChecker conditionChecker, AmbulanceUtilities ambulanceUtilities, Partition myPartition) {
        super(world, conditionChecker, ambulanceUtilities);
    }

    /**
     * Finds best target between specified possible targets
     *
     * @param targets targets to search between them
     * @return best target to select
     */
    @Override
    public StandardEntity nextTarget(Set<StandardEntity> targets) {
        StandardEntity bestTarget = null;
        int nearestDistance = Integer.MAX_VALUE;
        int tempDistance;
        StandardEntity position;
        if (targets != null && !targets.isEmpty()) {
            for (StandardEntity targetEntity : targets) {
                position = ((Human) targetEntity).getPosition(world);
                tempDistance = world.getDistance(position, world.getSelfPosition());
                if (tempDistance < nearestDistance) {
                    nearestDistance = tempDistance;
                    bestTarget = targetEntity;
                }
            }
        }
        return bestTarget;
    }
}
