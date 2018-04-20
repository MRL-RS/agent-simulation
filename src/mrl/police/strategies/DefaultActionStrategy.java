package mrl.police.strategies;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.partitioning.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.platoon.genericsearch.*;
import mrl.platoon.search.SearchHelper;
import mrl.police.PoliceConditionChecker;
import mrl.police.clear.ClearActManager;
import mrl.police.moa.PoliceForceUtilities;
import mrl.task.Task;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import mrl.world.routing.pathPlanner.PathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 9:04 PM
 */
public abstract class DefaultActionStrategy implements IPoliceActionStrategy {

    protected MrlWorld world;
    //    protected ClearHelper clearHelper;
    protected PoliceMessageHelper policeMessageHelper;
    protected PoliceForceUtilities utilities;
    protected PoliceConditionChecker conditionChecker;
    protected ClearActManager clearActManager;

    protected Human selfHuman;
    protected MrlPlatoonAgent me;
    protected Task myTask;

    protected Road targetRoad = null;

    protected SearchHelper searchHelper;
    private Partition targetPartition;
    private List<Path> paths;
    protected Building targetBuilding = null;
    protected Path targetPath = null;

    protected DefaultActionStrategy(MrlWorld world, ClearActManager clearActManager, PoliceMessageHelper policeMessageHelper, PoliceForceUtilities utilities, PoliceConditionChecker conditionChecker) {
        this.world = world;
//        this.clearHelper = clearHelper;
        this.clearActManager = clearActManager;
        this.policeMessageHelper = policeMessageHelper;
        this.utilities = utilities;
        this.conditionChecker = conditionChecker;
        paths = new ArrayList<Path>();

        this.selfHuman = world.getSelfHuman();
        this.me = world.getPlatoonAgent();


//        me.possibleBuildingSearchDecisionMaker = new PossibleBuildingSearchDecisionMaker(world);
//        me.possibleBuildingSearchDecisionMaker.setSearchInPartition(true);
        me.legacyHeatTracerDecisionMaker = new LegacyHeatTracerDecisionMaker(world);
        me.legacyHeatTracerDecisionMaker.setSearchInPartition(true);
        me.heatTracerDecisionMaker = new HeatTracerDecisionMaker(world);
        me.heatTracerDecisionMaker.setSearchInPartition(false);
        me.stupidSearchDecisionMaker = new StupidSearchDecisionMaker(world);
        me.stupidSearchDecisionMaker.setSearchInPartition(true);

//        me.possibleBuildingSearchManager = new PossibleBuildingSearchManager(world, me, me.possibleBuildingSearchDecisionMaker, me.senseSearchStrategy);
        me.heatTracerSearchManager = new HeatTracerSearchManager(world, me, me.heatTracerDecisionMaker, me.senseSearchStrategy);
        me.stupidSearchManager = new StupidSearchManager(world, me, me.stupidSearchDecisionMaker, me.senseSearchStrategy);
        me.stupidSearchManager.allowMove(false);
        me.defaultSearchManager = new DefaultSearchManager(world, me, me.stupidSearchDecisionMaker, me.senseSearchStrategy);
        me.civilianSearchBBDecisionMaker = new CivilianSearchBBDecisionMaker(world);
        me.civilianSearchBBDecisionMaker.setSearchInPartition(true);
        me.civilianSearchStrategy = new CivilianSearchStrategy(me, world);
        me.civilianSearchManager = new CivilianSearchManager(world, me, me.civilianSearchBBDecisionMaker, me.civilianSearchStrategy);
        me.civilianSearchManager.allowMove(false);
        me.simpleSearchDecisionMaker = new SimpleSearchDecisionMaker(world);
        me.simpleSearchManager = new SimpleSearchManager(world, me, me.simpleSearchDecisionMaker, me.civilianSearchStrategy);


    }

    @Override
    public abstract void execute() throws CommandException, TimeOutException;


    /**
     * This method forces the PF agent to scape out of the building.
     *
     * @throws CommandException
     */
    public void scapeOut() throws CommandException {

        if (shouldScape()) {
            Building building = (Building) world.getSelfPosition();
            Point2D movePoint = null;

            List<Entrance> entrances = world.getMrlBuilding(world.getSelfPosition().getID()).getEntrances();
            Road entranceRoad = utilities.findEntranceInMyWay(entrances, me.getPathPlanner().getLastMovePlan());
            if (entranceRoad == null) {
                entranceRoad = utilities.findNearestEntrance(entrances);
            }
            if (entrances != null && !entrances.isEmpty()) {
                movePoint = utilities.findMovePointInBuilding(building, entranceRoad);
                if (movePoint != null) {
                    if (Util.distance(selfHuman.getX(), selfHuman.getY(), movePoint.getX(), movePoint.getY()) > 500) {
                        me.moveToPoint(building.getID(), (int) movePoint.getX(), (int) movePoint.getY());
                    }
                }
            }
            clearActManager.clearBlockadesInRange(new Pair<Integer, Integer>(selfHuman.getX(), selfHuman.getY()), 1000);
            if (building.getID().equals(lastStuckBuilding)) {

                try {
                    ((PathPlanner) me.getPathPlanner()).escapeHere.escape(entranceRoad);
                } catch (TimeOutException e) {
//                    e.printStackTrace();
                    world.printData("time out for building scape operation");
                }
                me.move(entranceRoad, 0, true);
            } else {
                lastStuckBuilding = building.getID();
                me.move(entranceRoad, 0, true);
            }
        }
    }

    /**
     * This method check agent should scape out of building or not
     *
     * @return return true if agent should scape out of building which in it.
     */
    private boolean shouldScape() {
        List<EntityID> plan = world.getPlatoonAgent().getPathPlanner().getLastMovePlan();
        if ((world.getSelfPosition() instanceof Building) && !me.getAgentState().equals(State.SEARCHING)) {
            if (!plan.contains(world.getSelfPosition().getID()) || plan.indexOf(world.getSelfPosition().getID()) < plan.size() - 1) {
                return true;
            }
        }
        return false;
    }

    private EntityID lastStuckBuilding = null;

    protected List<EntityID> search() throws CommandException {
        me.setAgentState(State.SEARCHING);

        List<EntityID> pathToGo;
        pathToGo = civilianSearch();

        if (pathToGo.isEmpty()){
            updatePartitionsToSearch();
        }

        if (pathToGo.isEmpty()) {
            pathToGo = randomBuildingSearch();
        }

        if (pathToGo.isEmpty()) {
            pathToGo = randomSearch();
        }
        if (pathToGo.isEmpty()) {
            System.out.println("");
        }
        return pathToGo;
    }

    private List<EntityID> civilianSearch() throws CommandException {
        me.civilianSearchManager.execute();//allow move flag in this method is false! so agent do not move anywhere in this function! just set target to go.
        Area targetToGo = me.civilianSearchManager.getTargetArea();
        if (MRLConstants.DEBUG_SEARCH) {
            world.printData("My civilian target to go is: " + targetToGo);
        }
        if (targetToGo != null) {
            return me.getPathPlanner().planMove((Area) world.getSelfPosition(), targetToGo, 0, true);
        } else {
            return new ArrayList<EntityID>();
        }
    }

    Set<Partition> partitionsToSearch = new HashSet<>();
    private List<EntityID> randomBuildingSearch() {



        if (targetPath == null || !hasUnvisitedBuilding(targetPath)) {
            targetBuilding = null;
            targetPath = null;
            do {
                if (paths.size() > 1) {
                    int index = random.nextInt(paths.size() - 1);
                    targetPath = paths.remove(index);
                } else {
                    //if equals 1
                    targetPath = paths.remove(0);
                }
            } while (paths.size() > 0 && !hasUnvisitedBuilding(targetPath));
        }

        if (targetPath == null) {
            return new ArrayList<EntityID>();
        }

        if (targetBuilding == null || world.getMrlBuilding(targetBuilding.getID()).isVisited() || isBuildingBurnt(targetBuilding)) {
            MrlBuilding mrlBuilding;
            for (Area buildingArea : targetPath.getBuildings()) {
                mrlBuilding = world.getMrlBuilding(buildingArea.getID());
                if (!mrlBuilding.isVisited() && !isBuildingBurnt(mrlBuilding.getSelfBuilding())) {
                    targetBuilding = mrlBuilding.getSelfBuilding();
                    break;
                }
            }
        }

        return me.getPathPlanner().planMove((Area) world.getSelfPosition(), targetBuilding, 0, true);
    }

    private boolean hasUnvisitedBuilding(Path path) {
        if (path == null) {
            return false;
        }
        MrlBuilding mrlBuilding;
        for (Area buildingArea : path.getBuildings()) {
            mrlBuilding = world.getMrlBuilding(buildingArea.getID());
            if (!mrlBuilding.isVisited() && !isBuildingBurnt(mrlBuilding.getSelfBuilding())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBuildingBurnt(Building building) {
        if (building == null || !building.isFierynessDefined()) {
            return false;
        }
        int fieriness = building.getFieryness();

        return fieriness != 0 && fieriness != 4 && fieriness != 5;
    }

    private List<EntityID> randomSearch() throws CommandException {
//
//        }
        if (partitionsToSearch.isEmpty()) {
            updatePartitionsToSearch();
        }


        if (targetRoad == null || world.getSelfPosition().equals(targetRoad)) {
            List<Road> roads = new ArrayList<>() ;
            for (Partition partition : partitionsToSearch){
                roads.addAll(partition.getRoads());
            }

            int index = random.nextInt(roads.size());
            targetRoad = roads.get(index);
            targetID = targetRoad.getID();
            if (world.getSelfPosition().equals(targetRoad)) {
                return randomSearch();
            }
        }
        return me.getPathPlanner().planMove((Area) world.getSelfPosition(), targetRoad, 0, true);

    }

    private void updatePartitionsToSearch() {
        paths = new ArrayList<>();
        Set<Partition> humanPartitions = world.getPartitionManager().findHumanPartitionsMap(selfHuman);
        if(paths==null
                || paths.isEmpty()
                || partitionsToSearch==null
                || humanPartitions.size()<partitionsToSearch.size()
                || !partitionsToSearch.equals(humanPartitions)) {
            partitionsToSearch = humanPartitions;
            for (Partition partition : humanPartitions) {

                paths.addAll(partition.getPaths());
            }
        }

        if (paths == null) {
            paths = world.getPaths();
        }
    }

    EntityID targetID;
    Random random = new Random(System.currentTimeMillis());

    private List<EntityID> randomCivilianSearch() {

        List<EntityID> pathToGo = new ArrayList<EntityID>();
        List<MrlBuilding> buildingsToSearch;

        MrlBuilding mrlBuilding = null;
        if (targetID == null || world.getSelfPosition().getID().equals(targetID)) {

            // find one building in partition
            if (world.getPartitionManager() != null) {
                Partition searchingPartition = world.getPartitionManager().findHumanPartition(selfHuman);
                if (searchingPartition != null) {
                    buildingsToSearch = findBuildingsToSearch(world.getVisitedBuildings(), searchingPartition.getBuildings());
                    if (!buildingsToSearch.isEmpty()) {
                        mrlBuilding = buildingsToSearch.get(random.nextInt(buildingsToSearch.size()));
                    }
                }
            }
            //find one building in the world when there is no partition for me
            if (mrlBuilding == null) {
                buildingsToSearch = findBuildingsToSearch(world.getVisitedBuildings(), world.getMrlBuildings());
                if (!buildingsToSearch.isEmpty()) {
                    mrlBuilding = buildingsToSearch.get(random.nextInt(buildingsToSearch.size()));
                }
            }

            targetID = mrlBuilding.getID();

        }

        if (targetID != null) {
            //show target in viewer

            pathToGo = me.getPathPlanner().planMove((Area) world.getSelfPosition(), (Area) world.getEntity(targetID), 0, true);
        }

        return pathToGo;
    }

    /**
     * This method finds buildings to go for search, and considers visited buildings to avoid  searching visited ones
     * <p/>
     * <b>WARNING:</b>This method has not good performance
     *
     * @param visitedBuildings visited buildings
     * @param buildings        buildings to search from
     * @return list of {@code MrlBuilding} to search
     */
    private List<MrlBuilding> findBuildingsToSearch(Set<EntityID> visitedBuildings, List<MrlBuilding> buildings) {

        List<MrlBuilding> buildingsToSearch = new ArrayList<MrlBuilding>(buildings);

        for (EntityID buildingID : visitedBuildings) {
            buildingsToSearch.remove(world.getMrlBuilding(buildingID));
        }
        return buildingsToSearch;
    }

    private void resetSearch() {
        paths.clear();
        targetPath = null;
        targetRoad = null;
    }
}
