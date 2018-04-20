package mrl.partitioning;

import javolution.util.FastMap;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Map;

/**
 * This implementation of {@link IPartitionNeededAgentsComputation} reports need of "1" agent for each partition.<br/>
 * <b>Note:</b> Use only for debugging.
 */
public class NeededAgentsComputation_OneToOne implements IPartitionNeededAgentsComputation {

    @Override
    public Map<EntityID, Integer> computeNeededAgents(List<Partition> partitions, int agents) {
        Map<EntityID, Integer> neededAgentMap = new FastMap<EntityID, Integer>();
        int numberOfPartitions = 0;
        for (Partition partition : partitions) {
            numberOfPartitions++;
            if (numberOfPartitions <= agents) {
                neededAgentMap.put(partition.getId(), 1);
            } else {
                neededAgentMap.put(partition.getId(), 0);
            }
        }
        return neededAgentMap;
    }
}
