package mrl.platoon.genericsearch;

import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.police.MrlPoliceForce;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Mahdi
 */
public abstract class CivilianSearchDecisionMaker extends SearchDecisionMaker {
    protected Set<EntityID> zeroBrokennessBuildings;
    protected Set<EntityID> shouldDiscoverBuildings;
    protected Set<EntityID> shouldFindCivilians;
    private Set<EntityID> unreachableCivilians;
    private Set<EntityID> discoveredBuilding;
    private List<Path> shouldDiscoverPaths;
    private EntityID civilianInProgress;
    private EntityID buildingInProgress;
    private Path pathInProgress;

    public CivilianSearchDecisionMaker(MrlWorld world) {
        super(world);
        initialize();
    }

    @Override
    public void initialize() {
        setBuildingInProgress(null);
        setCivilianInProgress(null);
        setPathInProgress(null);
        zeroBrokennessBuildings = new HashSet<EntityID>();
        discoveredBuilding = new HashSet<EntityID>();
        shouldFindCivilians = new HashSet<EntityID>();
        shouldDiscoverBuildings = new HashSet<EntityID>();
        unreachableCivilians = new HashSet<EntityID>();
        shouldDiscoverPaths = new ArrayList<Path>(validPaths);
    }

    @Override
    public void update() {
        super.update();
        setShouldFindCivilians();
        shouldFindCivilians.removeAll(unreachableCivilians);
        setShouldDiscoverBuildings();
        if (searchInPartition) {
            shouldDiscoverBuildings.retainAll(validBuildings);
        }
        removeZeroBrokennessBuildings();
        removeBurningBuildings();
        removeVisitedBuildings();
        if (!(world.getPlatoonAgent() instanceof MrlPoliceForce)) {
            removeUnreachableBuildings();
        }
        updateCivilianPossibleValues();

        MrlPersonalData.VIEWER_DATA.setCivilianData(world.getPlatoonAgent().getID(), shouldDiscoverBuildings, civilianInProgress, buildingInProgress);

    }

    private void removeUnreachableBuildings() {
        List<EntityID> toRemove = new ArrayList<EntityID>();
        MrlBuilding mrlBuilding;
        for (EntityID bID : shouldDiscoverBuildings) {
            mrlBuilding = world.getMrlBuilding(bID);
            if (!mrlBuilding.isVisitable()) {
                toRemove.add(bID);
            }
        }
        for (EntityID bID : notVisitable.keySet()) {
            if (notVisitable.get(bID) < MRLConstants.BUILDING_PASSABLY_RESET_TIME) {
                toRemove.add(bID);
            }
        }
        shouldDiscoverBuildings.removeAll(toRemove);

        MrlPersonalData.VIEWER_DATA.setUnreachableBuildings(world.getPlatoonAgent().getID(), new HashSet<EntityID>(toRemove));
    }

    private void removeVisitedBuildings() {
        shouldDiscoverBuildings.removeAll(world.getVisitedBuildings());
    }

    @Override
    public List<Area> evaluateTargets() {
        return null;
    }

    @Override
    public Path getNextPath() {
        if (shouldDiscoverPaths.isEmpty()) {
            return null;
        }
        pathInProgress = shouldDiscoverPaths.remove(0);
        return pathInProgress;
    }

    @Override
    public Area getNextArea() {
        EntityID greatestValue = null;
        double maxValue = 0;
        MrlBuilding mrlBuilding;
        for (EntityID buildingID : shouldDiscoverBuildings) {
            mrlBuilding = world.getMrlBuilding(buildingID);
            double value = mrlBuilding.getCivilianPossibleValue();
            if (value > maxValue) {
                maxValue = value;
                greatestValue = buildingID;
            }
        }

        if (greatestValue == null) {
            return null;
        }

        return world.getEntity(greatestValue, Area.class);
    }

    /**
     * set civilian possible value every cycle.
     * number of civilian whom voice of them heard around / time to arrive
     * finally *2 value for buildings that in a same path with current agent.
     */
    public void updateCivilianPossibleValues() {
        MrlBuilding mrlBuilding;
        for (EntityID bID : shouldDiscoverBuildings) {
            mrlBuilding = world.getMrlBuilding(bID);
            double civilianPossibleValue = mrlBuilding.getCivilianPossibly().size();
            if (civilianPossibleValue != 0) {
                StandardEntity position = world.getSelfPosition();
                double distance = Util.distance(world.getSelfLocation(), mrlBuilding.getSelfBuilding().getLocation(world));
                double timeToArrive = distance / MRLConstants.MEAN_VELOCITY_OF_MOVING;
                if (timeToArrive > 0) {
                    civilianPossibleValue /= timeToArrive;
                    //set double value for buildings that inside current path!
                    if (position instanceof Road) {
                        MrlRoad mrlRoad = world.getMrlRoad(position.getID());
                        for (Path path : mrlRoad.getPaths()) {
                            if (path.getBuildings().contains(mrlBuilding.getSelfBuilding())) {
                                civilianPossibleValue *= 2;
                            }
                        }
                    }
                } else {
                    civilianPossibleValue = 0;
                }
            }
            mrlBuilding.setCivilianPossibleValue(civilianPossibleValue);
        }
    }

    public Area getBetterTarget(Area presentTarget) {
        Area bestArea = getNextArea();
        MrlBuilding presentBuildingTarget = world.getMrlBuilding(presentTarget.getID());
        if (bestArea instanceof Building) {
            MrlBuilding bestBuilding = world.getMrlBuilding(bestArea.getID());
            if (bestBuilding.getCivilianPossibleValue() >= presentBuildingTarget.getCivilianPossibleValue() * 2) {
                return bestArea;
            }
        }
        return null;
    }

    private void removeBurningBuildings() {
        Building building;
        Set<EntityID> toRemove = new HashSet<EntityID>();
        for (EntityID buildingID : shouldDiscoverBuildings) {
            building = world.getEntity(buildingID, Building.class);
            if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() != 4) {
                toRemove.add(buildingID);
            }
        }
        if (toRemove.size() > 0) {
            System.out.print("");
        }
        shouldDiscoverBuildings.removeAll(toRemove);
    }

    private void setShouldFindCivilians() {
        shouldFindCivilians.clear();
        for (StandardEntity civEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) civEntity;
            if (!civilian.isPositionDefined()) {
                shouldFindCivilians.add(civilian.getID());
            }
        }
    }

    /**
     * Fill buildings that have civilian possibly to discover them!
     * It will get information of possible buildings of civilians whom heard voice of them from CivilianHelper
     */
    protected abstract void setShouldDiscoverBuildings();

    public void result(SearchResult result) {
        MrlBuilding mrlBuilding;

        switch (result) {
            case SUCCESSFUL:
//                discoveredBuilding.add(buildingInProgress);
//                shouldDiscoverBuildings.remove(buildingInProgress);
//                shouldFindCivilians.remove(civilianInProgress);
                buildingInProgress = null;
                civilianInProgress = null;
                break;
            case UNREACHABLE:
                notVisitable.put(buildingInProgress, world.getTime());
//                mrlBuilding = world.getMrlBuilding(buildingInProgress);
//                if (mrlBuilding != null)
//                    mrlBuilding.setReachable(false);
                break;
            case NO_VALID_TARGET:
                buildingInProgress = null;
                civilianInProgress = null;
                break;
        }
    }

    public void setCivilianInProgress(EntityID civilianInProgress) {
        this.civilianInProgress = civilianInProgress;
    }

    public void setBuildingInProgress(EntityID buildingInProgress) {
        this.buildingInProgress = buildingInProgress;
    }

    public void setPathInProgress(Path pathInProgress) {
        this.pathInProgress = pathInProgress;
    }

    public void removeZeroBrokennessBuildings() {
        Building building;
        List<EntityID> toRemove = new ArrayList<EntityID>();
        for (EntityID buildingID : shouldDiscoverBuildings) {
            building = world.getEntity(buildingID, Building.class);
            if (building.isBrokennessDefined() && building.getBrokenness() == 0) {
                toRemove.add(buildingID);
            }
        }
        shouldDiscoverBuildings.removeAll(toRemove);
    }

    public enum SearchResult {
        SUCCESSFUL,
        UNREACHABLE,
        NO_VALID_TARGET,
    }
}


