package mrl.world.object;

import mrl.common.ConvexHull_Rubbish;
import mrl.common.Util;
import mrl.world.MrlWorld;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;


/**
 * User: Roohola
 * Date: Oct 16, 2008
 * Time: 11:21:03 AM
 */
public class FireCluster extends HashSet<MrlZone> {
    private static final int MAX_NEIGHBOURHOOD_DISTANCE = 50000;

    protected MrlWorld world;
    protected int minX, maxX, minY, maxY;
    private int x, y;
    private int totalBurningTempValue = 0;
    private double value = -1.0;
    private double totalArea = 0.0f;
    public int sensingTime = 0;
    private boolean isPutOff = false;
    private boolean isSensingTimeSet = false;
    private int burningTime = 0;
    private boolean isReachable = false;
    private double sumCndValues;
    private int distanceToCenterOfMap;
    private int directionOfExtinguish;
    private Polygon polygon;

    double totalSemiFierinessValue;
    private int civilianCount;
    double totalTemperatureValue;
    double totalFierinessCoef = -6;
    double totalTemperatureCoef = -5;
    double distanceToCenterCoef = -0.33;


    public FireCluster(MrlZone zoneEntity, MrlWorld world) {
        this.world = world;
        this.add(zoneEntity);
        update();
    }


    private Comparator<Area> xComparator = new Comparator<Area>() {
        public int compare(Area a1, Area a2) {

            if (a1.getX() > a2.getX())
                return 1;
            if (a1.getX() == a2.getX())
                return 0;

            return -1;
        }
    };

    private Comparator<Area> yComparator = new Comparator<Area>() {
        public int compare(Area a1, Area a2) {

            if (a1.getY() > a2.getY())
                return 1;
            if (a1.getY() == a2.getY())
                return 0;

            return -1;
        }
    };


    public void calculateCoordinates() {
        Building bd;
        maxX = Integer.MIN_VALUE;
        minX = Integer.MAX_VALUE;
        maxY = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE;
        for (MrlZone z : this) {
            if (z.getCenter().getX() > maxX)
                maxX = (int) z.getCenter().getX();
            if (z.getCenter().getX() < minX)
                minX = (int) z.getCenter().getX();
            if (z.getCenter().getY() > maxY)
                maxY = (int) z.getCenter().getY();
            if (z.getCenter().getY() < minY)
                minY = (int) z.getCenter().getY();

        }
        x = (maxX + minX) / 2;
        y = (maxY + minY) / 2;
    }

    public Point getCenter() {
        int x_total = 0;
        int y_total = 0;
        int count = 1;
        for (MrlZone zone : this) {
            count++;
            x_total += zone.getCenter().getX();
            y_total += zone.getCenter().getY();
        }

        int fireClusterX = x_total / count;
        int fireClusterY = y_total / count;
        return new Point(fireClusterX, fireClusterY);
    }

    public void calculateTotalBurningTempValue() {
        totalBurningTempValue = 0;
        for (MrlZone zone : this) {
            totalBurningTempValue += zone.getTotalBurningBuildingTemperatureValue();
        }
    }

    protected boolean canBeInMyFireCluster(MrlZone zoneEntity) {
        for (MrlZone z : this) {
            if (z.getNeighbors().contains(zoneEntity))
                return true;
        }
        return false;
    }

    public void update() {
        totalArea = 0.0f;
        List<MrlZone> toRemove = new ArrayList<MrlZone>();

        for (MrlZone zone : this) {
            zone.setFireCluster(this);
            if (zone.isPutOff()) {
                toRemove.add(zone);
                zone.setFireCluster(null);
            }
        }
        this.removeAll(toRemove);
        for (MrlZone zoneEntity : this) {
            totalArea += zoneEntity.getTotalArea();
        }
//        for (Building bd : this.getBurningBuilding()) {
//            totalArea += bd.getTotalArea();
//        }
//        System.out.println("zana>> time " + world.getTime() + " totalArea " + this.totalArea);
        if (!world.getZones().getBurningZones().isEmpty() && this.isPutOff  /*&&!this.isIgnitionTimeSet*/) {
//            this.isIgniteAgain = true;
//            this.isIgnitionTimeSet = true;
            this.burningTime = world.getTime();
        }

        if (world.getZones().getBurningZones().isEmpty()) {
            this.isPutOff = true;
            this.isSensingTimeSet = false;
            return;
        } else {
            this.isPutOff = false;
        }
//        if (this.getBurningBuilding().isEmpty()) {
//            this.isPutOff = true;
////            this.isIgniteAgain = false;
////            this.isIgnitionTimeSet = false;
//            this.isSensingTimeSet = false;
//            return;
//        } else
//            this.isPutOff = false;


        UpdateValue();

//        updateConvexHull();

    }

    private void updateConvexHull() {
        ConvexHull_Rubbish convexHull = new ConvexHull_Rubbish();
        for (MrlZone zone : this) {
            for (MrlBuilding building : zone) {
                if (building.isBurning() || building.isPutOff() || building.isBurned()) {

                    int[] apexes = building.getSelfBuilding().getApexList();
                    for (int i = 0; i < apexes.length; i += 2) {
                        convexHull.addPoint(apexes[i], apexes[i + 1]);
                    }
                }
            }
        }
        polygon = convexHull.getConvexPolygon();
        for (MrlZone zone : this) {
            for (MrlBuilding building : zone) {
                if (building.getEstimatedFieryness() == 0) {
                    if (polygon.contains(building.getSelfBuilding().getX(), building.getSelfBuilding().getY())) {
                        building.setProbablyOnFire(true);
                    }
                }
            }
        }

    }

    protected boolean canBeMerged(FireCluster site) {
        for (MrlZone zoneEntity : site) {
            if (canBeInMyFireCluster(zoneEntity)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containInClusters(MrlZone zoneEntity, ArrayList<FireCluster> cluster) {
        for (FireCluster fc : cluster) {
            if (fc.contains(zoneEntity)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBurning() {
        return !world.getZones().getBurningZones().isEmpty();
    }

    public String toStringAllInfo() {
        String s = "FireSite(Count:" + size() + ") - burnings(" + world.getZones().getBurningZones().size() + ") burned(" + getBurnedZone().size() + ") watered(" + getWateredZone().size() + ") <<>> ";
        s += "BURNING" + world.getZones().getBurningZones() + " -- ";
        s += "BURNED" + getBurnedZone() + " -- ";
        s += "WATERED" + getWateredZone();
        return s;
    }

    public String toString() {
        return "FireSite( Count:" + size() + ") - zones: ";
//        return "FireSite(Count:" + size() + ") - burnings(" + world.getZones().getBurningZones().size() + ") burned(" + getBurnedZone().size() + ") watered(" + getWateredZone().size() + ") ";
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public double getValue() {
        return value;
    }

    public double getTotalArea() {
        return totalArea;
    }

    public boolean isReachable() {
        return isReachable;
    }

    public void setReachable(boolean reachable) {
        isReachable = reachable;
    }


    private void UpdateValue() {
        calculateCoordinates();
        calculateTotalBurningTempValue();
        Area mapCenter = world.getCenterOfMap();
        distanceToCenterOfMap = Util.distance(x, y, mapCenter.getX(), mapCenter.getY());
//
//        float val;
//        val = -(float) (totalBurningTempValue * 0.0001);
//        val += distanceToCenterOfMap * (50 / world.getPole());
//        value = val;
//        if (!this.getBurningAndBurnedBuilding().isEmpty())
//            world.printData(" site: " +this.getBurningAndBurnedBuilding().get(0).get(0) + "  ValueSite:" + value + "   /\\ tempV:" + (float) (-1*totalBurningTempValue * 0.0002) + " disToCenter:" + (distanceToCenterOfMap * (50 / world.getPole())));

        totalSemiFierinessValue = 0;
        totalTemperatureValue = 0;

        String s = "";
        for (MrlZone zone : this) {
            totalSemiFierinessValue += zone.getTotalSemiFierinessValue();
            totalTemperatureValue += zone.getTotalTemperatureValue();
            s += zone.get(0).getSelfBuilding().getID().getValue() + ", ";
        }

        value = ((totalFierinessCoef * (totalSemiFierinessValue / (3 * totalArea)))
                + (totalTemperatureCoef * (totalTemperatureValue / (900 * totalArea)))
                + (distanceToCenterCoef * (distanceToCenterOfMap / world.getMapDiameter())));

//        if (!this.getBurningAndBurnedBuilding().isEmpty())
//            world.printData("  ValueSite:" + value + "    avg = " + (value / size()) + " size = " + size() + "  f: " + (totalFierinessCoef * (totalSemiFierinessValue / (3 * totalArea)))
//                    + "  t: " + (totalTemperatureCoef * (totalTemperatureValue / (900 * totalArea)))
//                    + "  d: " + (distanceToCenterCoef * (distanceToCenterOfMap / world.getMapDiameter())) + " site: " + s);

    }

    // todo: baraye estimate tempreture be kar rafte
//    public HashSet<EntityID> getNeighboursByEdge(){
//        HashSet<EntityID> set=new HashSet<EntityID>();
//        for(Building building:this){
//            for(EntityID neighbourBuilding:building.getNeighboursByEdge()){
//                if( !set.contains(neighbourBuilding) && !this.contains(neighbourBuilding) ){
//                    set.add(neighbourBuilding);
//                }
//            }
//        }
//        return set;
//    }


//    public List<Building> getBurningBuilding() {
////        return Collections.unmodifiableSet(burningBuilding);
//        List<Building> burnings = new ArrayList<Building>();
//        for (Building building : this) {
//            if (building.isFierynessDefined() && building.getFieryness() >= 1 && building.getFieryness() <= 3)
//                burnings.add(building);
//        }
//        return burnings;
//    }

    public List<MrlZone> getBurnedZone() {
//        return Collections.unmodifiableSet(burnedBuilding);
        List<MrlZone> burned = new ArrayList<MrlZone>();
        for (MrlZone zoneEntity : world.getZones().getBurningZones())
            for (MrlBuilding building : zoneEntity) {
                if (building.getEstimatedFieryness() == 8) {
                    if (!burned.contains(zoneEntity))
                        burned.add(zoneEntity);
                }
            }
        return burned;
    }

    public List<MrlZone> getWateredZone() {
//        return Collections.unmodifiableSet(wateredBuilding);
        List<MrlZone> watered = new ArrayList<MrlZone>();
        for (MrlZone zoneEntity : world.getZones()) {
            for (MrlBuilding building : zoneEntity) {
                if (building.getEstimatedFieryness() >= 5 && building.getEstimatedFieryness() <= 7) {
                    if (!watered.contains(zoneEntity)) {

                        watered.add(zoneEntity);
                    }
                }
            }
        }
        return watered;
    }

    public List<MrlZone> getBurningAndBurnedBuilding() {
//        return Collections.unmodifiableSet(wateredBuilding);
        List<MrlZone> burningAndBurned = new ArrayList<MrlZone>();
        for (MrlZone zoneEntity : this) {
            for (MrlBuilding building : zoneEntity) {
                if ((building.getEstimatedFieryness() >= 1 && building.getEstimatedFieryness() <= 3) || building.getEstimatedFieryness() == 8)
                    if (!burningAndBurned.contains(zoneEntity))
                        burningAndBurned.add(zoneEntity);
            }
        }
        return burningAndBurned;
    }

    public double getSumCndValues() {
        return sumCndValues;
    }

    public void setSumCndValues(float sumCndValues) {
        this.sumCndValues = sumCndValues;
    }

    public int getDistanceToCenterOfMap() {
        return distanceToCenterOfMap;
    }


    public int getDirectionOfExtinguish() {
        return directionOfExtinguish;
    }

    public void setDirectionOfExtinguish(int directionOfExtinguish) {
        this.directionOfExtinguish = directionOfExtinguish;
    }

    public boolean isSensingTimeSet() {
        return isSensingTimeSet;
    }

    public int getBurningTime() {
        return burningTime;
    }

    public void setSensingTimeSet(boolean sensingTimeSet) {
        isSensingTimeSet = sensingTimeSet;
    }

}
