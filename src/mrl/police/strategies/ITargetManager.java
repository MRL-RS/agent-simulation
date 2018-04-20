package mrl.police.strategies;

import mrl.partitioning.Partition;
import mrl.police.moa.Target;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 12/16/12
 *         Time: 2:36 PM
 */
public interface ITargetManager {


    public Map<EntityID, Target> getTargets(Partition partition);


    public Map<StandardEntity,Integer> getDoneTargets();
}
