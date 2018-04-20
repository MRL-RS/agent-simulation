package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Siavash
 * @see ISearchStrategy
 */
public abstract class SearchStrategy implements ISearchStrategy {

    private static Log logger = LogFactory.getLog(SearchStrategy.class);

    private List<EntityID> visitedBuildings;
    protected List<Area> visitedAreas;
    protected Set<Area> blackList;
    protected MrlPlatoonAgent agent;
    protected MrlWorld world;
    protected Path path;
    protected Building building;
    protected boolean searchInside;

    SearchStrategy(MrlPlatoonAgent agent, MrlWorld world) {
        this.agent = agent;
        this.world = world;
        visitedBuildings = new ArrayList<EntityID>();
        visitedAreas = new ArrayList<Area>();
        blackList = new HashSet<Area>();
        searchInside = false;
    }

    @Override
    public SearchStatus manualMoveToArea(Area targetArea) throws CommandException {
        if (targetArea != null && !world.getSelfPosition().equals(targetArea)) {
            agent.move(targetArea, MRLConstants.IN_TARGET, false);
            blackList.add(targetArea);
        } else if (targetArea != null && world.getSelfPosition().equals(targetArea)) {
            return SearchStatus.FINISHED;
        }
        return SearchStatus.CANCELED;
    }

    @Override
    public SearchStatus manualMoveToRoad(Road targetRoad) throws CommandException {
        if (targetRoad != null && !world.getSelfPosition().equals(targetRoad)) {
            agent.move(targetRoad, MRLConstants.IN_TARGET, false);
            blackList.add(targetRoad);
        } else if (targetRoad != null && world.getSelfPosition().equals(targetRoad)) {
            return SearchStatus.FINISHED;
        }
        return SearchStatus.CANCELED;
    }

    /**
     * Plan move to {@code moveToSearchBuilding} considering option parameters
     *
     * @param targetBuildingToSearch   target building
     * @param searchUnvisitedBuildings search unvisited building only
     * @param searchInside             search inside building
     * @throws mrl.common.CommandException
     */
    public SearchStatus manualMoveToSearchingBuilding(Building targetBuildingToSearch, boolean searchInside, boolean searchUnvisitedBuildings) throws CommandException {
        //TODO: handle the condition if there is no entrance open to a building
        throw new UnsupportedOperationException("This method should be replace with new one");
//
//        List<Road> entranceRoads = null;
//        if (targetBuildingToSearch != null && !world.getSelfPosition().equals(targetBuildingToSearch)) {
//            entranceRoads = BuildingHelper.getEntranceRoads(world, targetBuildingToSearch);
//
//            if (entranceRoads == null || entranceRoads.isEmpty()) {
//                return SearchStatus.CANCELED;
//            }
//
//            if (searchInside) {
//                agent.move(targetBuildingToSearch, MRLConstants.IN_TARGET, false);
//            } else {
//                if (world.getChanges().contains(targetBuildingToSearch.getID())) {
//                    updateVisitedBuildings(targetBuildingToSearch);
//                    targetBuildingToSearch = null;
//                    return SearchStatus.FINISHED;
//                }
//                for (Road road : entranceRoads) {
//                    if(world.getSelfPosition().equals(road)){
//                        updateVisitedBuildings(targetBuildingToSearch);
//                        targetBuildingToSearch = null;
//                        return SearchStatus.FINISHED;
//                    }
//                    agent.move(road, MRLConstants.IN_TARGET, false);
//                    blackList.add(road);
//                }
//            }
//
//        } else if (targetBuildingToSearch != null) {
//            if (searchInside) {
//                if (world.getSelfPosition().equals(targetBuildingToSearch)) {
//                    targetBuildingToSearch = null;
//                    return SearchStatus.FINISHED;
//                }
//            } else {
//                entranceRoads = BuildingHelper.getEntranceRoads(world, targetBuildingToSearch);
//                if (entranceRoads != null && entranceRoads.contains(world.getSelfPosition())) {
//                    updateVisitedBuildings(targetBuildingToSearch);
//                    targetBuildingToSearch = null;
//                    return SearchStatus.FINISHED;
//                }
//                else if (world.getChanges().contains(targetBuildingToSearch.getID())) {
//                    updateVisitedBuildings(targetBuildingToSearch);
//                    targetBuildingToSearch = null;
//                    return SearchStatus.FINISHED;
//                }
//            }
//        }
//        return SearchStatus.CANCELED;
    }

    /**
     * creates visited building message and adds visited buildings to {@code visitedBuildings}.
     */
    public void updateVisitedBuildings() {
//        if (world.getSelfPosition() instanceof Building) {
//
//            Building visitedBuild = (Building) world.getSelfPosition();
//            if (!visitedBuildings.contains(visitedBuild.getID())) {
//                visitedBuildings.add(visitedBuild.getID());
//            }
//            if (world.getUnvisitedBuildings().contains(visitedBuild.getID())) {
//                world.getUnvisitedBuildings().remove(visitedBuild.getID());
//            }
//        }
    }

    /**
     * adds {@code visited} to visited buildings and removes it from unvisited buildings and sends the proper message.
     *
     * @param visited visited building
     */
    public void updateVisitedBuildings(Building visited) {
//        if (!visitedBuildings.contains(visited.getID())) {
//            visitedBuildings.add(visited.getID());
//        }
//
//        if(world.getUnvisitedBuildings() != null && visited != null){
//            world.getUnvisitedBuildings().remove(visited.getID());
//            world.getVisitedBuildings().add(visited.getID());
//            MrlBuilding mrlBuilding = world.getMrlBuilding(visited.getID());
//            mrlBuilding.setVisited();
//        }

    }

    public void addVisitedArea(Area visitedArea) {
        if (!visitedAreas.contains(visitedArea)) {
            visitedAreas.add(visitedArea);
        }
    }

    /**
     * Search inside buildings
     *
     * @param searchInside true if you want to search inside the buildings.
     */
    public void setSearchInside(boolean searchInside) {
        this.searchInside = searchInside;
    }

    public void setVisitedBuildings(List<EntityID> visitedBuildings) {
        this.visitedBuildings = visitedBuildings;
    }

    protected MrlPlatoonAgent getAgent() {
        return agent;
    }

    protected MrlWorld getWorld() {
        return world;
    }
}
