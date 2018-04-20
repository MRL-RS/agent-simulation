package mrl.partitioning.segmentation;

import javolution.util.FastMap;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/16/12
 * Time: 12:33 PM
 */
public abstract class ClusteringSegmentation extends Segmentation<EntityCluster> {

    protected Map<EntityID, EntityCluster> entityClusterMap = new FastMap<EntityID, EntityCluster>();
    protected List<EntityCluster> entityClusters = new ArrayList<EntityCluster>();


    public ClusteringSegmentation(MrlWorld world) {
        super(world);
    }

    @Override
    public abstract SegmentationResult split(EntityCluster entityCluster, int n);


    protected void addToClusterSet(EntityCluster cluster, EntityID entityID) {
        entityClusterMap.put(entityID, cluster);
        entityClusters.add(cluster);
    }


    /**
     * merge new cluster to others and replace the result with all others
     *
     * @param adjacentClusters adjacent clusters to the new cluster
     * @param cluster          new constructed cluster
     * @param entityID
     */
    protected void merge(Set<EntityCluster> adjacentClusters, EntityCluster cluster, EntityID entityID) {

        for (EntityCluster c : adjacentClusters) {
            cluster.eat(world, c);

            // refreshing EntityClusterMap
            for (StandardEntity entity : c.getEntities()) {
                entityClusterMap.remove(entity.getID());
                entityClusterMap.put(entity.getID(), cluster);
            }
            entityClusters.remove(c);
        }

        addToClusterSet(cluster, entityID);
    }
}
