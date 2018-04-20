package mrl.helper;

import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Siavash
 */
public class BuildingHelper implements IHelper {
    @Override
    public void init() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns a list of {@link rescuecore2.standard.entities.Road} containing roads that ends to {@code building}
     *
     * @param world
     * @param building building to find entrance roads
     * @return List of entrance roads
     * @author Siavash
     */
    public static List<Road> getEntranceRoads(MrlWorld world, Building building) {
        ArrayList<Road> entranceRoads = new ArrayList<Road>();
        MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());
        for(Entrance entrance : mrlBuilding.getEntrances()){
            entranceRoads.add(entrance.getNeighbour());
        }
        return entranceRoads;


        // throw new NotImplementedException();
    }

    public static List<Area> getEntranceAreas(MrlWorld world, Building building) {
        ArrayList<Area> entranceAreas = new ArrayList<Area>();
        if (building != null && building.getNeighbours() != null) {
            for (EntityID entityID : building.getNeighbours()) {
                Area area = (Area) world.getEntity(entityID);
                entranceAreas.add(area);
            }
        }
        return entranceAreas;
    }

    /**
     * check is this building have fieriness 1,2,3,6,7,8 or not!
     * this building probably have no alive human!
     *
     * @param building building that want know have this condition or not
     * @return answer
     */
    public static boolean hasPossibleAliveHuman(Building building) {
        return (building.isFierynessDefined() && !(building.getFieryness() == 0 || building.getFieryness() == 4 || building.getFieryness() == 5));
    }


}
