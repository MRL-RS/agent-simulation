package mrl.world.object.mrlZoneEntity;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: 5/13/11
 * Time: 6:20 PM
 */
public class MrlZones extends ArrayList<MrlZone> {
    Map<EntityID, MrlZone> buildingZoneMap = new FastMap<EntityID, MrlZone>();
    List<MrlZone> burningMrlZones = new ArrayList<MrlZone>();
    private double maximumTotalArea;
    private double zonesBuildingsCountAverage = 0;
    private double maximumZoneBuildingCount = 0;
    Random random;

    Stack<MrlZone> extinguishedZones = new Stack<MrlZone>();
    private List<Road> exploringEntrances = new ArrayList<Road>();

    //added by Vahid Hooshangi
    Map<MrlZone, Integer> CivZone = new FastMap<MrlZone, Integer>();
    Set<Civilian> civilianSet = new FastSet<Civilian>();
    Map<MrlZone, Set<Building>> ridgeBuilding = new FastMap<MrlZone, Set<Building>>();
    //


    public MrlZones(Random random) {
        this.random = random;
    }

    public void update() {
//        List<MrlZone> previousBurningZones = new ArrayList<MrlZone>();
//        previousBurningZones.addAll(burningMrlZones);

        burningMrlZones.clear();

        for (MrlZone zone : this) {
//            zone.update(tempBurningZones);
            zone.update();
//            if (zone.getValue() > 100000000000000000l) {
//                burningMrlZones.add(zone);
//            }
            if (zone.onFire && !burningMrlZones.contains(zone)) {
                burningMrlZones.add(zone);
            }

        }

//        previousBurningZones.removeAll(burningMrlZones);
//        extinguishedZones.addAll(previousBurningZones);

//        tempBurningZones.clear();
//        int i = 0;
        for (MrlZone zone : this) {
            zone.updateGlobalValues();
        }

        List<MrlZone> toRemove = new ArrayList<MrlZone>();
        Collections.sort(burningMrlZones);

//        this.get(0).getWorld().printData("--------------------------------------------------------");
        for (MrlZone zone : burningMrlZones) {
//            System.out.println(" i="+(++i)+" "+zone.toString());

            if (zone.isEmpty() || !zone.onFire) {
                toRemove.add(zone);
            }
        }
        burningMrlZones.removeAll(toRemove);
    }

    public void fillZoneAroundObjects() {
        zonesBuildingsCountAverage = 0;
        for (MrlZone zone : this) {
            zonesBuildingsCountAverage += zone.size();
//            System.out.println(zone.getNeighborZoneIds());
            for (int ai : zone.getNeighborZoneIds()) {
                MrlZone az = getZone(ai);
                if (az == null)
                    throw new RuntimeException("MrlZone not found.");
                else
                    zone.addNeighbor(az);
            }
            if (maximumTotalArea < zone.getTotalArea()) {
                maximumTotalArea = zone.getTotalArea();
            }
            if (maximumZoneBuildingCount < zone.size()) {
                maximumZoneBuildingCount = zone.size();
            }

        }
        zonesBuildingsCountAverage /= (double) size();
    }

//    public void createBuildingMap() {
//        for (MrlZone zone : this) {
//            for (MrlBuilding building : zone) {
//                buildingZoneMap.put(building.getSelfBuilding().getID(), zone);
//            }
//        }
//    }

    public void addBuildingZoneMap(EntityID id, MrlZone zone) {
        buildingZoneMap.put(id, zone);
    }

    public MrlZone getZone(int index) {
        for (MrlZone zone : this) {
            if (zone.id == index)
                return zone;
        }
        return null;
    }

    public List<MrlZone> getBurningZones() {
        return burningMrlZones;
    }

    public MrlZone getBuildingZone(EntityID id) {
        return buildingZoneMap.get(id);
    }

    public double getMaximumTotalArea() {
        return maximumTotalArea;
    }

    public Map<MrlZone, Integer> getCivZone() {
        return CivZone;
    }

    public void setCivZone(MrlZone zoneEntity, int count) {
        if (CivZone.get(zoneEntity) != null) {
            int num = CivZone.get(zoneEntity);
            CivZone.put(zoneEntity, num + count);
        } else {
            CivZone.put(zoneEntity, count);
        }
    }

    public boolean setCivilianSet(Civilian civilian) {
        if (civilianSet.add(civilian)) {
            return true;
        } else {
            return false;
        }
    }

    public void setRidgeBuilding(MrlWorld world) {
        for (MrlZone zoneEntity : this) {
            Set<Building> setBuilding = new FastSet<Building>();
            for (MrlBuilding building : zoneEntity) {
                for (StandardEntity standardEntity : world.getObjectsInRange(building.getSelfBuilding(), 5000)) {
                    if (standardEntity instanceof Road) {
                        setBuilding.add(building.getSelfBuilding());
                    }
                }
            }

            ridgeBuilding.put(zoneEntity, new FastSet<Building>(setBuilding));

            setBuilding.clear();
        }
    }

    public Map<MrlZone, Set<Building>> getRidgeBuilding() {
        return ridgeBuilding;
    }

    private List<MrlBuilding> getSomeExploringBuildings(MrlZone zoneEntity) {
        List<MrlBuilding> buildings = new ArrayList<MrlBuilding>();
        for (MrlBuilding building : zoneEntity) {
            if (building.getEstimatedFieryness() == 0) {
                buildings.add(building);
            }
        }
        return buildings;
    }

    public void updateExploringEntrance(MrlFireBrigadeWorld world) {
        if (exploringEntrances.isEmpty()) {
            if (!extinguishedZones.isEmpty()) {
                MrlZone zone = extinguishedZones.pop();
                List<MrlBuilding> someExploringBuildings = getSomeExploringBuildings(zone);

                int neededRandomExploreBuildings = (int) Math.ceil(someExploringBuildings.size() * 0.56f);
                List<Road> availableEntrances = new ArrayList<Road>();

                for (MrlBuilding building : someExploringBuildings) {
                    for (Entrance entrance : building.getEntrances()) {
                        if (!world.getRoadHelper().isSeenAndBlocked(building.getID(), entrance.getNeighbour().getID()) && !exploringEntrances.contains(entrance.getNeighbour())) {
                            availableEntrances.add(entrance.getNeighbour());
                        }
                    }
                }

                if (!availableEntrances.isEmpty()) {
                    Road entrance;
                    for (int i = 0; i < neededRandomExploreBuildings && availableEntrances.size() > 0; i++) {
                        entrance = availableEntrances.get(random.nextInt(availableEntrances.size()));
                        exploringEntrances.add(entrance);
                        availableEntrances.remove(entrance);
                    }
                }
            }
        }
    }

    public List<Road> getExploringEntrances() {
        return exploringEntrances;
    }

    public void pushExtinguishedZones(MrlZone extinguishedZone) {
        if (!extinguishedZones.contains(extinguishedZone)) {
            this.extinguishedZones.push(extinguishedZone);
        }
    }

    public double getZonesBuildingsCountAverage() {
        return zonesBuildingsCountAverage;
    }

    public double getMaximumZoneBuildingCount() {
        return maximumZoneBuildingCount;
    }
}
