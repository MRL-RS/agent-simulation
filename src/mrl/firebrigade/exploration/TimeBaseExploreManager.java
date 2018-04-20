package mrl.firebrigade.exploration;

import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.firebrigade.targetSelection.FireBrigadeTarget;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/23/13
 * Time: 3:51 PM
 *
 * @Author: Mostafa Shabani
 */
public class TimeBaseExploreManager extends ExploreManager {
    public TimeBaseExploreManager(MrlFireBrigadeWorld world) {
        super(world);
    }

    private int timeToExplore = 0;
    private int timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
    private static final int MAX_TIME_TO_EXPLORE = 4;
    private static final int MAX_TIME_TO_EXTINGUISH = 8;

    /**
     * In this strategy fireBrigade have "MAX_TIME_TO_EXPLORE" to explore and "MAX_TIME_TO_EXTINGUISH" to extinguish.
     * if time to extinguish finished and target is not extinguished FB should be goto explore for MAX_TIME_TO_EXPLORE and back to extinguish work.
     *
     * @param fireBrigadeTarget target of this agent
     * @return boolean is true equals now goto explore
     */
    @Override
    public boolean isTimeToExplore(FireBrigadeTarget fireBrigadeTarget) {
        if (timeToExplore-- > 0) {
            lastFireBrigadeTarget = fireBrigadeTarget;
            return true;
        } else {
            timeToExplore = 0;
        }

        boolean returnVal = false;
        if (lastFireBrigadeTarget != null) {

            EntityID clusterLeaderId = leaderSelector.findLeader(lastFireBrigadeTarget);
            if (clusterLeaderId != null && world.getSelf().getID().equals(clusterLeaderId)) {
                if (fireBrigadeTarget == null || lastFireBrigadeTarget.getCluster().getId() != fireBrigadeTarget.getCluster().getId() || timeToExtinguish == 0) {
                    timeToExplore = MAX_TIME_TO_EXPLORE;
                    timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
                    exploreDecisionMaker.setTargetFire(lastFireBrigadeTarget, fireBrigadeTarget == null);
                    returnVal = true;
//                    LOGGER.debug("now I'm goto exploring cluster " + lastFireBrigadeTarget.getCluster().getId() + " places:" + exploreTargets);
                } else if (world.getPlatoonAgent().getLastCommand().equalsIgnoreCase("Extinguish")) {
                    timeToExtinguish--;
                }
            }
        } else if (exploreDecisionMaker.getNextArea() != null && fireBrigadeTarget == null) {
            returnVal = true;
        }
        lastFireBrigadeTarget = fireBrigadeTarget;
        return returnVal;
    }
}
