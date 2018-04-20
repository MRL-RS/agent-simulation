package mrl.ambulance.marketLearnerStrategy;

import mrl.helper.HumanHelper;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;

import java.util.HashSet;

/**
 * Created by P.D.G.
 * User: Pooyad
 * Date: Nov 10, 2010
 * Time: 9:04:16 PM
 */
public class RewardUtill {

    public static double movePunishment = -0.1;
    public static double rescueReward = 0.1;
    public static double loadReward = 2;
    public static double saveReward = 5;
    public static double deadPunishment = -5;


    public static double computeReward(MrlWorld world, HashSet<Civilian> rescuedCivilians, int rescueTimeOfMe, int totalRescueTime, int totalATsInThisRescue, int traveledDistance) {

        int totalSavedCivilians = getTotalSavedCivilians(rescuedCivilians);
        int totalDiscoveredCivilians = getTotalDiscoveredCivilians(world);

        int totalSavedHP = getTotalSavedHP(world, rescuedCivilians);
        int totalDiscoveredHP = getTotalDiscoveredHP(world);

        int remainingCiviliansHP = getRemainingCiviliansHP(world, rescuedCivilians);
        //int totalDiscoveredHP=getTotalDiscoveredHP();

        int maxTravelDistanceInMap = world.getLongestDistanceOfTheMap() * world.getAmbulanceTeams().size();

        //int totalATsInRescue = totalATsInThisRescue;
        int totalATs = world.getAmbulanceTeams().size();

        //todo ===> should I consider total change target as a punishment factor?


        double reward = (totalSavedCivilians / totalDiscoveredCivilians) + (totalSavedHP / totalDiscoveredHP)
                + (rescueTimeOfMe / totalRescueTime);

        double punishment = (remainingCiviliansHP / totalDiscoveredHP) + ((long) traveledDistance / maxTravelDistanceInMap)
                + (totalATsInThisRescue / totalATs);


        //todo ==> Compute RewardUtill
        return reward - punishment;
    }


    /**
     * compute Remained Civilians HP from which is not yet rescued
     *
     * @param world            world
     * @param rescuedCivilians rescuedCivilians
     * @return sum of remained civilians HP
     */
    private static int getRemainingCiviliansHP(MrlWorld world, HashSet<Civilian> rescuedCivilians) {

        int sum = 0;
        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civ = (Civilian) standardEntity;
            if (!rescuedCivilians.contains(civ)) {
                sum += world.getHelper(HumanHelper.class).getCurrentHP(civ.getID());
            }

        }
        return sum;
    }

    private static int getTotalDiscoveredHP(MrlWorld world) {
        int sumOfHPs = 0;
        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) standardEntity;
            if (!civilian.isHPDefined())
                continue;
            sumOfHPs += world.getHelper(HumanHelper.class).getFirstHP(civilian.getID());
        }

        return sumOfHPs;

    }

    private static int getTotalSavedHP(MrlWorld world, HashSet<Civilian> rescuedCivilians) {
        int sumOfHPs = 0;
        for (Civilian rescuedCivilian : rescuedCivilians) {

            sumOfHPs += world.getHelper(HumanHelper.class).getCurrentHP(rescuedCivilian.getID());
        }

        return sumOfHPs;
    }

    private static int getTotalDiscoveredCivilians(MrlWorld world) {
        return world.getCivilians().size();
    }

    private static int getTotalSavedCivilians(HashSet<Civilian> rescuedCivilians) {
        return rescuedCivilians.size();
    }


}
