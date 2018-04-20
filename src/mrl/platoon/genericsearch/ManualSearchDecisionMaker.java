package mrl.platoon.genericsearch;

import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Siavsah
 */
public class ManualSearchDecisionMaker extends SearchDecisionMaker {
    public ManualSearchDecisionMaker(MrlWorld world) {
        super(world);
    }

    @Override
    public void update() {

    }

    @Override
    public void initialize() {
        throw new UnsupportedOperationException();
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
        super.update();
        List<Area> toRemoveAreas = new ArrayList<Area>();
        for (EntityID areaID : world.getVisitedBuildings()) {
            toRemoveAreas.add((Area) world.getEntity(areaID));
        }
        targets.removeAll(toRemoveAreas);
        return targets;
    }


}
