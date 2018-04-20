package mrl.world.object;

import mrl.common.ConvexHull_Rubbish;
import mrl.firebrigade.Direction;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.util.SortUtil;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Sajjad Salehi
 * Date: 12/22/11
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class MrlFireCluster extends MrlCluster {

    private ConvexHull_Rubbish convex;
    private int numberOfFirebrigadesNeeded;
    private float volume;
    private int cyclesToExtinguish;
    private int MAX_POWER;
    private List<Direction> directions;
    private EntityID myId;
    private Point centerPoint;
    private MrlBuilding centerBuilding;
    private List<EntityID> assignedFireBrigades;
    private List<Integer> groups;
    private int numberOfGroups;
    private Map<Integer, Integer> powerSet;
    private int myGroup;
    private int clock = 0;
    private int CLOCK_TIK = 10;

    public MrlFireCluster(int fireBrigadeMaxPower) {
        convex = new ConvexHull_Rubbish();
        groups = new ArrayList<Integer>();
        centerPoint = new Point();
        directions = new ArrayList<Direction>();
        assignedFireBrigades = new ArrayList<EntityID>();
        volume = 0;
        powerSet = new HashMap<Integer, Integer>();
        MAX_POWER = fireBrigadeMaxPower;
        numberOfFirebrigadesNeeded = 0;
        cyclesToExtinguish = 1;
        myGroup = 1;
        calculateGroupsNeeded();
        setGroupsCount();
        setPowers();
    }

    public boolean isNearToBuilding(MrlBuilding b) {
        for (MrlBuilding bld : buildings)
            if (bld.getNeighbourFireBuildings().contains(b.getID())) {
                return true;
            }
        return false;
    }

    public void calculateGroupsNeeded() //The number of groups that fire brigades should split
    {
        numberOfGroups = 2;
    }

    public void setGroupsCount() {
        for (int i = 0; i < numberOfGroups; i++) {
            groups.add(numberOfFirebrigadesNeeded / numberOfGroups);
        }
    }

    public void setPowers() {
        powerSet.put(1, MAX_POWER);
        powerSet.put(2, MAX_POWER / 2);
    }

    public void assignAFireBrigade(EntityID id) {
        groupUpAgents();
        assignedFireBrigades.add(id);
    }

    public void assignMe(EntityID Id) {
        myId = Id;
        assignAFireBrigade(Id);
    }

    public boolean removeFireBrigade(EntityID id) {
        groupUpAgents();
        return assignedFireBrigades.remove(id);
    }

    public int size() {
        return buildings.size();
    }

    public void computeCyclesToExtinguish() {
        cyclesToExtinguish = (int) (volume * 550 / MAX_POWER);
    }

    public void computeFirebrigadesNeeded() {
        numberOfFirebrigadesNeeded = cyclesToExtinguish / 20;
        if (numberOfFirebrigadesNeeded == 0)
            numberOfFirebrigadesNeeded = 1;
    }

    private void updateVolume() {
        volume = 0;
        for (MrlBuilding building : convex.getEdgeBuildings()) {
            volume += building.getVolume();
        }
        computeCyclesToExtinguish();
        computeFirebrigadesNeeded();
    }

    @Override
    public void addBuilding(MrlBuilding b) {
        if (buildingIDS.size() == 0) {
            centerBuilding = b;
        }
        buildings.add(b);
        buildingIDS.add(b.getID());
        convex.addBuilding(b);
        convex.updateEdgeBuildings(buildings);
        updateVolume();
    }

    public void addAllBuildings(List<MrlBuilding> buildingList) {
        buildings.addAll(buildingList);
        buildingIDS.addAll(getBuildingIDs(buildingList));
        convex.addMrlBuildings(buildingList);
        convex.updateEdgeBuildings(buildings);
        updateVolume();
    }

    public MrlFireCluster mergeCluster(MrlFireCluster fireClusters) {
        //TODO complete this method
        return this;
    }

    public void removeBuilding(MrlBuilding b, MrlFireBrigadeWorld world) {
        buildings.remove(b);
        buildingIDS.remove(b.getID());
        convex.removeBuilding(b, world);
        updateVolume();
    }

    public ConvexHull_Rubbish getConvexHull() {
        return convex;
    }

    //an static method to give us the IDs, maybe it should be moved to another class
    public static List<EntityID> getBuildingIDs(List<MrlBuilding> buildings) {
        List<EntityID> IDs = new ArrayList<EntityID>();
        for (MrlBuilding building : buildings)
            IDs.add(building.getID());
        return IDs;
    }

    public void groupUpAgents() {
        int number = getMyNumber();
        int counter = 0;
        for (int i = 0; i < groups.size(); i++) {
            counter += groups.get(i);
            if (number <= counter)
                myGroup = i + 1;
        }
    }

    public void setCenterPoint() {
        //convex.getConvexPolygon()
    }

    public int getMyNumber() {
        if (myId == null || !assignedFireBrigades.contains(myId))
            return 0;
        SortUtil.sortByEntityID(assignedFireBrigades);
        return assignedFireBrigades.indexOf(myId);
    }

    public void setDirection(MrlFireBrigadeWorld world) {
//        PreRoutingPartitions partitions = new PreRoutingPartitions(world, 3, 3);
//        partitions.findPartitionID(centerBuilding.getSelfBuilding().getX(), centerBuilding.getSelfBuilding().getY());
    }

    public void printCluster(int i) {
        System.out.println("=========Cluster" + i + "=========");
        System.out.println("fireBrigades needed: " + numberOfFirebrigadesNeeded);
        for (EntityID building : buildingIDS)
            System.out.println(building.toString());
        convex.printHull();
        System.out.println("==========================");
    }

    public void printCluster() {
        System.out.println("=========Cluster=========");
        for (EntityID building : buildingIDS)
            System.out.println(building.toString());
        System.out.println("==========================");
    }

    @Override
    public String toString() {
        String str = "";
        str += "=========Cluster=========\n";
        for (EntityID building : buildingIDS)
            str += building.toString() + "\n";
        str += "==========================\n";
        return str;
    }

    public double getPerimeter() {
        return convex.perimeter();
    }

    public int getNumberOfFirebrigadesNeeded() {
        return numberOfFirebrigadesNeeded;
    }

    public float getVolume() {
        return volume;
    }

    public List<EntityID> getAssignedFireBrigades() {
        return assignedFireBrigades;
    }

    public void setCenterBuilding(MrlBuilding center) {
        this.centerBuilding = center;
    }

    public EntityID getCenter() {
        if (centerBuilding != null)
            return centerBuilding.getID();
        return null;
    }

    public int getMyPower() {
        Integer r = powerSet.get(myGroup);
        return r;
    }

    public int power() {
        return Math.min((int) (MAX_POWER / (Math.sqrt(Math.sqrt(getMyNumber()) - 0.65))), MAX_POWER);
    }
}
