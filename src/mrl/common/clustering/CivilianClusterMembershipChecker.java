package mrl.common.clustering;

import javolution.util.FastSet;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
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
public class CivilianClusterMembershipChecker implements IClusterMembershipChecker {
    //TODO @Vahid Please update javadocs in this class (Take a look at FireClusterMembershipChecker)
    private MrlWorld world;
    private Set<EntityID> notEligibleSet = new FastSet<EntityID>();
    private Set<EntityID> checkedHumans = new FastSet<EntityID>();

    public CivilianClusterMembershipChecker(MrlWorld world) {
        this.world = world;
    }

    @Override
    public boolean checkMembership(Object object) {
        MrlBuilding building = null;
        if (object instanceof MrlBuilding) {
            if (checkInBuilding((MrlBuilding) object)) {
                return true;
            }
        } else {
            throw new IllegalArgumentException("Incompatible object with this condition checker is used.");
        }
        return false;
    }

    private boolean checkInBuilding(MrlBuilding building) {
        Human human;

        if (notEligibleSet.contains(building.getID())) {
            return false;
        }
        for (StandardEntity se : world.getHumans()) {
            if (checkedHumans.contains(se.getID())) {
                continue;
            }
            human = (Human) se;
            if (human.isPositionDefined() && human.getPosition() == building.getID()
                    && human.isBuriednessDefined() && human.getBuriedness() > 0
                    && human.isHPDefined() && human.getHP() > 0) {
                checkedHumans.add(se.getID());
                return true;
            }


        }
        notEligibleSet.add(building.getID());
        return false;
    }
}
