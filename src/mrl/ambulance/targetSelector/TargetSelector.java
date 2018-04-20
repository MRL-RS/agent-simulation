package mrl.ambulance.targetSelector;

import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.Util;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.IPathPlanner;

import java.util.Comparator;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 2/21/13
 *         Time: 1:58 PM
 */
public abstract class TargetSelector implements ITargetSelector {

    protected MrlWorld world;
    protected AmbulanceConditionChecker conditionChecker;
    protected AmbulanceUtilities ambulanceUtilities;
    protected AmbulanceTarget previousTarget;
    protected IPathPlanner pathPlanner;
    protected Partition myBasePartition;
    protected Set<Partition> myPartitions;

    protected TargetSelector(MrlWorld world, AmbulanceConditionChecker conditionChecker, AmbulanceUtilities ambulanceUtilities) {
        this.world = world;
        this.conditionChecker = conditionChecker;
        this.ambulanceUtilities = ambulanceUtilities;
        this.pathPlanner = world.getPlatoonAgent().getPathPlanner();
        setWorkingPartition();
    }


    protected Comparator<Partition> DISTANCE_TO_PARTITION_COMPARATOR = new Comparator<Partition>() {
        public int compare(Partition r1, Partition r2) {

            int firstDistance = Util.distance(r1.getCenter(), world.getSelfLocation());
            int secondDistance = Util.distance(r2.getCenter(), world.getSelfLocation());

            if (firstDistance > secondDistance)
                return 1;
            if (firstDistance == secondDistance)
                return 0;

            return -1;
        }
    };


    protected void setWorkingPartition() {
        if (world.getPartitionManager() != null) {
            myBasePartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
        }
    }


    protected void setWorkingPartitions(){
        if (world.getPartitionManager() != null) {
            myPartitions = world.getPartitionManager().getMyPartitions();
        }
    }

}
