package mrl.platoon.genericsearch;

import mrl.helper.CivilianHelper;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Mahdi
 *         Building Base of Civilian Search:
 */
public class CivilianSearchBBDecisionMaker extends CivilianSearchDecisionMaker {
    public CivilianSearchBBDecisionMaker(MrlWorld world) {
        super(world);
    }

    @Override
    protected void setShouldDiscoverBuildings() {
        CivilianHelper civilianHelper = world.getHelper(CivilianHelper.class);
        Set<EntityID> possibleBuildings;
        MrlBuilding mrlBuilding;
        shouldDiscoverBuildings.clear();
        for (EntityID civId : shouldFindCivilians) {
            possibleBuildings = new HashSet<EntityID>(civilianHelper.getPossibleBuildings(civId));
            for (EntityID possibleBuildingID : possibleBuildings) {
                mrlBuilding = world.getMrlBuilding(possibleBuildingID);
//                if(mrlBuilding.isVisited()){
//                    continue;
//                }
                mrlBuilding.addCivilianPossibly(civId);
                shouldDiscoverBuildings.add(possibleBuildingID);
            }
        }
    }


}
