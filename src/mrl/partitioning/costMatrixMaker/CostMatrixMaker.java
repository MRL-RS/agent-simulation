package mrl.partitioning.costMatrixMaker;

import mrl.common.Util;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.IPathPlanner;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/9/12
 * Time: 7:32 PM
 */
public class CostMatrixMaker {


    private MrlWorld world;

    public CostMatrixMaker(MrlWorld world) {
        this.world = world;
    }

    /**
     * This method makes cost matrix from each agent distances to center of each partition
     *
     * @param deflatedPartitions
     * @param agentList          list of agents to compute their costs
     * @return
     */
    public double[][] makingCostMatrix(List<Partition> deflatedPartitions, List<StandardEntity> agentList) {
        int n = agentList.size();
        int m = deflatedPartitions.size();
        double[][] costMatrix = new double[n][m];
        StandardEntity positionEntity;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                positionEntity = world.getEntity(world.getAgentFirstPositionMap().get(agentList.get(i).getID()));
                costMatrix[i][j] = Util.distance(positionEntity.getLocation(world), deflatedPartitions.get(j).getCenter());
            }
        }


        return costMatrix;
    }


    public double[][] makingCostMatrix(List<Partition> deflatedPartitions, List<StandardEntity> agentList, Set<Map.Entry<Human, Partition>> humanPartitionMap) {
        int n = agentList.size();
        double[][] costMatrix = new double[n][n];
        Partition partition;

        for (int i = 0; i < n; i++) {
            partition = findPartition(humanPartitionMap, agentList.get(i));
            for (int j = 0; j < n; j++) {
                costMatrix[i][j] = Util.distance(partition.getCenter(), deflatedPartitions.get(j).getCenter());
            }
        }

        return costMatrix;
    }

    private Partition findPartition(Set<Map.Entry<Human, Partition>> humanPartitionMap, StandardEntity agentEntity) {
        Partition foundPartition = null;
        for (Map.Entry<Human, Partition> humanPartitionEntry : humanPartitionMap) {
            if (humanPartitionEntry.getKey().getID().equals(agentEntity.getID())) {
                foundPartition = humanPartitionEntry.getValue();
                break;
            }
        }
        return foundPartition;
    }


    public int[][] makingCostMatrix_BasedOnID(List<StandardEntity> agentList) {

        int n = agentList.size();
        int[][] costMatrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // cost for agent i to reach current partition is zero and to others is Max value to keep each assigned agent
                // in its current partition
                costMatrix[i][j] = agentList.get(i).getID().getValue();
            }
        }


        return costMatrix;

    }


    /**
     * Computes simple euclidean distance between a policeForce and center of a partition
     *
     * @param agent     agent to compute its distance
     * @param partition a partition of the map
     * @return an integer which shows the distance of two entities
     */
    private int computeDistance(StandardEntity agent, Partition partition) {
        IPathPlanner pathPlanner = world.getPlatoonAgent().getPathPlanner();
        Human human = (Human) agent;
        pathPlanner.planMove((Area) world.getEntity(human.getPosition()), (Area) partition.getCenterEntity(), 0, true);
        return pathPlanner.getPathCost();
//        return Util.distance(agent.getLocation(world), partition.getCenter());
    }


}
