package mrl.police.strategies;

import mrl.partitioning.Partition;
import mrl.police.moa.Importance;
import mrl.police.moa.Target;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 12/16/12
 *         Time: 1:21 PM
 */
public class CoincidentalTargetManager extends DefaultTargetManager {

    protected CoincidentalTargetManager(MrlWorld world) {
        super(world);
    }


    @Override
    protected void putIntoTargets(Partition partition, Map<EntityID, Target> targets, StandardEntity entity, boolean isBuried,boolean fireExists) {
        Target target;
        Pair<Importance, Integer> importancePair;
        int distanceToIt;
        if (entity instanceof Human) {
            Human human = (Human) entity;
            importancePair = getImportance(entity,null,fireExists);
            target = new Target(human.getID(), human.getPosition(), importancePair.second(), importancePair.first());
            distanceToIt = world.getMyDistanceTo(human.getPosition());
        } else {
            importancePair = getImportance(entity,null,fireExists);
            target = foundTargets.get(entity.getID());
            if (target == null) {
                target = new Target(entity.getID(), entity.getID(), importancePair.second(), importancePair.first());
                foundTargets.put(entity.getID(), target);
            }
            distanceToIt = world.getMyDistanceTo(entity);
        }
        target.setDistanceToIt(distanceToIt);
        target.setImportance(importancePair.second());


        if (!doneTargets.containsKey(world.getEntity(target.getId())) && !targets.keySet().contains(target.getId())) {
            if (isNearMe(target)) {
                targets.put(target.getId(), target);
            }
        }
    }


}
