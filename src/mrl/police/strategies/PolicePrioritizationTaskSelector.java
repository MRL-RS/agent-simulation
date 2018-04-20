package mrl.police.strategies;

import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.partitioning.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.PoliceConditionChecker;
import mrl.police.moa.Importance;
import mrl.police.moa.PoliceForceUtilities;
import mrl.police.moa.Target;
import mrl.task.MoveStyle;
import mrl.task.PoliceActionStyle;
import mrl.task.Task;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: Pooya
 * Date: 3/1/12
 * Time: 7:45 PM
 */
public class PolicePrioritizationTaskSelector extends PoliceTaskSelection {

    Map<Partition, Partition> donePartitionAndNeighbours = new HashMap<>();


    public PolicePrioritizationTaskSelector(MrlWorld world, MrlPlatoonAgent self, Set<Partition> partitions, PoliceForceUtilities utilities, PoliceMessageHelper messageHelper, PoliceConditionChecker conditionChecker, ITargetManager targetManager) {
        super(world, self, partitions, utilities, messageHelper, conditionChecker, targetManager);
    }

    @Override
    public Task act() {

        List<Target> targetList = new ArrayList<Target>();
        if (partitions == null) {
            targetList.addAll(targetManager.getTargets(null).values());
        } else {
            for (Partition partition : partitions) {
                targetList.addAll(targetManager.getTargets(partition).values());
            }
        }


        //TODO: Make VALUE for targets based on different parameters and sort targets based on them
        Task prevTask = myTask;
        if (prevTask == null || conditionChecker.isTaskDone(prevTask)) {
            prevTask = null;
        }

        if (!targetList.isEmpty()) {
            Collections.sort(targetList, utilities.TARGET_IMPORTANCE_COMPARATOR);
            EntityID selectedID = world.getSelf().getID();

            if (selectedID != null && (selectedID.equals(MrlViewer.CHECK_ID) || selectedID.equals(MrlViewer.CHECK_ID2))) {
                world.printData(targetList.toString());
            }
//            if (world.getSelf().getID().getValue() == 702982422) {
//                for (Target target : targetList) {
//                    world.printData("targetImportance: " + target.getImportanceType().toString() + " ID: " + target.getId() + " importance: " + target.getImportance());
//                }
//            }

            myTask = null;
            Target mostImportantTarget = null;
            int i = 0;
            while (!targetList.isEmpty() && i < targetList.size()) {
                if (!targetList.isEmpty()) {
                    mostImportantTarget = targetList.get(i);
                }


                //TODO: change isThereMoreImportantTask with isThereMoreValuable task

                if (myTask == null || isThereMoreImportantTask(myTask.getTarget(), mostImportantTarget)) {
                    if (mostImportantTarget != null) {

                        if (mostImportantTarget.getImportanceType().equals(Importance.NEIGHBOUR_PARTITION_CENTER)) {

                            if (world.getBurningBuildings().isEmpty()) {
                                continue;
                            } else {

                                Target target = reevaluateTargetListBasedOnFire(targetList);

                                if (target == null) {
                                    continue;//don't count i
                                }
                                mostImportantTarget = target;
//                                mostImportantTarget = targetList.get(i);
                            }
                        }

                        mostImportantTarget.setRoadsToMove(utilities.getRoadsToMove(mostImportantTarget));
//                        if (mostImportantTarget.getImportanceType().equals(Importance.PARTITION_CENTER)) {
//                            myTask = new Task(mostImportantTarget, PoliceActionStyle.CLEAR_ROUTE, MoveStyle.FASTEST_PATH);
//                        } else {
                            myTask = new Task(mostImportantTarget, utilities.getActionStyle(mostImportantTarget), MoveStyle.FASTEST_PATH);
//                        }
                        if (conditionChecker.isTaskDone(myTask)) {
                            doneTaskOperations(targetList);
                            continue;// don't count i
                        } else {
                            break;//found new task, so break te loop immediately
                        }
                    }
                } else if (conditionChecker.isTaskDone(myTask)/*utilities.isDoneTask(myTask)*/) {

                    doneTaskOperations(targetList);

                }
                i++;

            }

        } else {
            myTask = null;
        }

        if (myTask != null && prevTask != null && !prevTask.equals(myTask)) {
            //this task is not same with previous task.
            if (self != null) {
                self.getPathPlanner().planMove((Area) world.getSelfPosition(), world.getEntity(prevTask.getTarget().getPositionID(), Area.class), MRLConstants.IN_TARGET, true);
                int prevPathCost = self.getPathPlanner().getPathCost();
                self.getPathPlanner().planMove((Area) world.getSelfPosition(), world.getEntity(myTask.getTarget().getPositionID(), Area.class), MRLConstants.IN_TARGET, true);
                int pathCost = self.getPathPlanner().getPathCost();
                if (pathCost < prevPathCost) {
                    //this mean new task is closer to agent! so we choose it as new task.
                } else {
                    //this mean new task is farther than previous one. so to prevent trapping in loop we don't choose it as new task..
                    myTask = prevTask;
                }
            }
        }


        return myTask;


    }


    /**
     * This method makes a plan move to the center of the fire and find a partition which is on the way to the fire, then
     * will remove other unimportant target partitions
     *
     * @param targetList
     */
    private Target reevaluateTargetListBasedOnFire(List<Target> targetList) {

        List<Target> toRemove = new ArrayList<>();


//        List fireClusters = new ArrayList(world.getFireClusterManager().getClusters());
        Partition basePartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());

        //first, we should find most important fire cluster from base partition.
//        FireCluster fireCluster = findMostImportantFireCluster(basePartition, fireClusters);


        //Then, find a partition which center of fire cluster is in it.
        Partition burningPartition = findNearestBurningPartition(basePartition);


        //Find a neighbour partition which makes agent closer to fire
        Partition neighbour = null;
        if (burningPartition != null) {
            if (basePartition.getId().equals(burningPartition.getId())) {
                //todo @mahdi What should we do in this situation?
                neighbour = basePartition;
            } else {
                neighbour = findNeighbourPartitionReachesMeToFire(basePartition, burningPartition);
            }
        }


        Target targetPartition = null;
        boolean hasMoreImportantTask = false;
        for (Target target : targetList) {
            if (target.getImportanceType().equals(Importance.PARTITION_CENTER)) {
                targetPartition = target;
                hasMoreImportantTask = true;
                break;
            } else if (target.getImportanceType().getImportance() > Importance.PARTITION_CENTER.getImportance()) {
                hasMoreImportantTask = true;
            }
        }


        for (Target target : targetList) {
            if (target.getImportanceType().equals(Importance.NEIGHBOUR_PARTITION_CENTER)) {
                //when my partition center task was not done, we should do it before neighbour partition center targets.
                if (!hasMoreImportantTask && neighbour != null && target.getId().equals(neighbour.getId()) && !donePartitionAndNeighbours.values().contains(neighbour)) {
                    targetPartition = target;
                } else {
                    toRemove.add(target);
                }
            }
        }

        targetList.removeAll(toRemove);


        if (!hasMoreImportantTask && targetPartition != null && targetManager.getDoneTargets().containsKey(world.getEntity(targetPartition.getId()))) {
            donePartitionAndNeighbours.put(burningPartition, neighbour);
        }

        MrlPersonalData.VIEWER_DATA.setPoliceFireTasksData(world.getSelf().getID(), burningPartition, neighbour);

        return targetPartition;
    }

    /**
     * This method find nearest {@code FireCluster} from base {@code Partition}.
     */
//    private FireCluster findMostImportantFireCluster(Partition myPartition, List fireClusters) {
//
//        if (fireClusters==null || fireClusters.isEmpty()){
//            return null;
//        }
//        FireCluster nearestCluster = null;
//        int minDistance = Integer.MAX_VALUE;
//        for (Object fireClusterObj : fireClusters) {
//            FireCluster fireCluster = (FireCluster) fireClusterObj;
//            int distance = Util.distance(myPartition.getCenter(), fireCluster.getCenter());
//            if (distance < minDistance) {
//                minDistance = distance;
//                nearestCluster = fireCluster;
//            }
//
//        }
//        return nearestCluster;
//    }
    private Partition findNeighbourPartitionReachesMeToFire(Partition basePartition, Partition burningPartition) {
        if (basePartition.getId().equals(burningPartition.getId())) {
            return null;
        }

        Partition neighbour = null;

        List<EntityID> plan = world.getPlatoonAgent().getPathPlanner().planMove((Area) basePartition.getCenterEntity(), (Area) burningPartition.getCenterEntity(), 0, true);

        for (EntityID areaID : plan) {
            if (basePartition.getRoadIDs().contains(areaID) || basePartition.getBuildingIDs().contains(areaID)) {
                //It means this area placed in base partition.
                continue;
            } else {
                neighbour = world.getPartitionManager().findPartitionAtArea(areaID);
                break;
            }

        }


        return neighbour;
    }

    /**
     * Finds nearest burning partition to basePartition
     *
     * @param basePartition
     * @return
     */
    private Partition findNearestBurningPartition(Partition basePartition) {
        if (basePartition == null) {
            return null;
        }


        int minDistance = Integer.MAX_VALUE;
        Partition nearestPartition = null;
        for (Partition partition : world.getPartitionManager().getPartitions()) {

            if (partition.getBurningBuildings().isEmpty() || donePartitionAndNeighbours.containsKey(partition)) {
                continue;
            }

            if (!partition.equals(basePartition)) {
                int distance = Util.distance(partition.getCenter(), basePartition.getCenter());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPartition = partition;
                }
            }
        }


        return nearestPartition;
    }


    private void doneTaskOperations(List<Target> targetList) {
//        if (myTask.getTarget().getId().getValue() == 55687 || myTask.getTarget().getId().getValue() == 55702) {
//            System.out.println("KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK");
//            conditionChecker.isTaskDone(myTask);
//        }

        targetManager.getDoneTargets().put(world.getEntity(myTask.getTarget().getId()), world.getTime());
        targetList.remove(myTask.getTarget());
//        System.out.println("time:" + world.getTime() + " me:" + world.getSelf().getID() + " Done Task:" + myTask.getTarget().getId());

        myTask = null;
    }


    private boolean isThereMoreImportantTask(Target myTarget, Target mostImportantTarget) {

        return mostImportantTarget != null && myTarget.getImportance() < mostImportantTarget.getImportance();

    }


}
