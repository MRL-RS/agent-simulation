package mrl.partitioning;

import javolution.util.FastMap;
import rescuecore2.worldmodel.EntityID;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/5/12
 * Time: 5:56 PM
 */


public class NeededAgentsComputation_ValueBased implements IPartitionNeededAgentsComputation {

    @Override
    public Map<EntityID, Integer> computeNeededAgents(List<Partition> partitions, int agents) {


        Map<EntityID, Integer> neededAgentMap = new FastMap<EntityID, Integer>();
        int numberOfNeed;
        int remainedAgents = agents;
        double sumOfValues = 0;

        Collections.sort(partitions, Partition.PARTITION_VALUE_COMPARATOR);

        for (Partition partition : partitions) {
            sumOfValues += partition.getValue();
        }
        for (Partition partition : partitions) {
            numberOfNeed = (int) Math.round(partition.getValue() / sumOfValues * agents);
            if (numberOfNeed > remainedAgents) {
                numberOfNeed = remainedAgents;
            }
            remainedAgents -= numberOfNeed;
            partition.setNumberOfNeededPFs(numberOfNeed);
            neededAgentMap.put(partition.getId(), numberOfNeed);
        }


        return neededAgentMap;
    }
}
