package mrl.partitioning.segmentation;

import mrl.common.Util;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;

import java.util.List;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 5:15 PM
 */
public class SimpleDistanceBasedClustering extends ClusteringSegmentation {


    private int CLUSTER_RANGE_THRESHOLD;


    public SimpleDistanceBasedClustering(MrlWorld world) {
        super(world);
        int range;
        if (world.getSelfHuman() instanceof PoliceForce) {
            range = (int) (world.getClearDistance() * 1.5);
        } else {
            range = world.getViewDistance();
        }


        if (world.isMapHuge()) {
            CLUSTER_RANGE_THRESHOLD = range * 2;
        } else {
            CLUSTER_RANGE_THRESHOLD = range;
        }
    }


    /**
     * <b>Note:</b> Once this method is called with a {@code world}, it should always be called with the same reference in the same thread.
     *
     * @param world The MrlWorld reference to which this Class will be bound.
     * @return Thread-singleton instance of {@link SimpleDistanceBasedClustering}.
     * @throws IllegalArgumentException When a different MrlWorld reference is passed to obtain the instance.
     */
    public static SimpleDistanceBasedClustering getThreadInstance(MrlWorld world) {
        SimpleDistanceBasedClustering threadInstance = ThreadLocalSingletonAccessor.getThreadInstance();
        if (threadInstance == null) {
            threadInstance = new SimpleDistanceBasedClustering(world);
            ThreadLocalSingletonAccessor.setThreadInstance(threadInstance);
        }
        if (world != threadInstance.world) {
            throw new IllegalArgumentException("Invalid MrlWorld reference passed to KMeansClustering.getThreadInstance(...) method. " +
                    "Thread Instance was initialized with another MrlWorld instance.");
        }
        return threadInstance;
    }

    private static class ThreadLocalSingletonAccessor extends ThreadLocal<SimpleDistanceBasedClustering> {
        private static ThreadLocalSingletonAccessor threadlocalSingletonAccessor
                = new ThreadLocalSingletonAccessor();

        @Override
        public void set(SimpleDistanceBasedClustering object) {
            super.set(object);
        }

        @Override
        public SimpleDistanceBasedClustering get() {
            return super.get();
        }

        public static void setThreadInstance(SimpleDistanceBasedClustering object) {
            threadlocalSingletonAccessor.set(object);
        }

        public static SimpleDistanceBasedClustering getThreadInstance() {
            return threadlocalSingletonAccessor.get();
        }
    }

    @Override
    public SegmentationResult split(EntityCluster mainCluster, int n) {

        SegmentationResult segmentationResult;
        EntityCluster tempCluster;

        for (StandardEntity entity : mainCluster.getEntities()) {
            tempCluster = findNearestInRageCluster(entityClusters, entity);
            if (tempCluster == null) {
                tempCluster = new EntityCluster(world, entity);
                addToClusterSet(tempCluster, entity.getID());
            } else {
                tempCluster.add(world, entity);
            }
        }
        segmentationResult = new SegmentationResult(entityClusters, SegmentType.ENTITY_CLUSTER);
        return segmentationResult;

    }


    /**
     * This method finds nearest cluster within all clusters to the {@code entity}.
     * <br/>
     * <br/>
     * <b>Note: </b> Nearest cluster must be in a specific distance which is determined by {@code CLUSTER_RANGE_THRESHOLD}
     *
     * @param entityClusters List of existing clusters
     * @param entity         The entity the method want to find nearest cluster to it.
     * @return
     */
    private EntityCluster findNearestInRageCluster(List<EntityCluster> entityClusters, StandardEntity entity) {
        EntityCluster nearestInRangeCluster = null;
        if (entityClusters == null || entityClusters.isEmpty()) {
            //do nothing
        } else {
            int minDistance = CLUSTER_RANGE_THRESHOLD;
            int tempDistance;
            for (EntityCluster cluster : entityClusters) {
                tempDistance = Util.distance(entity.getLocation(world), cluster.getCenter());
                if (tempDistance < minDistance) {
                    minDistance = tempDistance;
                    nearestInRangeCluster = cluster;
                }
            }
        }

        return nearestInRangeCluster;
    }
}
