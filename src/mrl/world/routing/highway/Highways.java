package mrl.world.routing.highway;

import javolution.util.FastMap;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Mostafa Shabani.
 * Date: Feb 10, 2011
 * Time: 7:01:27 PM
 */
public class Highways extends ArrayList<Highway> {


    Map<EntityID, EntityID> roadHighWayMap = new FastMap<EntityID, EntityID>();

    public void initRoadHighwayMap() {
        EntityID id;
        for (Highway highway : this) {
            id = highway.getId();
            for (Road road : highway) {
                roadHighWayMap.put(road.getID(), id);
            }
        }
    }

    public boolean isContains(EntityID id) {
        return (roadHighWayMap.get(id) != null);
    }
}
