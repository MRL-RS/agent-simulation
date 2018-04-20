package mrl.firebrigade;

import javolution.util.FastMap;
import mrl.LaunchMRL;
import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.firebrigade.simulator.Simulator;
import mrl.firebrigade.tools.ProcessAreaVisibility;
import mrl.helper.HumanHelper;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.object.FireClusters;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: roohi
 * Date: 2/17/11
 * Time: 7:40 PM
 */
public class MrlFireBrigadeWorld extends MrlWorld {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlFireBrigadeWorld.class);
    protected RoadHelper roadHelper;
    protected Simulator simulator;
    //    private WaterCoolingEstimator coolingEstimator;
    private Map<EntityID, EntityID> gotoMap = new FastMap<EntityID, EntityID>();

    //----------------- connection value ---------------
    private boolean isPolyLoaded;
    private float rayRate = 0.0025f;
    //--------------------------------------------------

    private int maxWater;
    private int waterRefillRate;
    private int waterRefillRateInHydrant;
    private boolean isVisibilityAreaDataLoaded;
    private boolean isBorderEntitiesDataLoaded;


    public MrlFireBrigadeWorld(StandardAgent self, Collection<? extends Entity> entities, Config config) {
        super(self, entities, config);
        createClusterManager();

        isVisibilityAreaDataLoaded = false;
        isBorderEntitiesDataLoaded = false;
        roadHelper = getHelper(RoadHelper.class);

        //----------------- connection value ---------------
        initSimulator(this);


//        coolingEstimator = new WaterCoolingEstimator();

        setMaxWater(config.getIntValue(MRLConstants.MAX_WATER_KEY));
        setWaterRefillRate(config.getIntValue(MRLConstants.WATER_REFILL_RATE_KEY, MRLConstants.WATER_REFILL_RATE));//It can not be reached from config.getIntValue(WATER_REFILL_RATE_KEY);
        setWaterRefillRateInHydrant(config.getIntValue(MRLConstants.WATER_REFILL_HYDRANT_RATE_KEY, MRLConstants.WATER_REFILL_RATE_IN_HYDRANT));

        MrlPersonalData.VIEWER_DATA.setExtinguishRange(getMaxExtinguishDistance());
        //call process area visibility
        ProcessAreaVisibility.process(this, config);
    }

    private void initSimulator(final MrlFireBrigadeWorld world) {
        Thread loader = new Thread() {
            @Override
            public void run() {
                initConnectionValues();
                simulator = new Simulator(world);
            }
        };
        loader.run();
    }

    @Override
    public void updateBeforeSense() {
        if (simulator != null) {
            simulator.update();
        }
    }

    @Override
    public void updateAfterSense() {
        super.updateAfterSense();


        try {
            fireClusterManager.updateClusters();
        } catch (Exception e) {
            e.printStackTrace();
        }


        estimatedBurningBuildings.clear();
        for (MrlBuilding mrlBuilding : getMrlBuildings()) {
            if (mrlBuilding.getEstimatedFieryness() >= 1 && mrlBuilding.getEstimatedFieryness() <= 3) {
                estimatedBurningBuildings.add(mrlBuilding);
            }
        }
//        MrlPersonalData.setCivilianClusterManager(civilianClusterManager.updateConvexHullsForViewer());
    }

    private void createClusterManager() {
        fireClusterManager = new mrl.common.clustering.FireClusterManager(this);
//        setCivilianClusterManager(new CivilianClusterManager(this));
//        policeTargetClusterManager=new PoliceTargetClusterManager(this);

    }


    //----------------- connection value ---------------
    private void initConnectionValues() {
        String fileName = MRLConstants.PRECOMPUTE_DIRECTORY + getMapName() + ".rays";
        try {
            readCND(fileName);
        } catch (Exception e) {
            if (MRLConstants.DEBUG_FIRE_BRIGADE) {
                System.err.println("Unable to load CND files");
                Logger.debug("Unable to Load CND files");
            }

            try {
//                if (LaunchMRL.shouldPrecompute) {
                createCND(fileName);
//                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void readCND(String fileName) throws IOException {
        File f = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(f));
        float rayDens = Float.parseFloat(br.readLine());
        String nl;
        while (null != (nl = br.readLine())) {
            int x = Integer.parseInt(nl);
            int y = Integer.parseInt(br.readLine());
            int quantity = Integer.parseInt(br.readLine());
            double hitRate = Double.parseDouble(br.readLine());
            List<MrlBuilding> bl = new ArrayList<MrlBuilding>();
            List<EntityID> bIDs = new ArrayList<EntityID>();
            List<Float> weight = new ArrayList<Float>();
            for (int c = 0; c < quantity; c++) {
                int ox = Integer.parseInt(br.readLine());
                int oy = Integer.parseInt(br.readLine());
                Building building = getBuildingInPoint(ox, oy);
                if (building == null) {
                    System.err.println("building not found: " + ox + "," + oy);
                    br.readLine();
                } else {
                    bl.add(getMrlBuilding(building.getID()));
                    bIDs.add(building.getID());
                    weight.add(Float.parseFloat(br.readLine()));
                }

            }
            Building b = getBuildingInPoint(x, y);
            MrlBuilding building = getMrlBuilding(b.getID());
//            buildingHelper.setConnectedBuildings(b.getID(), bl);
//            buildingHelper.setConnectedValue(b.getID(), weight);
            building.setConnectedBuilding(bl);
            building.setConnectedValues(weight);
            building.setHitRate(hitRate);
            MrlPersonalData.VIEWER_DATA.setConnectedBuildings(b.getID(), bIDs);
        }
        br.close();
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("Read from file:" + fileName);
        }
    }

    private void createCND(String fileName) throws IOException {
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("  Creating CND Files .... ");
        }

        int n = 1;
        long t1 = System.currentTimeMillis();
        long timeStart = System.currentTimeMillis();

//        System.out.println("init walls time = "+(System.currentTimeMillis()-timeStart));

        int size = getMrlBuildings().size();

        File f = new File(fileName);
//        noinspection ResultOfMethodCallIgnored
        BufferedWriter bw = null;
        if (LaunchMRL.shouldPrecompute) {
            f.createNewFile();
            bw = new BufferedWriter(new FileWriter(f));
            bw.write(rayRate + "\n");
        }

        for (MrlBuilding mrlB : getMrlBuildings()) {

            mrlB.initWallValues(this);
            if (bw != null) {
                bw.write(mrlB.getSelfBuilding().getX() + "\n");
                bw.write(mrlB.getSelfBuilding().getY() + "\n");
                bw.write(mrlB.getConnectedBuilding().size() + "\n");
                bw.write(mrlB.getHitRate() + "\n");
            }

            for (int c = 0; c < mrlB.getConnectedBuilding().size(); c++) {
                MrlBuilding building = mrlB.getConnectedBuilding().get(c);
                Float val = mrlB.getConnectedValues().get(c);
                if (bw != null) {
                    bw.write(building.getSelfBuilding().getX() + "\n");
                    bw.write(building.getSelfBuilding().getY() + "\n");
                    bw.write(val + "\n");
                }
            }
//            if (MRLConstants.DEBUG_FIRE_BRIGADE) {
//
//                long dt = System.currentTimeMillis() - t1;
//                dt = dt / n;
//                dt = dt * (size - n);
//                long sec = dt / (1000);
//                long min = (sec / 60) % 60;
//                long hour = sec / (60 * 60);
//                sec = sec % 60;
//
////                if (n % 100 == 0)
////                    System.out.println(" Time Left: " + hour + ":" + min + ":" + sec+" rayrate:"+rayRate);
//            }
            mrlB.cleanup();
        }
        if (bw != null) {
            bw.close();
        }
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
//            System.out.println("wrote CND file \"" + fileName + "\"");
            printTookTime("creating CND files", timeStart);
        }
    }

//    private void writeCND(String fileName) throws IOException {
//        File f = new File(fileName);
//        //noinspection ResultOfMethodCallIgnored
//        f.createNewFile();
//        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
//        bw.write(rayRate + "\n");
//        for (StandardEntity standardEntity : getEntities()) {
//            Building b = (Building) standardEntity;
//            bw.write(b.getX() + "\n");
//            bw.write(b.getY() + "\n");
//            bw.write(buildingHelper.getConnectedBuildings(b.getID()).size() + "\n");
//            for (int c = 0; c < buildingHelper.getConnectedBuildings(b.getID()).size(); c++) {
//                EntityID id = buildingHelper.getConnectedBuildings(b.getID()).get(c);
//                Float val = buildingHelper.getConnectedValue(b.getID()).get(c);
//                Building building = (Building) getEntity(id);
//                bw.write(building.getX() + "\n");
//                bw.write(building.getY() + "\n");
//                bw.write(val + "\n");
//            }
//        }
//        bw.close();
//        System.out.println("wrote CND file \"" + fileName + "\"");
//    }

    public static void printTookTime(String title, long start) {
        long dtTotal = System.currentTimeMillis() - start;
        long hour = dtTotal / (1000 * 60 * 60);
        dtTotal = dtTotal % (1000 * 60 * 60);
        long min = dtTotal / (1000 * 60);
        dtTotal = dtTotal % (1000 * 60);
        long sec = dtTotal / (1000);

        System.out.println(title + " took  " + (hour < 10 ? "0" + hour : hour) + ":" + (min < 10 ? "0" + min : min) + ":" + (sec < 10 ? "0" + sec : sec));
    }

//    public Point getSecureCenterOfMap() {
//        HashSet<Building> unBurnedBuildings = new HashSet<Building>();
//        for (Building b : this.getEntities()) {
//            if (b.isUnburned())
//                unBurnedBuildings.add(b);
//        }
//        int sumX = 0;
//        int sumY = 0;
//        for (Building bd : unBurnedBuildings) {
//            sumX += bd.getX();
//            sumY += bd.getY();
//        }
//        if (!unBurnedBuildings.isEmpty()) {
//            sumX /= unBurnedBuildings.size();
//            sumY /= unBurnedBuildings.size();
//        }
//        return new Point(sumX, sumY);
//    }

    public boolean isPolyLoaded() {
        return isPolyLoaded;
    }

    public void setPolyLoaded(boolean polyLoaded) {
        isPolyLoaded = polyLoaded;
    }

    public float getRayRate() {
        return rayRate;
    }

    public List<StandardEntity> getFreeFireBrigades() {
        HumanHelper humanHelper = getHelper(HumanHelper.class);
        MrlPlatoonAgent mrlPlatoonAgent;
        List<StandardEntity> freeAgents = new ArrayList<StandardEntity>();
        freeAgents.addAll(getFireBrigades());
        freeAgents.removeAll(humanHelper.getBlockedAgents());
        freeAgents.removeAll(getBuriedAgents());
        List<StandardEntity> atRefuges = new ArrayList<StandardEntity>();
        for (StandardEntity entity : freeAgents) {
            FireBrigade fireBrigade = (FireBrigade) entity;
            if (!fireBrigade.isPositionDefined() || (getEntity(fireBrigade.getPosition()) instanceof Refuge)) {
                atRefuges.add(fireBrigade);
            }
        }
        freeAgents.removeAll(atRefuges);
        return freeAgents;
    }

    public RoadHelper getRoadHelper() {
        return roadHelper;
    }

//    public List<MrlBuilding> getMrlBuildings() {
//        return mrlBuildings;
//    }
//
//    public MrlBuilding getMrlBuilding(EntityID id) {
//        return tempBuildingsMap.get(id);
//    }


    public Simulator getSimulator() {
        return simulator;
    }

    public FireClusters getFireClusters() {
//        return fireClusters;         //TODO commented by sajjad, uncomment if needed, but it should be unnessesary
        return null;
    }

//    public WaterCoolingEstimator getCoolingEstimator() {
//        return coolingEstimator;
//    }

    public Map getGotoMap() {
        return gotoMap;
    }

    public void addGotoMap(Map<EntityID, EntityID> FireBrigadeGotoMAp) {
        for (EntityID id : FireBrigadeGotoMAp.keySet())
            gotoMap.put(id, FireBrigadeGotoMAp.get(id));
    }

    public void clearGoToMap() {
        gotoMap.clear();
    }

    public void setBorderBuildings() {
        Thread loader = new Thread() {
            @Override
            public void run() {
                //long tm1 = System.currentTimeMillis();
                borderBuildings = borderFinder.getBordersOf(0.9);
                //long tm2 = System.currentTimeMillis();
                //long tm = tm2 - tm1;
                //int number = getBuildingIDs().size();
                MrlPersonalData.VIEWER_DATA.setBorderMapBuildings(getSelf().getID(), borderBuildings);
                //System.out.println("done on " + tm + "Miliseconds for " + number + "Buildings.");
                setBorderEntitiesDataLoaded(true);
            }
        };
        loader.start();

    }


    public int getMaxWater() {
        return maxWater;
    }

    public void setMaxWater(int maxWater) {
        this.maxWater = maxWater;
    }

    public int getWaterRefillRate() {
        return waterRefillRate;
    }

    public int getWaterRefillRateInHydrant() {
        return waterRefillRateInHydrant;
    }

    public void setWaterRefillRate(int waterRefillRate) {
        this.waterRefillRate = waterRefillRate;
    }

    public void setWaterRefillRateInHydrant(int waterRefillRate) {
        this.waterRefillRateInHydrant = waterRefillRate;
    }

    public boolean isPrecomputedDataLoaded() {
        return isVisibilityAreaDataLoaded && isBorderEntitiesDataLoaded;
    }

    public boolean isVisibilityAreaDataLoaded() {
        return isVisibilityAreaDataLoaded;
    }

    public void setProcessVisibilityDataLoaded(boolean isPrecomputedDataLoaded) {
        this.isVisibilityAreaDataLoaded = isPrecomputedDataLoaded;
    }

    public boolean isBorderEntitiesDataLoaded() {
        return isBorderEntitiesDataLoaded;
    }

    public void setBorderEntitiesDataLoaded(boolean isBorderEntitesDataLoaded) {
        this.isBorderEntitiesDataLoaded = isBorderEntitesDataLoaded;
    }
}
