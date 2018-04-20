package mrl.platoon.genericsearch;

import javolution.util.FastMap;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Siavash
 */
public class CheckAndReportDecisionMaker extends SearchDecisionMaker {

    private List<EntityID> checkList;
    private Map<EntityID, Integer> checked;

    public CheckAndReportDecisionMaker(MrlWorld world) {
        super(world);
        this.checkList = new ArrayList<EntityID>();
        checkList = new ArrayList<EntityID>();
        checked = new FastMap<EntityID, Integer>();
    }

    @Override
    public void initialize() {

        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) world.getEntity(standardEntity.getID());
            if (civilian.isPositionDefined() && civilian.isHPDefined()) {
//                if(visitedBuildings.contains(civilian.getPosition(world).getID())){
//                    continue;
//                }
                if (civilian.getHP() > 10
                        && civilian.getPosition(world) instanceof Building
                        && !(civilian.getPosition(world) instanceof Refuge)) {
                    checkList.add(standardEntity.getID());
                }
            }
        }
    }

    @Override
    public void update() {
        for (StandardEntity standardEntity : world.getCivilians()) {

            Civilian civilian = (Civilian) world.getEntity(standardEntity.getID());
            if (civilian.isPositionDefined() && civilian.isHPDefined()) {
//                if(visitedBuildings.contains(civilian.getPosition(world).getID())){
//                    continue;
//                }
                if (civilian.getHP() > 10
                        && civilian.getPosition(world) instanceof Building
                        && !(civilian.getPosition(world) instanceof Refuge)) {
                    if (!checkList.contains(standardEntity.getID())) {
                        checkList.add(standardEntity.getID());
                    }
                } else {
                    checkList.remove(standardEntity.getID());
                }
            }
        }

        for (EntityID entityID : checked.keySet()) {
            int val = checked.get(entityID);
            if (val > 0) {
                val--;
                checked.put(entityID, val);
            } else {
                checked.remove(entityID);
            }
        }

        checkList.removeAll(checked.keySet());
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
        Civilian civilian = null;
        Civilian selectedCivilian = null;
        int minDistance = Integer.MAX_VALUE;
        int distance = 0;
        for (EntityID entityId : checkList) {
            civilian = (Civilian) world.getEntity(entityId);
            distance = world.getDistance(civilian, world.getSelfPosition());
            if (distance < minDistance) {
                minDistance = distance;
                selectedCivilian = civilian;
            }
        }
        Area civilianPosition = null;
        if (civilian != null) {
            civilianPosition = (Area) civilian.getPosition(world);
        }
        return civilianPosition;

    }
}
