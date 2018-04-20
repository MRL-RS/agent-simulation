package mrl.partition;

import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;

import java.util.Collection;
import java.util.Map;

/**
 * User: Pooyad
 * Date: Jun 13, 2010
 * Time: 10:06:15 PM
 */
public interface PartitionsI extends Collection<Partition> {
    Partition getMyPartition();

    Partition getPartition(Pair<Integer, Integer> pairPosition);

    Partition getPartition(Partitionable object);

    Partition getPartition(Path path);

    Partition findPartitionID(int x, int y);

    Map<StandardEntity, Partition> getHumanPartitionMap();
}
