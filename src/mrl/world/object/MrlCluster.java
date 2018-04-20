package mrl.world.object;

import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Sajjad Salehi
 * Date: 12/22/11
 * Time: 5:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class MrlCluster {
    protected List<EntityID> buildingIDS;
    protected List<MrlBuilding> buildings;

    public MrlCluster() {
        buildingIDS = new ArrayList<EntityID>();
        buildings = new ArrayList<MrlBuilding>();
    }

    public MrlCluster(List<MrlBuilding> blds) {
        buildings = new ArrayList<MrlBuilding>(blds);
        for (MrlBuilding building : blds)
            buildingIDS.add(building.getID());
    }

    public MrlCluster(MrlBuilding b) {
        buildings = new ArrayList<MrlBuilding>();
        buildings.add(b);
        buildingIDS.add(b.getID());
    }

    public void addBuilding(MrlBuilding b) {
        buildings.add(b);
        buildingIDS.add(b.getID());
    }

    public void removeBuilding(MrlBuilding b) {
        buildings.remove(b);
        buildingIDS.remove(b.getID());
    }

    public List<MrlBuilding> getBuildings() {
        return buildings;
    }

    public List<EntityID> getBuildingIDs() {
        return buildingIDS;
    }
}
