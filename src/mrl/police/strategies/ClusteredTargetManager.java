package mrl.police.strategies;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.partitioning.Partition;
import mrl.police.moa.Importance;
import mrl.police.moa.Target;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 12/16/12
 *         Time: 10:45 AM
 */
public class ClusteredTargetManager extends DefaultTargetManager {


    //map of StandardEntity of targets, not their positions and their done time
    private Map<StandardEntity, Integer> unDoneTargets;

    public ClusteredTargetManager(MrlWorld world) {
        super(world);
    }


    @Override
    public Map<EntityID, Target> getTargets(Partition partition) {

        Map<EntityID, Target> targets = new FastMap<EntityID, Target>();

        boolean fireExists = isFireExists();

        if (unDoneTargets == null) {
            unDoneTargets = new FastMap<StandardEntity, Integer>();
            // fill unDoneTargets with specified clustered targets
            Collection<List<StandardEntity>> targetsCollection = partition.getEntityPositionMap().values();
            if (targetsCollection != null && !targetsCollection.isEmpty()) {
                for (List<StandardEntity> targetsList : targetsCollection) {
                    for (StandardEntity entity : targetsList) {
                        unDoneTargets.put(entity, -1);
                    }
                }
            }
        }

        updateTargets(unDoneTargets);

        //if there is no target in this partition, so it is DONE!
        if (unDoneTargets.isEmpty()) {
            partition.setDone(true);
        } else {

            Target target = null;
            Pair<Importance, Integer> importancePair;
            int distanceToIt;

            for (StandardEntity entity : unDoneTargets.keySet()) {


                if (entity instanceof Human) {
                    Human human = (Human) entity;
                    importancePair = getImportance(entity,null,fireExists);
                    target = new Target(human.getID(), world.getAgentPositionMap().get(human.getID()), importancePair.second(), importancePair.first());
                    distanceToIt = world.getMyDistanceTo(human.getPosition());
                } else {
                    importancePair = getImportance(entity,null,fireExists);
                    target = new Target(entity.getID(), entity.getID(), importancePair.second(), importancePair.first());
                    distanceToIt = world.getMyDistanceTo(entity);
                }
                target.setDistanceToIt(distanceToIt);
                target.setImportance(importancePair.second());


                targets.put(target.getId(), target);
            }
        }

        return targets;
    }

    /**
     * Remove all done targets from {@code unDoneTargets}
     *
     * @param unDoneTargets set of all un-inspected entities as targets
     */
    private void updateTargets(Map<StandardEntity, Integer> unDoneTargets) {
        for (StandardEntity entity : doneTargets.keySet()) {
            unDoneTargets.remove(entity);
        }
    }


    @Override
    protected Pair<Importance, Integer> getImportance(StandardEntity entity, Importance importanceType,boolean fireExists) {

        Pair<Importance, Integer> importancePair = null;
        int importanceValue = 0;
        Human human = null;
        if (entity instanceof Human) {
            human = (Human) entity;
        }


        //checking Refuge Building
        if (entity instanceof Refuge) {
            importanceType = Importance.REFUGE_ENTRANCE;
            importanceValue += importanceType.getImportance();
            importancePair = new Pair<Importance, Integer>(importanceType, importanceValue);

            // checking Fire Brigade
        } else if (entity instanceof FireBrigade) {
            if (world.isBuried(human)) {
                importanceType = Importance.BURIED_FIRE_BRIGADE;
                importanceValue += importanceType.getImportance();
                importancePair = new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = Importance.BLOCKED_FIRE_BRIGADE;
                importanceValue += importanceType.getImportance();
                importancePair = new Pair<Importance, Integer>(importanceType, importanceValue);

            }

            //checking Police Force
        } else if (entity instanceof PoliceForce) {
            importanceType = Importance.BURIED_POLICE_FORCE;
            importanceValue += importanceType.getImportance();
            importancePair = new Pair<Importance, Integer>(importanceType, importanceValue);


            //checking Ambulance Team
        } else if (entity instanceof AmbulanceTeam) {
            if (world.isBuried(human)) {
                importanceType = Importance.BURIED_AMBULANCE_TEAM;
                importanceValue += importanceType.getImportance();
                importancePair = new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = Importance.BLOCKED_AMBULANCE_TEAM;
                importanceValue += importanceType.getImportance();
                importancePair = new Pair<Importance, Integer>(importanceType, importanceValue);

            }

        }
        return importancePair;
    }
}
