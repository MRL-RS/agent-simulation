package mrl.platoon.genericsearch;

import mrl.common.Util;
import mrl.helper.CivilianHelper;
import mrl.world.MrlWorld;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Mahdi
 *         Civilian Based Search Strategy
 *         in this search type, agents are force to looking for first civilian who was heard voice of it.
 */
public class CivilianSearchCBDecisionMaker extends CivilianSearchDecisionMaker {
    public CivilianSearchCBDecisionMaker(MrlWorld world) {
        super(world);
    }

    @Override
    protected void setShouldDiscoverBuildings() {
        CivilianHelper civilianHelper = world.getHelper(CivilianHelper.class);
        Set<EntityID> possibleBuildings;
        shouldDiscoverBuildings.clear();
        for (EntityID civId : shouldFindCivilians) {
            possibleBuildings = new HashSet<EntityID>(civilianHelper.getPossibleBuildings(civId));
            if (Util.containsEach(validBuildings, possibleBuildings)) {
                setCivilianInProgress(civId);
                shouldDiscoverBuildings.addAll(possibleBuildings);
                break;
            }
        }
    }
}
