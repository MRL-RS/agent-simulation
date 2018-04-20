package mrl.communication2013.message;

import mrl.common.Util;
import mrl.common.comparator.ConstantComparators;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 */
//this class indexes objects of the model to reduce ID bit num

//Notice: this class has some problems with indexing Civilians and Blockades
public class IDConverter {
    private static HashMap<Integer, EntityID> buildings = new HashMap<Integer, EntityID>();
    private static HashMap<Integer, EntityID> roads = new HashMap<Integer, EntityID>();
    private static HashMap<EntityID, Integer> agentsIdKeyMap;
    private static HashMap<Integer, EntityID> agentsKeyIdMap;


    private static int buildingsBitSize;
    private static int agentsBitSize;
    private static int roadsBitSize;

    /////////////Get ID with key////////////////
    public static EntityID getBuildingID(int key) {
        return buildings.get(key);
    }

    public static EntityID getAgentEntityID(int key) {
        return agentsKeyIdMap.get(key);
    }

    public static EntityID getRoadID(int key) {
        return roads.get(key);
    }


    /////////////////////////////////////////////////
    ///////////////Get key with ID///////////////////
    public static int getBuildingKey(EntityID buildingID) {
        for (int i = 0; i < buildings.size(); i++) {
            if (buildings.get(i).equals(buildingID)) {
                return i;
            }
        }

        return -1;
    }

    public static int getAgentsKey(EntityID agentID) {
        return agentsIdKeyMap.get(agentID);
    }

    public static int getRoadKey(EntityID roadID) {
        for (int i = 0; i < roads.size(); i++) {
            if (roads.get(i).equals(roadID)) {
                return i;
            }
        }
        return -1;
    }

    /////////////////////////////////////////////////////////
    //////////////////////Converts///////////////////////////
    public void convertBuildings(Collection<StandardEntity> buildings) {
        int i = 0;
        this.getBuildings().clear();
        for (StandardEntity next : buildings) {
            this.getBuildings().put(i++, next.getID());
        }
        calculateBuildingsBitSize();
    }

    public void convertRoads(List<StandardEntity> roads) {
        int i = 0;
        this.getRoads().clear();
        for (StandardEntity next : roads) {
            this.getRoads().put(i++, next.getID());
        }
        calculateRoadsBitSize();
    }

    public void convertAgents(Collection<StandardEntity> agents) {
        int i = 0;
        agentsIdKeyMap.clear();
        agentsKeyIdMap.clear();
        for (StandardEntity next : agents) {
            agentsIdKeyMap.put(next.getID(), i);
            agentsKeyIdMap.put(i, next.getID());
            i++;
        }
        calculateAgentsBitSize();
    }



    public void convertAll(MrlWorld model) {
        List<StandardEntity> buildings = new ArrayList<StandardEntity>(model.getBuildings());
        Collections.sort(buildings, ConstantComparators.ID_COMPARATOR);
        List<StandardEntity> roads = new ArrayList<StandardEntity>(model.getRoads());
        Collections.sort(roads,ConstantComparators.ID_COMPARATOR);
        List<StandardEntity> agents=new ArrayList<StandardEntity>(model.getPlatoonAgents());
        Collections.sort(agents,ConstantComparators.ID_COMPARATOR);
        agentsKeyIdMap = new HashMap<Integer, EntityID>();
        agentsIdKeyMap = new HashMap<EntityID, Integer>();
        convertBuildings(buildings);
        convertAgents(agents);
        convertRoads(roads);

    }

    //////////////////////////////////////////////////////////
    /////////////////Get hash maps////////////////////////////
    public HashMap<Integer, EntityID> getBuildings() {
        return buildings;
    }

    public HashMap<Integer, EntityID> getRoads() {
        return roads;
    }


    //////////////////////////////////////////////////////////
    ///////////////////Calculate bit size/////////////////////
    public void calculateBuildingsBitSize() {
        int res = 1;
        if (buildings.size() != 0) {
            int size = buildings.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setBuildingsBitSize(res);
    }

    public void calculateAgentsBitSize() {
        int res = 1;
        if (agentsKeyIdMap.size() != 0) {
            int size = agentsKeyIdMap.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setAgentsBitSize(res);
    }

    public void calculateRoadsBitSize() {
        int res = 1;
        if (roads.size() != 0) {
            int size = roads.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setRoadsBitSize(res);
    }

    ///////////////////////////////////////////////////////////
    ////////////////////Gets and sets for bit sizes////////////
    public static int getBuildingsBitSize() {
        return buildingsBitSize;
    }

    public static void setBuildingsBitSize(int buildingsBitSize) {
        IDConverter.buildingsBitSize = buildingsBitSize;
    }

    public static int getAgentsBitSize() {
        return agentsBitSize;
    }

    public static void setAgentsBitSize(int agentsBitSize) {
        IDConverter.agentsBitSize = agentsBitSize;
    }

    public static int getRoadsBitSize() {
        return roadsBitSize;
    }

    public static void setRoadsBitSize(int roadsBitSize) {
        IDConverter.roadsBitSize = roadsBitSize;
    }


}
