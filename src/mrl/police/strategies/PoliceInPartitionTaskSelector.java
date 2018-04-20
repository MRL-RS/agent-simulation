package mrl.police.strategies;

import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.partitioning.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.PoliceConditionChecker;
import mrl.police.moa.PoliceForceUtilities;
import mrl.task.Task;
import mrl.world.MrlWorld;

import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 12/2/12
 *         Time: 7:39 PM
 */
public class PoliceInPartitionTaskSelector extends PoliceTaskSelection {

    public PoliceInPartitionTaskSelector(MrlWorld world, MrlPlatoonAgent self, Set<Partition> partitions, PoliceForceUtilities utilities, PoliceMessageHelper messageHelper, PoliceConditionChecker conditionChecker, ITargetManager targetManager) {
        super(world, self, partitions, utilities, messageHelper, conditionChecker, targetManager);
    }

    @Override
    public Task act() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
