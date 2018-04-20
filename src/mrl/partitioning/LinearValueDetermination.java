package mrl.partitioning;

import mrl.common.MRLConstants;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/5/12
 * Time: 12:52 PM
 */
public class LinearValueDetermination implements IPartitionValueDetermination {


    public LinearValueDetermination(MrlWorld world) {

        computeCapacities(world);

    }

    private double policeForcesCapacity;
    private double fireBrigadesCapacity;
    private double ambulanceTeamsCapacity;
    private double refugesCapacity;

    @Override
    public double computeValue(MrlWorld world, Partition partition) {

        double value = 0;
        StandardEntity entity;
        int numberOfBlockedFBs = 0;
        int numberOfBlockedATs = 0;
        int numberOfBuriedFBs = 0;
        int numberOfBuriedATs = 0;
        int numberOfBuriedPFs = 0;
        int numberOfRefuges = partition.getRefuges().size();

        for (EntityID id : partition.getBlockedAgents()) {
            entity = world.getEntity(id);

            if (entity instanceof FireBrigade) {
                numberOfBlockedFBs++;
            } else if (entity instanceof AmbulanceTeam) {
                numberOfBlockedATs++;
            }
        }

        for (EntityID id : partition.getBuriedAgents()) {
            entity = world.getEntity(id);

            if (entity instanceof FireBrigade) {
                numberOfBuriedFBs++;
            } else if (entity instanceof AmbulanceTeam) {
                numberOfBuriedATs++;
            } else if (entity instanceof PoliceForce) {
                numberOfBuriedPFs++;
            }
        }


        value = fireBrigadesCapacity * numberOfBlockedFBs * PolicePartitionImportance.BLOCKED_FB_AGENT.getImportance()
                + fireBrigadesCapacity * numberOfBuriedFBs * PolicePartitionImportance.BURIED_FB_AGENT.getImportance()

                + ambulanceTeamsCapacity * numberOfBlockedATs * PolicePartitionImportance.BLOCKED_AT_AGENT.getImportance()
                + ambulanceTeamsCapacity * numberOfBuriedATs * PolicePartitionImportance.BURIED_AT_AGENT.getImportance()

                + policeForcesCapacity * numberOfBuriedPFs * PolicePartitionImportance.BURIED_PF_AGENT.getImportance()

                + refugesCapacity * numberOfRefuges * PolicePartitionImportance.REFUGE_BUILDING.getImportance();


        return Math.round(value);
    }


    private void computeCapacities(MrlWorld world) {

        if (!world.getPoliceForces().isEmpty()) {
            policeForcesCapacity = (double) MRLConstants.MAX_POLICE_FORCES / (double) world.getPoliceForces().size();
        } else {
            policeForcesCapacity = 0;
        }

        if (!world.getFireBrigades().isEmpty()) {
            fireBrigadesCapacity = (double) MRLConstants.MAX_FIRE_BRIGADES / (double) world.getFireBrigades().size();
        } else {
            fireBrigadesCapacity = 0;
        }

        if (!world.getAmbulanceTeams().isEmpty()) {
            ambulanceTeamsCapacity = (double) MRLConstants.MAX_AMBULANCE_TEAMS / (double) world.getAmbulanceTeams().size();
        } else {
            ambulanceTeamsCapacity = 0;
        }

        if (!world.getRefuges().isEmpty()) {
            if (world.isMapHuge()) {
                refugesCapacity = (double) MRLConstants.MAX_BIG_MAP_REFUGES / (double) world.getRefuges().size();
            } else if (world.isMapMedium()) {
                refugesCapacity = (double) MRLConstants.MAX_MEDIUM_MAP_REFUGES / (double) world.getRefuges().size();
            } else {
                refugesCapacity = (double) MRLConstants.MAX_SMALL_MAP_REFUGES / (double) world.getRefuges().size();
            }

        } else {
            ambulanceTeamsCapacity = 0;
        }


    }
}
