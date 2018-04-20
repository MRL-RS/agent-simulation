package mrl.platoon.search;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: 6/10/11
 * Time: 2:33 PM
 */
public class SearchMethod implements ISearchMethod {

    private MrlPlatoonAgent agent;
    private MrlWorld world;
    private RoadHelper roadHelper;

    private Integer targetZoneId;
    private EntityID targetPathId;
    private List<Path> targetPathList;
    private List<Area> entrancesQueue;
    private List<Area> targetList;
    private boolean isFirstTime;
    private int searchedPaths;

    public SearchMethod(MrlWorld world) {
        this.world = world;
        agent = world.getPlatoonAgent();
        entrancesQueue = new ArrayList<Area>();
        targetList = new ArrayList<Area>();
        targetPathList = new ArrayList<Path>();
        roadHelper = world.getHelper(RoadHelper.class);
    }

//    EntityID ii;

    @Override
    public SearchStatus searchIn(Path path, boolean inside, boolean unvisited) throws CommandException {

        if (path == null || (unvisited && path.isSearched())) {
            return SearchStatus.SEARCHED;
        }
        Area myPosition = (Area) world.getSelfPosition();

        if ((targetList.isEmpty() && entrancesQueue.isEmpty()) || (targetPathId == null || !targetPathId.equals(path.getId()))) {
            targetPathId = path.getId();
            targetList.clear();
//            world.printData("P                     START search in " + path + "   unvisited : " + unvisited + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
            updateEntrances(path, unvisited);
            isFirstTime = true;
        } else {
            isFirstTime = false;
        }

        while (targetList.isEmpty() && !entrancesQueue.isEmpty()) {
            fillTargetsToGo(inside);
        }

        if (targetList.isEmpty()) {
//            ii = null;
            if (path.getUnvisitedBuildingCount() == 0) {
                path.setSearched(-1);
//                world.printData("P              targetList.isEmpty()  FINISH SEARCH " + path + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
                return SearchStatus.FINISHED;
            }
//            world.printData("P              targetList.isEmpty()  CANCEL SEARCH " + path + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
            path.setSearched(world.getTime());
            return SearchStatus.CANCELED;
        }

        Area target = targetList.get(0);
//        if (ii == null) {
//            world.printData("P              search in " + path + "  target = " + target + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
//            ii = target.getID();
//        }

        if (myPosition.equals(target)) {
//            createVisitedBuildingMessage(myPosition);
            targetList.remove(target);
//            ii = null;

//            if (targetList.isEmpty() && entrancesQueue.isEmpty() && path.getUnvisitedBuildingCount() == 0) {
////                world.printData("P              search FINISHED " + path + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
//                path.setSearched(-1);
//                return SearchStatus.FINISHED;
//            }
//            return searchIn(path, inside, unvisited);

            if (targetList.isEmpty() && entrancesQueue.isEmpty()) {
                if (path.getUnvisitedBuildingCount() == 0) {
//                world.printData("P              search FINISHED " + path + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
                    path.setSearched(-1);
                    return SearchStatus.FINISHED;
                }
            } else {
                return searchIn(path, inside, unvisited);
            }
        } else {

            if (unvisited && isVisited(target)) {
                targetList.remove(target);
//                ii = null;

                if (!targetList.isEmpty()) {
                    return searchIn(path, inside, unvisited);
                }
                if (entrancesQueue.isEmpty() && path.getUnvisitedBuildingCount() == 0) {
//                    world.printData("P              search FINISHED other visit " + path + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
                    path.setSearched(-1);
                    return SearchStatus.FINISHED;
                }
            } else if (target instanceof Building) {
                Building building = (Building) target;
                if (building.isFierynessDefined()) {
                    if (building.getFieryness() == 0 || (building.getFieryness() > 3 && building.getFieryness() < 8)) {
                        agent.move(target, MRLConstants.IN_TARGET, false);
//                        world.printData(" IIIIIIIIIIIIIIIIIIIII cant move to " + target);
                    } else if (building.getFieryness() == 8) {
                        // age ye building-i sukhte bood baghie building haye chasbide behesh ro ham visited mikone.
//                        for (Area area : targetList) {
//                            createVisitedBuildingMessage(area);
//                        }
                    }
                } else if (inside) {
                    agent.move(target, MRLConstants.IN_TARGET, false);
//                    world.printData(" IIIIIIIIIIIIIIIIIIIII cant move to " + target);
                }
            } else {
                agent.move(target, MRLConstants.IN_TARGET, false);
//                world.printData(" IIIIIIIIIIIIIIIIIIIII cant move to " + target);
            }
            targetList.clear();
//            ii = null;

            if (entrancesQueue.isEmpty()) {
//                world.printData("P              search Cancelled " + path + " UnvisitedBuildingCount = " + path.getUnvisitedBuildingCount());
                path.setSearched(world.getTime());
                return SearchStatus.CANCELED;
            }
            return searchIn(path, inside, unvisited);
        }
        return SearchStatus.CANCELED;
    }

    @Override
    public SearchStatus searchIn(MrlZone zone, boolean inside, boolean unvisited) throws CommandException {
        if (zone.getZio().isSearched() && unvisited) {
            return SearchStatus.SEARCHED;
        }

        if ((targetPathList.isEmpty() && zone.getZio().getLastSearchTime() + 1 < world.getTime()) || (targetZoneId == null || !targetZoneId.equals(zone.getId()))) {
            searchedPaths = 0;
            targetZoneId = zone.getId();
            targetPathList.clear();

            for (Path path : zone.getPaths()) {
                if (path.isSearched() && unvisited) {
                    searchedPaths++;
                } else if (path.checkCanSearch(world.getTime())) {
                    targetPathList.add(path);
                }
            }
        }

        SearchStatus status = SearchStatus.CANCELED;
        if (!targetPathList.isEmpty()) {
            status = searchIn(targetPathList.get(0), inside, unvisited);
//            world.printData("Z   finish " + zone + " status = " + status + "  " + targetPathList.get(0) + " targetPathSize = " + (targetPathList.size() - 1));
            if (!unvisited) {
                zone.getZio().setLastSearchTime(world.getTime());
            }
            targetPathList.remove(0);
        }
        if (status != SearchStatus.CANCELED) {
            searchedPaths++;
        }

        if (!targetPathList.isEmpty()) {
            return searchIn(zone, inside, unvisited);
        }

        zone.getZio().setLastSearchTime(world.getTime());

        if (searchedPaths == zone.getPaths().size()) {
//            world.printData("Z   FINISH SEARCH " + zone + " targetPathSize = " + targetPathList.size());
            zone.getZio().setSearched(-1);
            return SearchStatus.FINISHED;
        }

//        world.printData("Z   cancel SEARCH " + zone + " targetPathSize = " + targetPathList.size());
        zone.getZio().setSearched(world.getTime());
        return SearchStatus.CANCELED;
    }

    /**
     * dar in method entrance-haye path gerefte mishan va bar asase fasele ta ebtedaye path sort mishan.
     *
     * @param path            : target path.
     * @param searchUnvisited : random search or only unvisited.
     */
    public void updateEntrances(Path path, boolean searchUnvisited) {
        Area headOfPath = path.getHeadOfPath();
        List<Area> pathEntrances = new ArrayList<Area>();
        List<Entrance> allPathEntrances;
        List<Building> entranceBuildings;
        allPathEntrances = path.getEntrances();
        int i;
        int distance;
        int minDistance;
        int size = (int) Math.floor(allPathEntrances.size() / 2);
        Area selected;


        if (!searchUnvisited) {
            Collections.shuffle(allPathEntrances);
            while (size-- > 0) {
                if (!allPathEntrances.isEmpty()) {
                    allPathEntrances.remove(0);
                }
            }
        }

        for (Entrance entrance : allPathEntrances) {
            entranceBuildings = entrance.getBuildings();
            for (Area area : entranceBuildings) {

                if (!world.getMrlBuilding(area.getID()).isVisited() || !searchUnvisited) {
                    pathEntrances.add(entrance.getNeighbour());
                    break;
                }
            }
        }

        i = pathEntrances.size();
        while (i-- >= 0) {
            minDistance = Integer.MAX_VALUE;
            selected = null;
            for (Area entrance : pathEntrances) {
                distance = Util.distance(entrance, headOfPath);
                if (distance < minDistance) {
                    minDistance = distance;
                    selected = entrance;
                }
            }

            if (selected != null) {
                entrancesQueue.add(selected);
                pathEntrances.remove(selected);
            }
        }
    }

    /**
     * in method ye list az target-ha baraye search ijad mikone.
     * age inside true bashe entrance ro add nemikone.
     * vali age false bashe add mikone.
     * dar soorati ke ye building entrance buildinge digei bashe un-haro ham be list ezafe mikone.
     * ta dar sorate amn budan search beshan.
     *
     * @param inside should be search in buildings or not.
     */
    private void fillTargetsToGo(boolean inside) {
        if (entrancesQueue.isEmpty()) {
            return;
        }

        if (isFirstTime) {
            // moratab kardan list entrance ha az aval be akhar ya az akhar ba aval.
            int distanceToHead = Util.distance((Area) world.getSelfPosition(), entrancesQueue.get(0));
            int distanceToTail = Util.distance((Area) world.getSelfPosition(), entrancesQueue.get(entrancesQueue.size() - 1));

            if (distanceToHead >= distanceToTail) {
                List<Area> list = new ArrayList<Area>();
                for (int i = entrancesQueue.size() - 1; i >= 0; i--) {
                    list.add(entrancesQueue.get(i));
                }

                entrancesQueue = list;
            }
        }

        Area targetEntrance = entrancesQueue.get(0);
        entrancesQueue.remove(targetEntrance);

        List<Building> buildingsOfThisEntrance;
        buildingsOfThisEntrance = roadHelper.getBuildingsOfThisEntrance(targetEntrance.getID());

        if (inside) {
            for (Building building : buildingsOfThisEntrance) {
                if (!building.isFierynessDefined() || (building.getFieryness() > 3 && building.getFieryness() < 7) || building.getFieryness() == 0) {
                    targetList.add(building);
                }
            }
            if (targetList.isEmpty() && !entrancesQueue.isEmpty()) {
                fillTargetsToGo(inside);
            }
        } else {
            targetList.add(targetEntrance);

            if (buildingsOfThisEntrance.size() > 1) {
                targetList.addAll(buildingsOfThisEntrance);
            }
        }
    }

//    /**
//     * in method jahaei ro ke dide ro be ye list ezafe mikone ke bahash message dorost mikonim.
//     *
//     * @param position : self position.
//     */
//    private void createVisitedBuildingMessage(Area position) {
//
//        if (position instanceof Road) {
//            List<Building> buildings = RoadHelper.getConnectedBuildings(world, (Road) position);
//            for (Building building : buildings) {
//                agent.removeVisitedBuildingFromWorld(building);
//            }
//        }
//
//        if (position instanceof Building) {
//            agent.removeVisitedBuildingFromWorld((Building) position);
//        }
//    }

    private boolean isVisited(Area area) {
        if (area instanceof Building) {
            if (world.getMrlBuilding(area.getID()).isVisited()) {
                return true;
            }
        } else {
            for (EntityID id : area.getNeighbours()) {
                if (world.getEntity(id) instanceof Building) {
                    if (world.getMrlBuilding(id).isVisited()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
