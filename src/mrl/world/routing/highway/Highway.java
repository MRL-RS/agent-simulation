package mrl.world.routing.highway;

import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;

/**
 * Created by Mostafa Shabani.
 * Date: Feb 10, 2011
 * Time: 7:00:57 PM
 */
public class Highway extends ArrayList<Road> {
    private EntityID id;

    public Highway(EntityID id) {
        this.id = id;
    }

    public EntityID getId() {
        return id;
    }
}
