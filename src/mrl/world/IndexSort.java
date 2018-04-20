package mrl.world;

import mrl.common.comparator.ConstantComparators;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collections;


/**
 * created by: Mostafa Shabani
 * Date: Jan 29, 2011
 * Time: 3:05:23 PM
 */
public class IndexSort {
    private ArrayList<EntityID> agentList;
    private ArrayList<EntityID> areaList;
    private ArrayList<EntityID> pathList;

    public IndexSort() {
        this.agentList = new ArrayList<EntityID>();
        this.areaList = new ArrayList<EntityID>();
        this.pathList = new ArrayList<EntityID>();
    }

    public void fillLists(MrlWorld world) {
        for (StandardEntity entity : world.getAgents()) {
            agentList.add(entity.getID());
        }
        Collections.sort(agentList, ConstantComparators.EntityID_COMPARATOR);

        for (StandardEntity entity : world.getAreas()) {
            areaList.add(entity.getID());
        }
        Collections.sort(areaList, ConstantComparators.EntityID_COMPARATOR);

        for (Path path : world.getPaths()) {
            pathList.add(path.getId());
        }
        Collections.sort(pathList, ConstantComparators.EntityID_COMPARATOR);

    }

    public int getAreaIndex(EntityID id) {
        return areaList.indexOf(id);
    }

    public int getAgentIndex(EntityID id) {
        return agentList.indexOf(id);
    }

    public int getPathIndex(EntityID id) {
        return pathList.indexOf(id);
    }

    public EntityID getAreaID(int index) {
        return areaList.get(index);
    }

    public EntityID getAgentID(int index) {
        return agentList.get(index);
    }

    public EntityID getPathID(int index) {
        return pathList.get(index);
    }

}
