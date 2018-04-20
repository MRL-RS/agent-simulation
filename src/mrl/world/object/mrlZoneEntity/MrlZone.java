package mrl.world.object.mrlZoneEntity;

import javolution.util.FastMap;
import mrl.common.ConvexHull_Rubbish;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.platoon.search.la.ZIOCollection;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.FireCluster;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Mostafa Shabani.
 * Date: 5/12/11
 * Time: 3:55 PM
 */
public class MrlZone extends ArrayList<MrlBuilding> implements Comparable, MRLConstants {

    protected int id;
    protected MrlWorld world;
//    private WaterCoolingEstimator coolingEstimator;

    // statics
    private Polygon polygon;
    private ZIOCollection zio;
    int totalGroundArea;
    int totalArea;
    int totalNeighbourArea;
    double totalInitFuel = 0;
    Point center = null;
    Integer distanceToCenterOfMap = null;
    private ArrayList<Integer> neighborZoneIds = new ArrayList<Integer>();
    private ArrayList<MrlZone> neighbors = new ArrayList<MrlZone>();
    private List<Path> paths = new ArrayList<Path>();

    // dynamics
    double totalSemiFierinessValue;
    double totalTemperatureValue;
    double totalBurningBuildingTemperatureValue;
    double burningBuildingSize;
    int distanceToCenterOfFireSite;
    double totalRemainedFuel;
    double fireStartTime = -1;
    double fireExpansion;
    List<MrlBuilding> burningBuildings = new ArrayList<MrlBuilding>();
    private List<MrlBuilding> unBurnedBuildings;
    protected FireCluster fireCluster;

    // coefficients
    double totalAreaCoef = 0;
    double totalFierinessCoef = -8;
    double totalTemperatureCoef = -7;
    double burnedPerUnBurnedCoef = 7;
    double distanceToCenterCoef = -4;
    double distanceToCenterOfFireSiteCoef = 4;
    double fireStartTimeCoef = 2;
    double fireExpansionCoef = 8;
    double civilianCoef = 8.0;

    int timeToSimulateTimeToBurn;

    // final values
    protected boolean onFire;
    protected boolean burned;
    protected double localValue;
    protected double globalValue;
    protected double zoneValue;
    protected int neededAgentsToExtinguish;
    protected int fireBrigadeSize;
    protected Map<EntityID, Integer> agentDistanceMap = new FastMap<EntityID, Integer>();
    private int maxWater;
    protected double searchValue;


    public MrlZone(MrlWorld world, int id) {
        this.world = world;
        this.id = id;
//        this.coolingEstimator = new WaterCoolingEstimator();
        maxWater = world.getConfig().getIntValue(MAX_WATER_KEY);
        fireBrigadeSize = world.getFireBrigades().size();
        unBurnedBuildings = new ArrayList<MrlBuilding>();
        timeToSimulateTimeToBurn = world.getPlatoonAgent().getRandom().nextInt(10) + 4;
    }

    public void initZoneInfo() {
        calculateCenterData();
        createPolygon();
        setZio(new ZIOCollection(this, world));
        addPaths();
    }

    public boolean addBuilding(MrlBuilding building) {
        totalArea += building.getSelfBuilding().getTotalArea();
        totalNeighbourArea = totalArea;
        totalGroundArea += building.getSelfBuilding().getGroundArea();
        totalInitFuel += building.getInitialFuel();

        return super.add(building);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private void calculateCenterData() {
        center = new Point();
        for (MrlBuilding b : this) {
            center.x += b.getSelfBuilding().getX();
            center.y += b.getSelfBuilding().getY();
            b.setCellCover(totalGroundArea);
        }
        int count = this.size();
        if (count != 0) {
            center.x /= count;
            center.y /= count;

            distanceToCenterOfMap = Util.distance(center.x, center.y, world.getCenterOfMap().getX(), world.getCenterOfMap().getY());
        }
    }

    @Override
    public String toString() {
//        String local = (localValue == Double.MAX_VALUE) ? ("MAX_VALUE") : (String.valueOf(localValue));
//        String global = (globalValue == Double.MAX_VALUE) ? ("MAX_VALUE") : (String.valueOf(globalValue));
        return "MrlZone[id:" + id + " - firstBuilding:" + this.get(0).getSelfBuilding().getID().getValue() + "  - NAgent:" + neededAgentsToExtinguish + "  - LValue: " + localValue + "  - GValue: " + globalValue + "  - FinalValue: " + zoneValue + "]";
    }

    @Override
    public int compareTo(Object o) throws ClassCastException {  //nozuly
        if (o instanceof MrlZone) {
            MrlZone entity = (MrlZone) o;
            if (zoneValue > entity.getValue()) {
                return -1;
            } else if (zoneValue < entity.getValue()) {
                return 1;
            } else {
                return 0;
            }
        }
        throw new ClassCastException();
    }

    public void update() {
        onFire = false;
        totalSemiFierinessValue = 0;
        totalTemperatureValue = 0;
        totalRemainedFuel = 0;
        fireExpansion = 0;
        totalBurningBuildingTemperatureValue = 0;
        burningBuildingSize = 0;
        burningBuildings.clear();
        unBurnedBuildings.clear();
//        Building bldg;

        for (MrlBuilding b : this) {


            int fieriness = 0;
            double temperature = 0;

//            if ( bldg.isTemperatureDefined()) {
            fieriness = b.getEstimatedFieryness();
            temperature = b.getEstimatedTemperature();
//            }

            totalSemiFierinessValue += (semiFieriness(fieriness) * b.getSelfBuilding().getTotalArea());
            totalTemperatureValue += (temperature * b.getSelfBuilding().getTotalArea());
            totalRemainedFuel += b.getFuel();

            if (fieriness == 0 || (fieriness > 3 && fieriness < 8)) {
                unBurnedBuildings.add(b);
            } else if (fieriness < 4) {
                onFire = true;
                if (fireStartTime == -1) {
                    fireStartTime = world.getTime();
                }
                totalBurningBuildingTemperatureValue += (temperature * b.getSelfBuilding().getTotalArea());
                burningBuildingSize += b.getSelfBuilding().getTotalArea();
                burningBuildings.add(b);
            }
        }

        // mohasebeye roshd dehi atash ta nCycle va zaman ta sukhtane kamel.
        if ((world.getSelf() instanceof MrlFireBrigade) && isOnFire()) {

            int ew = extraWaterForNeighbours();
            neededAgentsToExtinguish = (int) Math.ceil((calculateNeededWaterToExtinguish() + (ew * 0.15)) / (maxWater * 0.8));
//            neededAgentsToExtinguish = (int) Math.round((totalBurningBuildingTemperatureValue/burningBuildingSize) / 6000);
//            neededAgentsToExtinguish = (int) Math.round((totalTemperatureValue/size()) / 4000);
            if (neededAgentsToExtinguish == 0) {
                neededAgentsToExtinguish = 1;
            }
//            if (neededAgentsToExtinguish > fireBrigadeSize) {
//                neededAgentsToExtinguish = fireBrigadeSize;
//            }
            if (fireCluster != null) {
                distanceToCenterOfFireSite = Util.distance(fireCluster.getCenter(), getCenter());
            }
            updateLocalValue();
        } else {
            neededAgentsToExtinguish = 0;
        }


//        if (zio != null)
//        {
//            zio.update();
//        }

        if (world.getTime() == 4 && world.getPlatoonAgent() != null) {
            Collections.shuffle(paths, world.getPlatoonAgent().getRandom());
        }

    }

    private int extraWaterForNeighbours() {
        int w = 0;
        for (MrlZone zone : neighbors) {
            w += zone.calculateNeededWaterToExtinguish();
        }
        if (!neighbors.isEmpty())
            w /= neighbors.size();
        return w;
    }

    private double getValueForNoneFireBrigades() {
        if (onFire) {
            return (totalAreaCoef * (totalArea / world.getZones().getMaximumTotalArea()))
                    + (totalFierinessCoef * (totalSemiFierinessValue / (3 * totalArea)))
                    + (totalTemperatureCoef * (totalTemperatureValue / (900 * totalArea)))
                    + (burnedPerUnBurnedCoef * (totalRemainedFuel / totalInitFuel))
                    + (distanceToCenterCoef * (distanceToCenterOfMap / world.getMapDiameter()))
                    + (fireStartTimeCoef * (fireStartTime / world.getTime()));
        } else {
            return 0;
        }
    }

    public List<MrlBuilding> getBurningBuildings() {
        return burningBuildings;
    }

    private void updateLocalValue() {
        localValue = (totalAreaCoef * (totalArea / world.getZones().getMaximumTotalArea()))
                + (totalFierinessCoef * (totalSemiFierinessValue / (3 * totalArea)))
                + (totalTemperatureCoef * (totalTemperatureValue / (900 * totalArea)))
                + (burnedPerUnBurnedCoef * (totalRemainedFuel / totalInitFuel))
                + (distanceToCenterCoef * (distanceToCenterOfMap / world.getMapDiameter()))
//                + (distanceToCenterOfFireSiteCoef * (distanceToCenterOfFireSite / world.getMapDiameter()))
                + (fireStartTimeCoef * (fireStartTime / world.getTime()))
                + (fireExpansionCoef * (fireExpansion / (3 * totalNeighbourArea)));

    }

    public void updateGlobalValues() {

        if (neighbors.isEmpty() || localValue == 0) {
            zoneValue = localValue;
            return;
        }

        double v = 0;

        for (MrlZone zone : neighbors) {
            v += zone.localValue;
        }

        v /= neighbors.size();
        globalValue = v;

        zoneValue = globalValue + localValue + (civilianCoef * (zio.aliveCivilianCount / 10.0));
    }

    public MrlBuilding getBestBuilding(MrlBuilding preTargetBuilding) {
        MrlBuilding best = null;
        double maxVal = Double.MIN_VALUE;
        for (MrlBuilding b : burningBuildings) {

            double val = (b.getBuildingAreaTempValue() * 1) + (b.getBuildingRadiation() * 2) - (b.getNeighbourRadiation() * 1);
            if (preTargetBuilding != null && b.equals(preTargetBuilding)) {
                val *= 1.54;
            }
            int civCount = 0;

            for (StandardEntity entity : world.getCivilians()) {
                Civilian civilian = (Civilian) entity;
                if (civilian.isPositionDefined() && b.getNeighbourIdBuildings().contains(civilian.getPosition())) {
                    civCount++;
                }
            }
            for (EntityID id : world.getBuriedAgents()) {
                Human human = (Human) world.getEntity(id);
                if (human.isPositionDefined() && b.getNeighbourIdBuildings().contains(human.getPosition())) {
                    civCount += 2;
                }
            }

            val += val * (((double) civCount * 5) / 100.0);
            if (val > maxVal) {
                maxVal = val;
                best = b;
            }
        }
        return best;
    }

    public void updateAgentDistance() {
        List<StandardEntity> freeAgents = ((MrlFireBrigadeWorld) world).getFreeFireBrigades();
//        System.out.println("All:" + world.getFireBrigades().size() + " free:" + freeAgents.size());

        int zoneX = getCenter().x;
        int zoneY = getCenter().y;
        agentDistanceMap.clear();

        for (StandardEntity standardEntity : freeAgents) {
            FireBrigade fireBrigade = (FireBrigade) standardEntity;
            Integer fX, fY;
            if (fireBrigade.isPositionDefined()) {
                StandardEntity entity = world.getEntity(fireBrigade.getPosition());
                if (entity instanceof Area) {
                    fX = ((Area) entity).getX();
                    fY = ((Area) entity).getY();
                } else {
                    System.err.println(world.getSelf() + " Time:" + world.getTime() + " Agent:" + fireBrigade + " position:" + entity);
                    agentDistanceMap.remove(fireBrigade.getID());
                    continue;
                }
            } else {
                System.err.println(world.getSelf() + " Time:" + world.getTime() + " Agent:" + fireBrigade + " position: UNKNOWN");
                agentDistanceMap.remove(fireBrigade.getID());
                continue;
            }
            int distance = Util.distance(zoneX, zoneY, fX, fY);
            agentDistanceMap.put(fireBrigade.getID(), distance);
        }
    }

    public static double semiFieriness(int fieriness) {       //Sajjad : what is this???
        switch (fieriness) {
            case 0:
                return 0;
            case 1:
            case 2:
            case 3:
                return fieriness;
            case 4:
                return 0;
            case 5:
            case 6:
            case 7:
                return (double) (fieriness - 4) / 4d;
            case 8:
                return 2;
        }
        return 0;
    }

    private int calculateNeededWaterToExtinguish() {
        int needed = 0;

        for (MrlBuilding building : burningBuildings) {
            needed += waterNeededToExtinguish(building);
        }
        return needed;
    }

    protected int waterNeededToExtinguish(MrlBuilding building) {
        return FireBrigadeUtilities.waterNeededToExtinguish(building);
//        return coolingEstimator.getWaterNeeded(building.getSelfBuilding().getGroundArea(), building.getSelfBuilding().getFloors(),
//                building.getSelfBuilding().getBuildingCode(), building.getEstimatedTemperature(), 20);
    }

    public Point getCenter() {
        return center;
    }

    public double getValue() {
        return zoneValue;
    }

    public void addNeighborZoneIds(Integer neighborZoneId) {
        if (!neighborZoneIds.contains(neighborZoneId)) {
            this.neighborZoneIds.add(neighborZoneId);
        }
    }

    public ArrayList<Integer> getNeighborZoneIds() {
        return neighborZoneIds;
    }

    public void addNeighbor(MrlZone neighbor) {
        totalNeighbourArea += neighbor.getTotalArea();
        this.neighbors.add(neighbor);
    }

    public ArrayList<MrlZone> getNeighbors() {
        return neighbors;
    }

    public int getId() {
        return id;
    }

    public Integer getAgentDistance(EntityID id) {
        return agentDistanceMap.get(id);
    }

    public Map<EntityID, Integer> getAgentDistanceMap() {
        return agentDistanceMap;
    }

    public int getNeededAgentsToExtinguish() {
        return neededAgentsToExtinguish;
    }

    public int getTotalArea() {
        return totalArea;
    }

    public void createPolygon() {
        ConvexHull_Rubbish hull = new ConvexHull_Rubbish();
        for (MrlBuilding building : this) {
            int[] apexes = building.getSelfBuilding().getApexList();
            for (int i = 0; i < apexes.length; i += 2) {
                hull.addPoint(apexes[i], apexes[i + 1]);
            }
        }
        polygon = hull.getConvexPolygon();
    }

    public boolean contains(int x, int y) {
        return polygon.contains(x, y);
    }

    public List<MrlBuilding> getUnBurnedBuildings() {
        return unBurnedBuildings;
    }

    public void setZio(ZIOCollection zio) {
        this.zio = zio;
    }

    public ZIOCollection getZio() {
        return zio;
    }

    public double getBurningBuildingSize() {
        return burningBuildingSize;
    }

    public double getTotalBurningBuildingTemperatureValue() {
        return totalBurningBuildingTemperatureValue;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public boolean isOnFire() {
        return onFire;
    }

    public double getLocalValue() {
        return localValue;
    }

    public double getTotalSemiFierinessValue() {
        return totalSemiFierinessValue;
    }

    public double getTotalTemperatureValue() {
        return totalTemperatureValue;
    }

    public boolean isBurned() {
        return burned;
    }

    public void addPaths() {
        Path path;

        for (MrlBuilding building : this) {
            for (Entrance entrance : building.getEntrances()) {
                path = world.getPaths().getRoadPath(entrance.getID());
                if (path != null && !paths.contains(path)) {
                    paths.add(path);
                }
            }
        }
    }

    public List<Path> getPaths() {
        return paths;
    }

    public double getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(double searchValue) {
        this.searchValue = searchValue;
    }

    public FireCluster getFireCluster() {
        return fireCluster;
    }

    public void setFireCluster(FireCluster fireCluster) {
        this.fireCluster = fireCluster;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MrlZone && getId() == ((MrlZone) o).getId();
    }

    public boolean isPutOff() {
        for (MrlBuilding building : this) {
            if (building.getEstimatedFieryness() <= 3 && building.getEstimatedFieryness() >= 1) {
                return true;
            }
        }
        return false;
    }

    public void increaseLastTargetZone() {
        zoneValue *= 1.4;
    }
}
