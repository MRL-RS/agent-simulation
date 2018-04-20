package mrl.partitioning.segmentation;

import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/16/12
 * Time: 7:17 PM
 */
public class EntityCluster {
    private List<StandardEntity> entities;
    private Pair<Integer, Integer> center;


    public EntityCluster(MrlWorld world, StandardEntity entity) {
        add(world, entity);
    }

    public EntityCluster(MrlWorld world, List<StandardEntity> entities) {
        this.entities = entities;
        computeCenter(world);
    }

    public EntityCluster(MrlWorld world, Set<StandardEntity> entities) {
        this.entities = new ArrayList<StandardEntity>();
        this.entities.addAll(entities);
        computeCenter(world);
    }


    public void add(MrlWorld world, StandardEntity entity) {
        if (entities == null) {
            entities = new ArrayList<StandardEntity>();
        }
        entities.add(entity);
        computeCenter(world);
    }

    public List<StandardEntity> getEntities() {
        return entities;
    }

    public void eat(MrlWorld world, EntityCluster cluster) {
        entities.addAll(cluster.getEntities());
        computeCenter(world);
    }


    public Pair<Integer, Integer> getCenter() {
        return center;
    }

    private void computeCenter(MrlWorld world) {
        int sumX = 0;
        int sumY = 0;
        Pair<Integer, Integer> location;
        if (entities == null || entities.isEmpty()) {
            //do nothing
        } else {
            int size = entities.size();
            for (StandardEntity entity : entities) {
                location = entity.getLocation(world);
                sumX += location.first();
                sumY += location.second();
            }
            center = new Pair<Integer, Integer>(sumX / size, sumY / size);
        }
    }

}
