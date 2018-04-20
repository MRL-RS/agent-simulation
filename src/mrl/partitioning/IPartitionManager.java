package mrl.partitioning;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 2/12/12
 *         Time: 6:02 PM
 */
public interface IPartitionManager {

    /**
     * Prepares partition manager for the first time
     */
    public void initialise();


    /**
     * Update partition manager, means, updates everything related to partition manager like Update calling
     * {@code updatePartitions} and other things
     */
    public void update();

    /**
     * Split specified partition to n sub partitions
     *
     * @param partition The specified partition to split
     * @param n         number of splitting
     * @return List of split partitions
     */
    public List<Partition> split(Partition partition, int n);


    /**
     * Fills partition fields with proper entities
     * @param agents
     */
    public void fillPartitions(int agents);


    /**
     * updates partitions features
     *
     * @param agents number of available agents
     */
    public void updatePartitions(int agents);

    /**
     * Update assignments of the specified agents according to the current partitioning.
     *
     * @param agents agents for which assignments are to be updated
     */
    public void updateAssignment(List<StandardEntity> agents);

    /**
     * Finds assigned partition of the provided {@code human}.
     *
     * @param human Human for which this method finds the assigned partition
     * @return The assigned partition or {@code null} if the specified {@code human} is not assigned to any partition.
     */
    public Partition findHumanPartition(Human human);

    /**
     * Finds assigned partitions of the provided {@code human}.
     *
     * @param human Human for which this method finds the assigned partition
     * @return List of the assigned partitions or {@code null} if the specified {@code human} is not assigned to any partition.
     */
    public Set<Partition> findHumanPartitionsMap(Human human);

    /**
     * Getter method for partitions.
     *
     * @return partitions this {@link IPartitionManager} created
     */
    public List<Partition> getPartitions();


    /**
     * Getter method for partitions with no assigned agents
     *
     * @return list of unassigned partitions
     */
    public List<Partition> getUnassignedPartitions();

    /**
     * Finds a partition with the specified {@link EntityID}.
     *
     * @param partitionID {@link EntityID} of the desired partition
     * @return A Partition with the specified {@link EntityID} or null.
     */
    public Partition findPartition(EntityID partitionID);

    /**
     * Gives access to Human-Partition map in Entry Set format for read-only access.
     *
     * @return delegates to {@link java.util.Map#entrySet()} of Human-Partition map
     */
    public Set<Map.Entry<Human, Partition>> getHumanPartitionEntrySet();


    /**
     * Current partitions of the running agent
     *
     * @return set of <code>Partition</code>
     */
    Set<Partition> getMyPartitions();


    /**
     * Finds the partitions among the specified {@code partitions} including the specified area.
     *
     * @param areaId ID of the Area (Road or Building) for which we need to find the partitions
     * @return The partitions which includes the specified area or {@code null} if no partitions includes the specified area.
     */
    public Partition findPartitionAtArea(EntityID areaId);

    /**
     * (Optional) <br/>
     * After partitioning is done and agents are assigned to partitions, if a new agent is available, this method
     * can forcefully assign it to the specified {@code partition}.<br/>
     *
     * @param platoonAgent Agent which is needed to be assigned to {@code partition}
     * @param partition    Partition to which {@code platoonAgent} should be assigned
     */
    public void forceAssignAgent(Human platoonAgent, Partition partition);

    /**
     * Makes sure changes are taken into account for partitioning
     *
     * @param changeSet raw change set
     * @return altered change set
     */
    public ChangeSet preprocessChanges(ChangeSet changeSet);
}
