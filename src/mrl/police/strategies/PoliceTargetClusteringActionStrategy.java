package mrl.police.strategies;

import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.partitioning.IPartitionManager;
import mrl.partitioning.Partition;
import mrl.partitioning.PolicePartitionManager;
import mrl.partitioning.PoliceTargetPartitionManager;
import mrl.platoon.State;
import mrl.platoon.genericsearch.CivilianSearchDecisionMaker;
import mrl.platoon.search.SearchHelper;
import mrl.police.PoliceConditionChecker;
import mrl.police.clear.ClearActManager;
import mrl.police.moa.PoliceForceUtilities;
import mrl.task.PoliceActionStyle;
import mrl.task.Task;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: Pooya
 * Date: 3/1/12
 * Time: 7:45 PM
 */
public class PoliceTargetClusteringActionStrategy extends DefaultActionStrategy {
    private static final Log logger = LogFactory.getLog(PoliceTargetClusteringActionStrategy.class);


    private IPartitionManager targetPartitionManager;
    private IPartitionManager searchPartitionManager;
    private PoliceTaskSelection taskStrategy;
    private ITargetManager clusteredTargetManager;
    private ITargetManager partitionTargetManager;
    private ITargetManager coincidentalTargetManager;
    private Map<StandardEntity, Integer> doneTargets;


    public PoliceTargetClusteringActionStrategy(MrlWorld world, ClearActManager clearActManager, PoliceMessageHelper policeMessageHelper, PoliceForceUtilities utilities, PoliceConditionChecker conditionChecker) {
        super(world, clearActManager, policeMessageHelper, utilities, conditionChecker);

        clusteredTargetManager = new ClusteredTargetManager(world);
        partitionTargetManager = new PartitionTargetManager(world);
        coincidentalTargetManager = new CoincidentalTargetManager(world);
        this.doneTargets = new FastMap<StandardEntity, Integer>();

        initializeSearchPartitionManager();
        initializeTargetPartitionManager();

        this.searchHelper = new SearchHelper(world, world.getPlatoonAgent());


    }

    @Override
    public void execute() throws CommandException, TimeOutException {

        utilities.updateHealthyPoliceForceList();
//        System.out.println("time:"+world.getTime()+" myID:"+selfHuman.getID()+" numberOfHealthyPFs:"+utilities.getHealthyPoliceForces().size());
        if (searchPartitionManager != null) {
            searchPartitionManager.update();
        }

        scapeOut();


        //find coincidental targets
        myTask = findCoincidentalWorks();

        //((((((((((((((( A ))))))))))))))
        myTask = findStrictWorks(myTask);

        //((((((((((((((( B ))))))))))))))
        //finding a task from targets in assigned searching partition
        myTask = findPartitionWorks(myTask);

        List<EntityID> pathToGo;

        MrlPersonalData.VIEWER_DATA.setTargetsToGo(selfHuman.getID(), myTask);

        //acting
        if (myTask == null) {
//            clearHelper.clearWay(null, null);
            clearActManager.doPostponeJobs();
            logger.debug("getNextTask() returned null.");
        } else {
            me.setAgentState(State.WORKING);

            if (myTask.getActionStyle().equals(PoliceActionStyle.CLEAR_ROUTE)) {
                if(myTask.isStarted()){
                    //TODO @pooya: try to keep moving to the target point
                    //TODO @pooya: consider to ignore open roads ;  Use shouldCheckForBlockadesOnWay from ClearTools
                }else {
                    //TODO @pooya: try to move to the start point of the road
                }
            } else {

                targetRoad = utilities.getNearestRoad(myTask.getTarget().getRoadsToMove());
                pathToGo = me.getPathPlanner().planMove((Area) world.getSelfPosition(), targetRoad, 0, true);
                if (!pathToGo.isEmpty()) { //means I am away form target first position
                    clearActManager.clearWay(pathToGo, myTask.getTarget().getId());
                    me.getPathPlanner().moveOnPlan(pathToGo);
                    clearActManager.clearWay(pathToGo, myTask.getTarget().getId());
                    //means I am too close to target first position
                } else if (world.getChanges().contains(targetRoad.getID())) {
                    StandardEntity targetEntity = world.getEntity(myTask.getTarget().getId());
                    if (targetEntity instanceof Human) {
                        Human human = (Human) targetEntity;
                        if (human.getPosition(world) instanceof Area) {
                            pathToGo = world.getPlatoonAgent().getPathPlanner().planMove((Area) world.getSelfPosition(), (Area) human.getPosition(world), 0, true);
                        }
                    }
                    //first clear way
                    clearActManager.clearWay(pathToGo, myTask.getTarget().getId());
                    //then clear around of it (if needed)
//                if (myTask.getActionStyle().equals(PoliceActionStyle.CLEAR_HUMAN)) {
                    clearActManager.clearAroundTarget(targetEntity.getLocation(world));
//                }
                }

                targetRoad = null;
            }
        }

        //No task to do, so start searching
        newKindOfSearch();
    }

    /**
     * This method handles works that should be done after each  aftershock
     */
    @Override
    public void doAftershockWork() {

        coincidentalTargetManager.getDoneTargets().clear();
        partitionTargetManager.getDoneTargets().clear();
//        clusteredTargetManager.getDoneTargets().clear();
        doneTargets.clear();
    }

    private Task findPartitionWorks(Task myTask) {
        Set<Partition> assignedPartition;
        if (myTask == null && searchPartitionManager != null) {
            //this update seems to be redundant
//            searchPartitionManager.update();
            assignedPartition = searchPartitionManager.findHumanPartitionsMap(selfHuman);
            if (assignedPartition != null) {
                partitionTargetManager.getDoneTargets().putAll(doneTargets);
                myTask = getNextTask(assignedPartition, partitionTargetManager);
                doneTargets.putAll(partitionTargetManager.getDoneTargets());

            } else {
                //disable searchPartitionManager
                searchPartitionManager = null;
            }
        }
        return myTask;
    }

    private Task findCoincidentalWorks() {
        Task myTask;
        myTask = getNextTask(null, coincidentalTargetManager);

        doneTargets.putAll(coincidentalTargetManager.getDoneTargets());
        return myTask;
    }

    private Task findStrictWorks(Task myTask) {

        Partition assignedPartition;// finding a task from targets of assigned clusters
        if (myTask == null && targetPartitionManager != null) {
            targetPartitionManager.update();
            Set<Partition> partitions = targetPartitionManager.findHumanPartitionsMap(selfHuman);
            if (partitions != null && !partitions.isEmpty()) {
                for (int i = 0; i < partitions.size(); i++) {
                    assignedPartition = targetPartitionManager.findHumanPartition(selfHuman);
                    if (assignedPartition != null) {
                        clusteredTargetManager.getDoneTargets().putAll(doneTargets);
                        Set<Partition> assignedPartitions = new HashSet<>();
                        assignedPartitions.add(assignedPartition);
                        myTask = getNextTask(assignedPartitions, clusteredTargetManager);
                        doneTargets.putAll(clusteredTargetManager.getDoneTargets());
                        if (myTask != null) {
//                            System.out.println("clusteredTarget  time:" + world.getTime() + " target:" + myTask.getTarget().getId());
                            break;
                        }
                    }
                }
            }
        }

        return myTask;
    }

    private void newKindOfSearch() throws CommandException {
        List<EntityID> pathToGo = new ArrayList<EntityID>(search());

        if (!pathToGo.isEmpty()) {

//            pathToGo=addSelfPosition(pathToGo);

            clearActManager.clearWay(pathToGo, pathToGo.get(pathToGo.size() - 1));
            //move based on pathToGo
            List<EntityID> pathToGoForMove = new ArrayList<EntityID>(pathToGo);

            me.getPathPlanner().moveOnPlan(pathToGoForMove);
            if (pathToGo.size() == 0) {
                return;
            }
            clearActManager.clearWay(pathToGo, pathToGo.get(pathToGo.size() - 1));
        }/* else if (targetRoad != null) {
            clearHelper.clearWay(pathToGo, pathToGo.get(pathToGo.size() - 1));
        }*/
        me.civilianSearchBBDecisionMaker.result(CivilianSearchDecisionMaker.SearchResult.SUCCESSFUL);
    }

    private List<EntityID> addSelfPosition(List<EntityID> pathToGo) {
        List<EntityID> tempList = new ArrayList<EntityID>();
        tempList.add(world.getSelfPosition().getID());
        tempList.addAll(pathToGo);
        return tempList;
    }


    private Task getNextTask(Set<Partition> partitions, ITargetManager targetManager) {
        if (taskStrategy == null) {
            taskStrategy = new PolicePrioritizationTaskSelector(world, me, partitions, utilities, policeMessageHelper, conditionChecker, targetManager);
        }
        taskStrategy.setPartitions(partitions);
        taskStrategy.setTargetManager(targetManager);
        return taskStrategy.act();
    }


    /**
     * Creates {@link #targetPartitionManager} and initializes it, Then sets {@link #world} targetPartitionManager reference to the newly
     * created and initialized partition manager.
     */
    private void initializeTargetPartitionManager() {

        if (targetPartitionManager == null) {
            targetPartitionManager = new PoliceTargetPartitionManager(world, utilities, new PartitionTargetManager(world));
        }
        targetPartitionManager.initialise();
    }

    /**
     * makes a new instance of {@link PoliceTargetPartitionManager}
     */
    private void resetTargetPartitionManager() {
        targetPartitionManager = new PoliceTargetPartitionManager(world, utilities, clusteredTargetManager);
        targetPartitionManager.initialise();
    }

    /**
     * Creates {@link #targetPartitionManager} and initializes it, Then sets {@link #world} targetPartitionManager reference to the newly
     * created and initialized partition manager.
     */
    private void initializeSearchPartitionManager() {

        if (searchPartitionManager == null) {
            searchPartitionManager = new PolicePartitionManager(world, utilities);
        }
        searchPartitionManager.initialise();

        world.setPartitionManager(searchPartitionManager);

    }
}
