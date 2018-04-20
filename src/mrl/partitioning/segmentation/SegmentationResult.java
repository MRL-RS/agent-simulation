package mrl.partitioning.segmentation;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class stores the result of Segmentation
 *
 * @Author Pooya Deldar Gohardani
 * @see SegmentType
 * @see ISegmentation
 *      <p/>
 *      Date: 5/15/12
 *      Time: 4:47 PM
 */
public class SegmentationResult {

    private List<EntityCluster> entityClusters;
    private List<Polygon> polygons;

    public SegmentationResult(List segments, SegmentType segmentType) {
        switch (segmentType) {
            case ENTITY_CLUSTER:
                entityClusters = new ArrayList<EntityCluster>(segments);
                break;
            case POLYGON:
                polygons = new ArrayList<Polygon>(segments);
                break;
        }
    }

    public boolean isEntityClustersDefined() {
        return entityClusters != null;
    }

    public boolean isPolygonsDefined() {
        return polygons != null;
    }


    public List<EntityCluster> getEntityClusters() {
        return entityClusters;
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }
}