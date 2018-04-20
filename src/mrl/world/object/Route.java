package mrl.world.object;

import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by RescueSim on 7/4/2015.
 */
public class Route {

    private List<EntityID> routEntities;

    public Route(List<EntityID> routEntities) {
        this.routEntities = new ArrayList<>(routEntities);
    }

    public List<EntityID> getRoutEntities() {
        return routEntities;
    }

    public void setRoutEntities(List<EntityID> routEntities) {
        this.routEntities = new ArrayList<>(routEntities);
    }
}
