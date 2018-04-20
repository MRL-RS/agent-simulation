package mrl.police.strategies;

import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.partitioning.IPartitionManager;
import mrl.partitioning.Partition;
import mrl.partitioning.PolicePartitionManager;
import mrl.platoon.State;
import mrl.police.PoliceConditionChecker;
import mrl.police.clear.ClearActManager;
import mrl.police.moa.PoliceForceUtilities;
import mrl.task.Task;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 9:00 PM
 */
public class TaskPrioritizationActionStrategy extends DefaultActionStrategy {
    private static final Log logger = LogFactory.getLog(TaskPrioritizationActionStrategy.class);

    private IPartitionManager partitionManager;
    private boolean inPartition = true;


    private PoliceTaskSelection taskStrategy;
    private ITargetManager targetManager;


    public TaskPrioritizationActionStrategy(MrlWorld world, ClearActManager clearActManager, PoliceMessageHelper policeMessageHelper, PoliceForceUtilities utilities, PoliceConditionChecker conditionChecker) {
        super(world, clearActManager, policeMessageHelper, utilities, conditionChecker);


        targetManager = new PartitionTargetManager(world);

        // In both SINGULAR_PARTITIONING and DUAL_PARTITIONING, we need to do the partitioning at Time 0 (right here) based
        // on Healthy Police Forces, namely those police forces which are already on the road.
        initializePartitionManager();

        MrlPersonalData.VIEWER_DATA.setPartitions(selfHuman.getID(), partitionManager.getPartitions(), partitionManager.findHumanPartition(selfHuman),partitionManager.findHumanPartitionsMap(selfHuman));
    }


    @Override
    public void execute() throws CommandException {

        // I'm buried and can't do anything
        if (selfHuman.getBuriedness() > 0) {
            //todo send buriedAgentMessage
            return;
        }

        utilities.updateHealthyPoliceForceList();

        partitionManager.update();

        scapeOut();

        myTask = getNextTask();

        List<EntityID> pathToGo;

        if (myTask == null) {
            clearActManager.clearWay(null, null);
            logger.debug("getNextTask() returned null.");
        } else {
            me.setAgentState(State.WORKING);
            targetRoad = utilities.getNearestRoad(myTask.getTarget().getRoadsToMove());
            pathToGo = me.getPathPlanner().planMove((Area) world.getSelfPosition(), targetRoad, 0, true);

            if (!pathToGo.isEmpty()) {
                clearActManager.clearWay(pathToGo, myTask.getTarget().getId());
                me.getPathPlanner().moveOnPlan(pathToGo);
//                sendMoveAct(world.getTime(), pathToGo);
                clearActManager.clearWay(pathToGo, myTask.getTarget().getId());
            } else if (targetRoad != null) {
                clearActManager.clearWay(pathToGo, myTask.getTarget().getId());
            }

            targetRoad = null;
        }

        search();
    }

    /**
     * This method handles works that should be done after each  aftershock
     */
    @Override
    public void doAftershockWork() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Creates {@link #partitionManager} and initializes it, Then sets {@link #world} partitionManager reference to the newly
     * created and initialized partition manager.
     */
    private void initializePartitionManager() {
//        if (world.getPoliceForceList().size() == 1) {
//            partitionManager = new NullPartitionManager(world);
//        } else {
        if (partitionManager == null) {
            partitionManager = new PolicePartitionManager(world, utilities);
        }
        partitionManager.initialise();
//        }
        world.setPartitionManager(partitionManager);
    }


    private Task getNextTask() {
        if (taskStrategy == null) {
            Set<Partition> partitions = new HashSet<Partition>();
            partitions.add(partitionManager.findHumanPartition(selfHuman));
            taskStrategy = new PolicePrioritizationTaskSelector(world, me, partitions, utilities, policeMessageHelper, conditionChecker, targetManager);
        }
        return taskStrategy.act();
    }

}
