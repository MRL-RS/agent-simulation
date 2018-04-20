package mrl.platoon.simpleSearch;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class DistanceInterface {
    private StandardWorldModel world;

    public DistanceInterface(StandardWorldModel world) {
        this.world = world;
    }

    /**
     * Computes the distance between two entities in the simulation world.
     *
     * @param id1 id of the first entity.
     * @param id2 id of the second entity.
     * @return euclidean distance in mm(?) or -1 if one of the entities does not exist.
     */
    public int getDistance(EntityID id1, EntityID id2) {
        return world.getDistance(id1, id2);
    }
}

