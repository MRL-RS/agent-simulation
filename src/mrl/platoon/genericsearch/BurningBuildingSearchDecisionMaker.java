package mrl.platoon.genericsearch;

import javolution.util.FastSet;
import mrl.helper.PropertyHelper;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Siavash
 */
public class BurningBuildingSearchDecisionMaker extends ManualSearchDecisionMaker {

    private int SEARCH_RADIUS = 40000;
    private Set<MrlBuilding> suspiciousBuildings = new FastSet<MrlBuilding>();
    private Set<EntityID> checkedIds = new FastSet<EntityID>();
    PropertyHelper propertyHelper = world.getHelper(PropertyHelper.class);
    private Map<EntityID, Integer> fierynessMap = new HashMap<EntityID, Integer>();

    public BurningBuildingSearchDecisionMaker(MrlWorld world) {
        super(world);
        if (world.isMapHuge()) {
            SEARCH_RADIUS *= 2;
        }
    }

    public void addSuspiciousBuilding(MrlBuilding building) {
        suspiciousBuildings.add(building);
    }

    public void addAllSuspiciousBuildings(Set<MrlBuilding> buildings) {
        suspiciousBuildings.addAll(buildings);
    }

    public void removeSuspiciousBuilding(MrlBuilding building) {
        suspiciousBuildings.remove(building);
        checkedIds.add(building.getID());
        int fness = (building.getSelfBuilding().isFierynessDefined()) ? building.getSelfBuilding().getFieryness() : 0;
        fierynessMap.put(building.getID(), fness);
    }

    private void findSuspiciousBuildings() {

        Set<EntityID> ids = world.getBurningBuildings();
        int fieryness;
        for (EntityID entity : ids) {
            if (world.getSelfPosition().getID().equals(entity)) {
                removeSuspiciousBuilding(world.getMrlBuilding(entity));
                continue;
            }
            fieryness = (world.getMrlBuilding(entity).getSelfBuilding().isFierynessDefined()) ? world.getMrlBuilding(entity).getSelfBuilding().getFieryness() : 0;
            if (checkedIds.contains(entity)) {
                if (!fierynessMap.get(entity).equals(fieryness)) {
                    checkedIds.remove(entity);
                } else {
                    continue;
                }
            }
            if (!checkedIds.contains(entity)) { //do not change it to else!
                checkedIds.add(entity);
                fierynessMap.put(entity, fieryness);
                Collection<StandardEntity> inRange = world.getObjectsInRange(entity, SEARCH_RADIUS);
                for (StandardEntity rangeEntity : inRange) {
                    if (rangeEntity instanceof Building) {
                        MrlBuilding building = world.getMrlBuilding(rangeEntity.getID());
                        fieryness = (building.getSelfBuilding().isFierynessDefined()) ? building.getSelfBuilding().getFieryness() : 0;
                        if (fieryness == 0 || fieryness > 3) {
                            addSuspiciousBuilding(building);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        int lastUpdate = 0;

        for (MrlBuilding tempBuilding : suspiciousBuildings) {
            lastUpdate = propertyHelper.getEntityLastUpdateTime(tempBuilding.getSelfBuilding());
            if ((world.getTime() - lastUpdate) < 2) {
                removeSuspiciousBuilding(tempBuilding);

            }
        }
        findSuspiciousBuildings();
    }

    @Override
    public void initialize() {

    }

    @Override
    public List<Area> evaluateTargets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getNextPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Area getNextArea() {
        throw new UnsupportedOperationException();
    }

    public List<Area> filterTargets(List<Area> targets) {
        targets.clear();
        for (MrlBuilding tempBuilding : suspiciousBuildings) {
            if (!targets.contains(tempBuilding.getSelfBuilding())) {
                targets.add(tempBuilding.getSelfBuilding());
            }
        }
        return targets;
    }


}
