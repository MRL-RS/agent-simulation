package mrl.partitioning;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/5/12
 * Time: 4:06 PM
 */


import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Map;

/**
 * This class defines an interface for computation of needed agents for each partition
 */
public interface IPartitionNeededAgentsComputation {

    /**
     * computes number of needed agents for specified partitions
     *
     * @param partitions partitions to compute their needs
     * @param agents     total number of available agents to allocate
     * @return a map of each partitionID to the number of its need
     */
    Map<EntityID, Integer> computeNeededAgents(List<Partition> partitions, int agents);

}
