package mrl.platoon.search.la;

import javolution.util.FastSet;
import mrl.helper.CivilianHelper;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: roohola
 * Date: 5/10/11
 * Time: 6:15 PM
 */
public class ZIOCollection {
    MrlZone zone;
    MrlWorld world;

    //    public List<StandardEntity> agentsInZone = new ArrayList<StandardEntity>();
    public List<Building> unvisitedBuildingsInZone = new ArrayList<Building>();
    public List<Building> possibleBuildingsInZone = new ArrayList<Building>();

    public int possibleCivCount = 0;
    public int civilianCount = 0;
    public int aliveCivilianCount = 0;

    boolean noMoreUnvisited = false;
    private int searched = 0;
    protected int lastTimeOfSearch = 0;

//    private static Log logger = LogFactory.getLog(ZIOCollection.class);

    public ZIOCollection(MrlZone zone, MrlWorld world) {
        this.zone = zone;
        this.world = world;
    }

    public void update() {
//        logger.debug("Update started...");
//        agentsInZone.clear();

        if (!noMoreUnvisited) {
            unvisitedBuildingsInZone.clear();
            possibleBuildingsInZone.clear();
            possibleCivCount = 0;
        }

//        updateAgentInZone();

        if (!noMoreUnvisited) {
            updateUnvisitedBuildings();
            updatePossibleBuildings();
        }

    }
/*
    private void updateAgentInZone() {
//        logger.debug("Update Agents in zone...");
//        int count = 0;
        for (StandardEntity entity : world.getPlatoonAgents()) {
            Human human = (Human) entity;
            if (human.isPositionDefined()) {
                StandardEntity standardEntity = world.getEntity(human.getPosition());
                if (standardEntity instanceof Area) {
                    Area area = (Area) standardEntity;
                    if (area instanceof Building) {
                        if (world.getMrlBuilding(area.getID()).getZoneId().equals(zone.getId())) {
                            agentsInZone.add(entity);
                        }
                    } else {
                        if (zone.contains(area.getX(), area.getY())) {
                            agentsInZone.add(entity);
                        }
                    }
                }
            }
        }
//        logger.debug("Number of agents in zone " + zone + " = " + count);
//        logger.debug("Finished updating agents in zone.");
    }*/

    private void updateUnvisitedBuildings() {

//        logger.debug("Updating unvisited buildings...");

        for (MrlBuilding building : zone) {
            if (!world.getMrlBuilding(building.getID()).isVisited() && !building.isBurning() && !building.isBurned()) {
                unvisitedBuildingsInZone.add(building.getSelfBuilding());
            }
        }

        if (unvisitedBuildingsInZone.isEmpty()) {
            noMoreUnvisited = true;
        }
//        logger.debug("Unvisited buildings in zone: " + unvisitedBuildingsInZone.size());
//        logger.debug("Unvisited buildings update finished.");

    }

    private void updatePossibleBuildings() {
        aliveCivilianCount = 0;
        civilianCount = 0;
//        logger.debug("Updating Possible buildings...");
        CivilianHelper civilianHelper = world.getHelper(CivilianHelper.class);
        Set<Civilian> civilians = new FastSet<Civilian>();
        for (StandardEntity entity : world.getCivilians()) {
            Civilian civilian = (Civilian) entity;
            if (!civilian.isPositionDefined()) {
                for (EntityID id : civilianHelper.getPossibleBuildings(civilian.getID())) {
                    Building building = (Building) world.getEntity(id);
//                    if (world.getMrlBuilding(building.getID()).getZoneId().equals(zone.getId())) {
//                        possibleBuildingsInZone.add(building);
//                        civilians.add(civilian);
//                    }
                    if (zone.contains(building.getX(), building.getY())) {
                        possibleBuildingsInZone.add(building);
                        civilians.add(civilian);
                    }
                }

            } else {
                StandardEntity standardEntity = civilian.getPosition(world);
                if (standardEntity instanceof Building) {
                    if (zone.contains(((Building) standardEntity).getX(), ((Building) standardEntity).getY())) {
                        if (civilian.isHPDefined() && civilian.getHP() > 0) {
                            aliveCivilianCount++;
                        } else {
                            civilianCount++;
                        }
                    }
                }

            }
        }
        possibleCivCount = civilians.size();
//        logger.debug("Possible buildings in zone: " + possibleBuildingsInZone);
//        logger.debug("Possible buildings update finished.");
    }

    public boolean isSearched() {
        return searched == -1;
    }

    /**
     * @param searched -1 for finished, 0 for not searched, others(world.time) repeat check this path if new time - pre time = 7.
     */
    public void setSearched(int searched) {
        this.searched = searched;
    }

    public boolean checkCanSearch(int time) {
        if (searched > 0 && searched + 7 <= time) {
            searched = 0;
        }
        return searched == 0;
    }

    public int getLastSearchTime() {
        return lastTimeOfSearch;
    }

    public void setLastSearchTime(int time) {
        this.lastTimeOfSearch = time;
    }


}


