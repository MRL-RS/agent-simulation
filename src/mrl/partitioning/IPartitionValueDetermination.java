package mrl.partitioning;

import mrl.world.MrlWorld;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/5/12
 * Time: 12:50 PM
 */
public interface IPartitionValueDetermination {

    double computeValue(MrlWorld world, Partition partition);

}
