package mrl.platoon.genericsearch;

import javolution.util.FastSet;
import mrl.MrlPersonalData;
import mrl.helper.BuildingHelper;
import mrl.helper.CivilianHelper;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Siavash
 */
public class PossibleBuildingSearchDecisionMaker extends SearchDecisionMaker {

    private static Log logger = LogFactory.getLog(PossibleBuildingSearchDecisionMaker.class);

    private Set<EntityID> visited;
    private List<EntityID> unknownCivilians;
    private CivilianHelper civilianHelper;
    private List<EntityID> possibleBuildings;

    public PossibleBuildingSearchDecisionMaker(MrlWorld world) {
        super(world);
        this.visited = new FastSet<EntityID>();
        this.unknownCivilians = new ArrayList<EntityID>();
        this.possibleBuildings = new ArrayList<EntityID>();
        this.civilianHelper = world.getHelper(CivilianHelper.class);
        initialize();
    }

    @Override
    public void initialize() {
        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) world.getEntity(standardEntity.getID());
            if (!civilian.isPositionDefined()) {
                unknownCivilians.add(standardEntity.getID());
            }
            possibleBuildings.addAll(civilianHelper.getPossibleBuildings(standardEntity.getID()));
        }
    }

    /**
     * @see ISearchDecisionMaker#update()
     */
    @Override
    public void update() {
        super.update();
        List<EntityID> tempPossibleBuildings = new ArrayList<EntityID>();
        visited.addAll(world.getVisitedBuildings());

        if (world.isMapHuge()) {
            Set<EntityID> inRange = new FastSet<EntityID>();
            for (StandardEntity entity : world.getObjectsInRange(world.getSelfPosition(), world.getViewDistance())) {
                if (entity instanceof Building) {
                    inRange.add(entity.getID());
                }
            }

            for (EntityID entityID : inRange) {
                if (!possibleBuildings.contains(entityID)) {
                    possibleBuildings.add(entityID);
                }
            }
        }
//        possibleBuildings.clear();
        for (StandardEntity civilianEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) civilianEntity;

            if (!civilian.isPositionDefined()) {
                logger.debug("T:" + world.getTime() + "  Civilian without position: [" + civilian + "]");

                if (!unknownCivilians.contains(civilianEntity.getID())) {
                    unknownCivilians.add(civilianEntity.getID());
                }
                possibleBuildings.addAll(civilianHelper.getPossibleBuildings(civilianEntity.getID()));
            } else if (unknownCivilians.contains(civilianEntity.getID())) {
                logger.debug("removing from unknown civilian.");
                unknownCivilians.remove(civilianEntity.getID());
                for (EntityID entityID : civilianHelper.getPossibleBuildings(civilianEntity.getID())) {
                    possibleBuildings.remove(entityID);
                }
            }
        }

        if (visited != null) {
            logger.debug("Visited is not null");
            for (EntityID entityID : visited) {
                while (possibleBuildings.contains(entityID)) {
                    possibleBuildings.remove(entityID);
                }
            }
        }

        if (searchInPartition) {
            for (EntityID id : possibleBuildings) {
                if (validBuildings.contains(id)) {
                    tempPossibleBuildings.add(id);
                }
            }

            possibleBuildings = tempPossibleBuildings;
        }

        MrlPersonalData.VIEWER_DATA.setPossibleBuildings(world.getPlatoonAgent().getID(), possibleBuildings);

    }

    /**
     * @see ISearchDecisionMaker#evaluateTargets()
     */
    @Override
    public List<Area> evaluateTargets() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ISearchDecisionMaker#getNextPath()
     */
    @Override
    public Path getNextPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Area getNextArea() {
        Building possibleBuilding = null;
        Area selectedBuilding = null;
        int minDistance = Integer.MAX_VALUE;
        int distance = 0;
        for (EntityID entityId : possibleBuildings) {
            possibleBuilding = (Building) world.getEntity(entityId);

            if (possibleBuilding.isFierynessDefined()
                    && possibleBuilding.getFieryness() > 0) {
                visited.add(entityId);
                continue;
            }

            List<Road> entrances = BuildingHelper.getEntranceRoads(world, possibleBuilding);
            if (entrances.isEmpty()) {
                visited.add(entityId);
                continue;
            }
            distance = world.getDistance(entrances.get(0).getID(), world.getSelfPosition().getID());
            if (distance < minDistance) {
                minDistance = distance;
                selectedBuilding = possibleBuilding;
            }
        }
        if (selectedBuilding != null) {
            visited.add(selectedBuilding.getID());
        }
        logger.debug("Selected building: [" + selectedBuilding + "]");
        return selectedBuilding;
    }

}
