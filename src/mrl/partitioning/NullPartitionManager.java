package mrl.partitioning;

import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.util.*;


/**
 * This partition manager simply logs entries and does nothing. Useful for situations where partition manager is not needed.
 *
 * @author Sam
 * @version 1.0
 */
public class NullPartitionManager implements IPartitionManager {
    private static final Log logger = LogFactory.getLog(NullPartitionManager.class);
    private MrlWorld world;

    public NullPartitionManager(MrlWorld world) {
        this.world = world;
    }


    @Override
    public void initialise() {
        logger.debug("initialize() is called.");
    }

    @Override
    public void update() {
        logger.debug("update() is called.");
    }

    @Override
    public List<Partition> split(Partition partition, int n) {
        logger.debug("split(Partition, int) is called.");
        return new ArrayList<Partition>();
    }

    /**
     * Fills partition fields with proper entities
     *
     * @param agents
     */
    @Override
    public void fillPartitions(int agents) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updatePartitions(int agents) {
        logger.debug("updatePartitions(int) is called.");
    }

    @Override
    public void updateAssignment(List<StandardEntity> agents) {
        logger.debug("updateAssignment(List) is called.");
    }

    @Override
    public Partition findHumanPartition(Human human) {
        logger.debug("findHumanPartitions(Human) is called.");
        return null;
    }

    @Override
    public Set<Partition> findHumanPartitionsMap(Human human) {
        return null;
    }

    @Override
    public List<Partition> getPartitions() {
        logger.debug("getPartitions() is called.");
        return new ArrayList<Partition>();
    }

    @Override
    public List<Partition> getUnassignedPartitions() {
        logger.debug("getUnassignedPartitions() is called.");
        return new ArrayList<Partition>();
    }

    @Override
    public Partition findPartition(EntityID partitionID) {
        logger.debug("findPartition(EntityID) is called.");
        return null;
    }

    @Override
    public Set<Map.Entry<Human, Partition>> getHumanPartitionEntrySet() {
        logger.debug("getHumanPartitionEntrySet() is called.");
        return new HashSet<Map.Entry<Human, Partition>>();
    }

    @Override
    public Set<Partition> getMyPartitions() {
        logger.debug("getMyPartitions() is called.");
        return null;
    }

    @Override
    public Partition findPartitionAtArea(EntityID areaId) {
        logger.debug("findPartitionAtArea(EntityID) is called.");
        return null;
    }

    @Override
    public void forceAssignAgent(Human platoonAgent, Partition partition) {
        logger.debug("forceAssignAgent(Human, Partition) is called.");
    }

    @Override
    public ChangeSet preprocessChanges(ChangeSet changeSet) {
        return changeSet;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
