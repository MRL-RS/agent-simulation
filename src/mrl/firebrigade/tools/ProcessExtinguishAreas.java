package mrl.firebrigade.tools;

import mrl.MrlPersonalData;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/4/13
 * Time: 5:20 PM
 * Author: Mostafa Movahedi
 */

/**
 *
 */
public class ProcessExtinguishAreas {
    private MrlFireBrigadeWorld world;
    private Map<EntityID, List<EntityID>> extinguishableFromAreasMap;
    private Map<EntityID, List<MrlBuilding>> buildingsInExtinguishRangeMap;

    public ProcessExtinguishAreas(MrlFireBrigadeWorld world) {
        this.world = world;
    }

    public void process() {
//        Long start = System.currentTimeMillis();
//        String mapName = world.getMapName();
//        if (mapName == null) {
//            mapName = Long.toString(world.getUniqueMapNumber());
//        }
//        String befFileName = MRLConstants.PRECOMPUTE_DIRECTORY + mapName + ".bef";
//        String berFileName = MRLConstants.PRECOMPUTE_DIRECTORY + mapName + ".ber";

        extinguishableFromAreasMap = new HashMap<EntityID, List<EntityID>>();
        buildingsInExtinguishRangeMap = new HashMap<EntityID, List<MrlBuilding>>();

//        if (new File(befFileName).exists() && new File(berFileName).exists()) {
//            try {
//                readBEF(befFileName);
//                readBER(berFileName);
//            } catch (Exception e) {
//                System.err.println("Cannot load EXTINGUISH data!!!!   " + e.getMessage());
//            }
//        }
        fillMaps();
//        try {
//            createBEF(befFileName);
//            createBER(berFileName);
//        } catch (IOException e) {
//            System.err.println("Cannot create EXTINGUISH data!!!!   " + e.getMessage());
//        }
//        System.out.println(System.currentTimeMillis() - start);

    }

    private void fillMaps() {
        for (MrlBuilding mrlBuilding : world.getMrlBuildings()) {
            List<EntityID> extinguishableFromAreas = FireBrigadeUtilities.findAreaIDsInExtinguishRange(world, mrlBuilding.getID());
            List<MrlBuilding> buildingsInExtinguishRange = new ArrayList<MrlBuilding>();
            for (EntityID next : extinguishableFromAreas) {
                if (world.getEntity(next) instanceof Building) {
                    buildingsInExtinguishRange.add(world.getMrlBuilding(next));
                }
            }
            mrlBuilding.setExtinguishableFromAreas(extinguishableFromAreas);
            extinguishableFromAreasMap.put(mrlBuilding.getID(), extinguishableFromAreas);
            mrlBuilding.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
            buildingsInExtinguishRangeMap.put(mrlBuilding.getID(), buildingsInExtinguishRange);
        }
        for (MrlRoad mrlRoad : world.getMrlRoads()) {
            List<MrlBuilding> buildingsInExtinguishRange = FireBrigadeUtilities.findBuildingsInExtinguishRangeOf(world, mrlRoad.getID());
            mrlRoad.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
            buildingsInExtinguishRangeMap.put(mrlRoad.getID(), buildingsInExtinguishRange);
        }

        MrlPersonalData.VIEWER_DATA.setExtinguishData(extinguishableFromAreasMap, buildingsInExtinguishRangeMap);
    }
/*

    private Set<EntityID> readBEF(String fileName) throws IOException {
        Set<EntityID> borderBuildings = new HashSet<EntityID>();
        File f = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String nl;
        while (null != (nl = br.readLine())) {
            Integer key = Integer.parseInt(nl);
            EntityID entityID = new EntityID(key);
            MrlBuilding mrlBuilding = world.getMrlBuilding(entityID);

            nl = br.readLine();
            String[] ids = nl.split(",");
            List<EntityID> extinguishableFromAreas = new ArrayList<EntityID>();
            for (String id : ids) {
                extinguishableFromAreas.add(new EntityID(Integer.parseInt(id)));
            }
            extinguishableFromAreasMap.put(entityID, extinguishableFromAreas);
            mrlBuilding.setExtinguishableFromAreas(extinguishableFromAreas);
        }
        br.close();
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("Read from file:" + fileName);
        }
        return borderBuildings;
    }

    private void createBEF(String fileName) throws IOException {
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("  Creating BEF Files .... ");
        }

        File f = new File(fileName);
        f.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));

        for (EntityID key : extinguishableFromAreasMap.keySet()) {
            bw.write(key.getValue() + "\n");
            String values = "";
            for (EntityID from : extinguishableFromAreasMap.get(key)) {
                values += "," + from.getValue();
            }
            bw.write(values.substring(1) + "\n");
        }
        bw.close();
    }

    private Set<EntityID> readBER(String fileName) throws IOException {
        Set<EntityID> borderBuildings = new HashSet<EntityID>();
        File f = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String nl;
        while (null != (nl = br.readLine())) {
            Integer key = Integer.parseInt(nl);
            EntityID entityID = new EntityID(key);

            nl = br.readLine();
            List<MrlBuilding> buildingsInExtinguishRange = new ArrayList<MrlBuilding>();
            if (nl.length() > 0) {
                String[] ids = nl.split(",");
                for (String id : ids) {
                    EntityID bId = new EntityID(Integer.parseInt(id));
                    buildingsInExtinguishRange.add(world.getMrlBuilding(bId));
                }
            }
            buildingsInExtinguishRangeMap.put(entityID, buildingsInExtinguishRange);
            StandardEntity entity = world.getEntity(entityID);
            if (entity instanceof Building) {
                MrlBuilding mrlBuilding = world.getMrlBuilding(entityID);
                mrlBuilding.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
            } else if (entity instanceof Road) {
                MrlRoad mrlRoad = world.getMrlRoad(entityID);
                mrlRoad.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
            }
        }
        br.close();
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("Read from file:" + fileName);
        }
        return borderBuildings;
    }

    private void createBER(String fileName) throws IOException {
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("  Creating BER Files .... ");
        }

        File f = new File(fileName);
        f.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));

        for (EntityID key : buildingsInExtinguishRangeMap.keySet()) {
            bw.write(key.getValue() + "\n");
            String values = "";
            for (MrlBuilding inRange : buildingsInExtinguishRangeMap.get(key)) {
                values += "," + inRange.getID().getValue();
            }
            if (values.length() > 0) {
                bw.write(values.substring(1));
            }
            bw.write("\n");
        }
        bw.close();
    }
*/

}
