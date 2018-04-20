package mrl.partitioning.segmentation;

import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//TODO @Pooya Complete the javadoc, please.

/**
 * This thread-singleton class is responsible to handle ??? for ??? this way: ...???
 * <br/>Assuming each agent runs inside its own thread, this type of singleton is only unique within thread.
 *
 * @author Pooya Deldar Gohardani
 *         <p/>
 *         Date: 5/11/12
 *         Time: 4:42 PM
 */
public class KMeansClustering extends ClusteringSegmentation {

    /**
     * <b>Note:</b> Once this method is called with a {@code world}, it should always be called with the same reference in the same thread.
     *
     * @param world The MrlWorld reference to which this Class will be bound.
     * @return Thread-singleton instance of {@link KMeansClustering}.
     * @throws IllegalArgumentException When a different MrlWorld reference is passed to obtain the instance.
     */
    public static KMeansClustering getThreadInstance(MrlWorld world) {
        KMeansClustering threadInstance = ThreadLocalSingletonAccessor.getThreadInstance();
        if (threadInstance == null) {
            threadInstance = new KMeansClustering(world);
            ThreadLocalSingletonAccessor.setThreadInstance(threadInstance);
        }
        if (world != threadInstance.world) {
            throw new IllegalArgumentException("Invalid MrlWorld reference passed to KMeansClustering.getThreadInstance(...) method. " +
                    "Thread Instance was initialized with another MrlWorld instance.");
        }
        return threadInstance;
    }

    private static class ThreadLocalSingletonAccessor extends ThreadLocal<KMeansClustering> {
        private static ThreadLocalSingletonAccessor threadlocalSingletonAccessor
                = new ThreadLocalSingletonAccessor();

        @Override
        public void set(KMeansClustering object) {
            super.set(object);
        }

        @Override
        public KMeansClustering get() {
            return super.get();
        }

        public static void setThreadInstance(KMeansClustering object) {
            threadlocalSingletonAccessor.set(object);
        }

        public static KMeansClustering getThreadInstance() {
            return threadlocalSingletonAccessor.get();
        }
    }


    private java.util.List<Point> clusterCenterPoints;

    private KMeansClustering(MrlWorld world) {
        super(world);
    }

    @Override
    public SegmentationResult split(EntityCluster entityCluster, int n) {
        clusterCenterPoints = new ArrayList<Point>();
//        java.util.List<MrlBuilding> buildingList = partition.getBuildings();


        Double[][] data = new Double[entityCluster.getEntities().size()][2];

        Map<Double[], StandardEntity> entityMap = new FastMap<Double[], StandardEntity>();
        SegmentationResult segmentationResult;
        Pair<Integer, Integer> location;

        int i = 0;
        for (StandardEntity entity : entityCluster.getEntities()) {
            location = entity.getLocation(world);
            data[i][0] = location.first().doubleValue();
            data[i][1] = location.second().doubleValue();
            entityMap.put(data[i], entity);
            i++;
        }

        int tryCount = 0;
        if (world.isMapHuge()) {
            tryCount = 10;
        } else {
            tryCount = 10;
        }

        KmeansI kmeans;
//        if (isOriginalKmeans) {
//            kmeans = new Kmeans_Original(data, n);
//
//        } else {
        kmeans = new Kmeans_Modified(data, n, tryCount);
//        }
        kmeans.calculateClusters();
        Double[][] centers = kmeans.getClusterCenters();


        for (Double[] center : centers) {
            clusterCenterPoints.add(new Point(center[0].intValue(), center[1].intValue()));
        }
        ArrayList<Double[]>[] clusters = kmeans.getClusters();

        List<EntityCluster> entityClusters = new ArrayList<EntityCluster>();
        List<StandardEntity> entities;
        for (ArrayList<Double[]> cluster : clusters) {
            entities = new ArrayList<StandardEntity>();
            if (cluster.isEmpty()) {
                continue;
            }
            for (Double[] clusterData : cluster) {
                entities.add(entityMap.get(clusterData));
            }
            entityClusters.add(new EntityCluster(world, entities));
        }


        segmentationResult = new SegmentationResult(entityClusters, SegmentType.ENTITY_CLUSTER);

        MrlPersonalData.VIEWER_DATA.setLayerPut(world.getSelf().getID(), clusters, clusterCenterPoints);
        return segmentationResult;
    }


    //TODO @Pooya What's the name of this method?
    private java.util.List<java.util.List<Point>> arrayToList(ArrayList[] clusters) {
        java.util.List<java.util.List<Point>> points = new ArrayList<java.util.List<Point>>();

        for (ArrayList cluster : clusters) {
            List<Point> pointList = new ArrayList<Point>();
            for (Object aCluster : cluster) {
                Double[] elem = (Double[]) aCluster;
                pointList.add(new Point(elem[0].intValue(), elem[1].intValue()));
            }
            points.add(pointList);
        }

        return points;
    }

}
