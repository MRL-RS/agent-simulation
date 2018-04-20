package mrl.partitioning;

import javolution.util.FastMap;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Map;

/**
 * This implementation of {@link mrl.partitioning.IPartitionNeededAgentsComputation} reports need of "1" agent for each partition.<br/>
 * <p/>
 * <b>Note:</b> There exist a Threshold T , so just partitions with value higher than T can have assigned agents
 */
public class NeededAgentsComputation_ThresholdBased implements IPartitionNeededAgentsComputation {

    private static double PARTITION_VALUE_THRESHOLD = 0.5;

    @Override
    public Map<EntityID, Integer> computeNeededAgents(List<Partition> partitions, int agents) {
        Map<EntityID, Integer> neededAgentMap = new FastMap<EntityID, Integer>();
        for (Partition partition : partitions) {
            if (partition.getValue() > PARTITION_VALUE_THRESHOLD) {
                neededAgentMap.put(partition.getId(), 1);
            } else {
                neededAgentMap.put(partition.getId(), 0);
            }
        }
        return neededAgentMap;
    }
}
