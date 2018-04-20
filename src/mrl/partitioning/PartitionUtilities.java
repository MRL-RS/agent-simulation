package mrl.partitioning;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.helper.HumanHelper;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/6/12
 * Time: 5:37 PM
 */
public class PartitionUtilities {

    private MrlWorld world;
    private HumanHelper humanHelper;

    public PartitionUtilities(MrlWorld world) {
        this.world = world;
        this.humanHelper = world.getHelper(HumanHelper.class);
    }


    public List<EntityID> getBlockedAgents(Partition partition, boolean considerFirstPosition) {
        List<EntityID> blockedAgents = new ArrayList<EntityID>();
        for (Human human : humanHelper.getBlockedAgents()) {
            if (partition.containsInEntities(human.getPosition())
                    || (considerFirstPosition && partition.containsInEntities(world.getAgentPositionMap().get(human.getID())))) {
                blockedAgents.add(human.getID());
            }
        }
        return blockedAgents;
    }


    public List<EntityID> getBuriedAgents(Partition partition, boolean considerFirstPosition) {

        List<EntityID> buriedAgents = new ArrayList<EntityID>();
        Human human;
        for (EntityID buriedID : world.getBuriedAgents()) {
            human = (Human) world.getEntity(buriedID);
            if (partition.containsInEntities(human.getPosition())
                    || (considerFirstPosition && partition.containsInEntities(world.getAgentPositionMap().get(human.getID())))) {
                buriedAgents.add(human.getID());
            }
        }
        return buriedAgents;


    }

    public List<EntityID> getUnVisitedBuildings(Partition partition) {

        List<EntityID> unvisitedBuildings = new ArrayList<EntityID>();
        for (EntityID id : partition.getBuildingIDs()) {
            if (world.getUnvisitedBuildings().contains(id)) {
                unvisitedBuildings.add(id);
            }
        }
        return unvisitedBuildings;
    }


    public List<EntityID> getBurningBuildings(Partition partition) {
        List<EntityID> buildings = new ArrayList<EntityID>(partition.getBuildingIDs());
        List<EntityID> burningBuildings = new ArrayList<EntityID>();
        buildings.removeAll(partition.getBurningBuildings());

        Building building;
        for (EntityID entityID : buildings) {
            building = (Building) world.getEntity(entityID);
            if ((building.isFierynessDefined() && building.getFieryness() != 0 && building.getFieryness() != 4)
                    || building.isTemperatureDefined() && building.getTemperature() > 0) {
                burningBuildings.add(entityID);
            }
        }
        burningBuildings.addAll(partition.getBurningBuildings());

        return burningBuildings;


    }


    /**
     * This method shows whether a partition is dead or not, which will be estimated by considering the amount of burning
     * buildings of that partition
     *
     * @param partition the partition to examine
     * @return true if partition is dead
     */
    public boolean isPartitionDead(Partition partition) {
        boolean isDead = false;

        //TODO: calculating the amount of overlap of burning buildings convexHull and this partition
//        for (Cluster cluster:world.getFireClusterManager().getClusters()){
//            cluster.getConvexHullObject().getConvexPolygon().;
//        }


        if (partition.isDead()
                || partition.getBurningBuildings().size() > 0.75 * partition.getBuildings().size()) {
            isDead = true;
        }
        return isDead;
    }


    public static Map<EntityID, Set<StandardEntity>> findPartitionVictimMap(MrlWorld world, Set<StandardEntity> victims) {

        // a map of each partition and victims in it
        Map<EntityID, Set<StandardEntity>> partitionVictimMap = new FastMap<EntityID, Set<StandardEntity>>();

        Human human;
        Partition partition;
        Set<StandardEntity> victimsInPartition;
        for (StandardEntity victimEntity : victims) {
            human = (Human) victimEntity;
            partition = world.getPartitionManager().findPartitionAtArea(human.getPosition());
            if (partition == null) {
                // so where the hell is this victim !!?
            } else {
                victimsInPartition = partitionVictimMap.get(partition.getId());
                if (victimsInPartition == null) {
                    victimsInPartition = new FastSet<StandardEntity>();
                }
                victimsInPartition.add(victimEntity);
                partitionVictimMap.put(partition.getId(), victimsInPartition);
            }
        }


        return partitionVictimMap;
    }


    /**
     * This method finds paths from each partition with no refuges in it to other partitions which has refuge inside.
     * The mentioned path will be gained by path planning algorithm which will be aware to the highways
     *
     * @param partitions partitions to find paths based on them
     * @return a map of PartitionID to the path of the center of that partition to the nearest refuge
     */
    public Map<EntityID, Set<EntityID>> findPartitionsRefugePaths(List<Partition> partitions) {

        Map<EntityID, Set<EntityID>> partitionsRefugePaths = new FastMap<EntityID, Set<EntityID>>();

        List<EntityID> nearestRefugePath;
        Set<EntityID> nearestRefugePathSet;
        for (Partition partition : partitions) {
            nearestRefugePath = world.getPlatoonAgent().getPathPlanner().getRefugePath((Area) partition.getCenterEntity(), true);
            if (nearestRefugePath != null && !nearestRefugePath.isEmpty()) {
                nearestRefugePathSet = new FastSet<EntityID>();
                for (EntityID entityID : nearestRefugePath) {
                    nearestRefugePathSet.add(entityID);
                }
                partitionsRefugePaths.put(partition.getId(), nearestRefugePathSet);
            }
        }
        return partitionsRefugePaths;
    }


    /**
     * This method finds entities of different paths which are paths throw the specified partition to a refuge
     *
     * @param partition               Partition to find its refuge path clearing entities
     * @param partitionsRefugePathMap A map of each Partition to the path of the center posiion of that path to its nearest refuge
     * @return Set of entity areas to be cleared
     */
    public Set<EntityID> findRefugePathsToClearInPartition(Partition partition, Map<EntityID, Set<EntityID>> partitionsRefugePathMap) {
        Set<EntityID> pathsToClear = new FastSet<EntityID>();
        for (Set<EntityID> list : partitionsRefugePathMap.values()) {
            for (EntityID entityID : list) {
                if (!pathsToClear.contains(entityID) && partition.containsInEntities(entityID)) {
                    pathsToClear.add(entityID);
                }
            }
        }

        return pathsToClear;
    }
}
