package mrl.firebrigade;

import mrl.common.MRLConstants;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: 5/14/11
 * Time: 5:30 PM
 */
public class FireBrigadeDecisionManager {
    MrlFireBrigadeWorld world;
    MrlZone targetMrlZone;
    MrlBuilding target;
    DirectionManager directionManager;
    List<MrlZone> preferredSearchZones = new ArrayList<MrlZone>();

    public FireBrigadeDecisionManager(MrlFireBrigadeWorld world, DirectionManager directionManager) {
        this.world = world;
        this.directionManager = directionManager;
    }

    public MrlZone selectFireZones(Collection<MrlZone> zones) {
        if (world.getZones() == null) {
            return null;
        }
        if (world.getZones().getBurningZones().isEmpty()) {
            target = null;
            return null;
        }

        if (targetMrlZone != null) {
            targetMrlZone.increaseLastTargetZone();
        }
        assignZones(zones);
        if (targetMrlZone == null && !zones.isEmpty()) {
            if (directionManager.isDirectionEnabled()) {
                targetMrlZone = zones.iterator().next();
            } else {
                preferredSearchZones.clear();
                preferredSearchZones.addAll(zones);
            }

        }
        if (targetMrlZone == null) {
            target = null;
//            if (MRLConstants.DEBUG_FIRE_BRIGADE) {
//                System.out.println(">>>>>>>> T:" + world.getTime() + " me:" + world.getSelf().getID() + " myAssignedZone: NULL");
//            }

            return null;
        }

        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println(">>>>>>>> T:" + world.getTime() + " me:" + world.getSelf().getID() + " myAssignedZone: " + targetMrlZone.toString());
        }
        return targetMrlZone;
    }

    private void assignZones(Collection<MrlZone> zones) {
        List<StandardEntity> freeAgents = world.getFreeFireBrigades();
        List<MrlZone> burningMrlZones = new ArrayList<MrlZone>();
        burningMrlZones.addAll(zones);

        if (targetMrlZone != null && !burningMrlZones.contains(targetMrlZone)) {
            world.getZones().pushExtinguishedZones(targetMrlZone);
        }

        Collections.sort(burningMrlZones);
        StandardEntity nearestAgent = null;
        int minDistance = Integer.MAX_VALUE;
        Integer distance;
        int needed;

        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println(world.getTime() + " me:" + world.getSelf() + "  -------------------------   BURNING ZONES:");
        }

        for (MrlZone zone : burningMrlZones) {
            if (MRLConstants.DEBUG_FIRE_BRIGADE) {
                System.out.println(zone.toString());
                System.out.print("assigned agents = ");
            }
            zone.updateAgentDistance();
            needed = zone.getNeededAgentsToExtinguish();

            while (needed-- > 0) {
                for (StandardEntity agent : freeAgents) {
                    distance = zone.getAgentDistance(agent.getID());
                    if (distance != null && distance < minDistance) {
                        minDistance = distance;
                        nearestAgent = agent;
                    }
                }
                if (nearestAgent == null) {
                    break;
                } else {
                    if (MRLConstants.DEBUG_FIRE_BRIGADE) {
                        System.out.print(nearestAgent.getID().getValue() + ", ");
                    }
                    freeAgents.remove(nearestAgent);
                    minDistance = Integer.MAX_VALUE;
                    if (nearestAgent.equals(world.getSelfHuman())) {
                        targetMrlZone = zone;
                    }
                }
            }
            if (MRLConstants.DEBUG_FIRE_BRIGADE) {
                System.out.println();
            }
        }
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("------------------------------------------------------------------------------------------");
        }
    }

    public MrlBuilding selectBuilding() {
        if (targetMrlZone == null) {
            return null;
        }
        target = targetMrlZone.getBestBuilding(target);
        if (target == null) {

            if (targetMrlZone != null && !world.getZones().getBurningZones().contains(targetMrlZone)) {
                world.getZones().pushExtinguishedZones(targetMrlZone);
            }

            targetMrlZone = null;
        }
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            world.printData("TARGET BUILDING = " + target);
            System.out.println("--------------------------------------------------------------------------------------------");
            System.out.println();
        }
        return target;
    }

    public List<MrlZone> getPreferredSearchZones() {
        return preferredSearchZones;
    }
}
