package mrl.common.clustering;

import javolution.util.FastSet;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.Set;

/**
 * Version 1.1: Renamed from CivilianCluster
 *
 * @author vahid Hooshangi
 * @version 1.1
 */
public class HumanClusterMembershipChecker implements IClusterMembershipChecker {
    private MrlWorld world;
    private Set<EntityID> notEligibleSet = new FastSet<EntityID>();
    private Set<EntityID> checkedHumans = new FastSet<EntityID>();

    public HumanClusterMembershipChecker(MrlWorld world) {
        this.world = world;
    }

    @Override
    public boolean checkMembership(Object object) {
        boolean isMember = false;
        if (object instanceof Human) {
            Human human = (Human) object;
            if (human.isHPDefined()
                    && human.isBuriednessDefined()
                    && human.getHP() > 1000
                    && human.getBuriedness() > 0
                    && human.getPosition(world) instanceof Building
                    ) {
                isMember = true;
            }
        } else {
            throw new IllegalArgumentException("Incompatible object with this condition checker is used.");
        }
        return isMember;
    }

    private boolean checkInBuilding(MrlBuilding building) {
        Human human;

/*        if(notEligibleSet.contains(building.getID())){
            return false;
        }*/
        for (StandardEntity se : world.getHumans()) {
//            if(checkedHumans.contains(se.getID())){
//                continue;
//            }
            human = (Human) se;
            if (human.isPositionDefined() && human.getPosition().equals(building.getID())
                    && human.isBuriednessDefined() && human.getBuriedness() > 0
                    && human.isHPDefined() && human.getHP() > 0) {
//                checkedHumans.add(se.getID());
                return true;
            }


        }
        notEligibleSet.add(building.getID());
        return false;
    }
}
