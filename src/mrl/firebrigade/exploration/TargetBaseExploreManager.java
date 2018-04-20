package mrl.firebrigade.exploration;

import javolution.util.FastSet;
import mrl.MrlPersonalData;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.firebrigade.targetSelection.FireBrigadeTarget;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/26/13
 * Time: 7:55 PM
 *
 * @Author: Mostafa Shabani
 */
public class TargetBaseExploreManager extends ExploreManager {
    public TargetBaseExploreManager(MrlFireBrigadeWorld world) {
        super(world);
    }

    protected FireBrigadeTarget exploringTarget = null;
    protected Area preFBTargetArea = null;
    private int timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
    private static final int MAX_TIME_TO_EXTINGUISH = 4;
    private static final int MAX_TIME_TO_EXTINGUISH_CL = 4;
    private boolean reachedToFirstTarget = true;

    /**
     * In this strategy fireBrigade "MAX_TIME_TO_EXTINGUISH" to extinguish only.
     * if time to extinguish finished and target is not extinguished FB should be goto explore for all explore targets and back to extinguish work.
     * zamani ke map CL bashad har 4 cycle explore mikonad. va dar gheire pas az 6 cycle extinguish momtad.
     *
     * @param fireBrigadeTarget target of this agent
     * @return boolean is true equals now goto explore
     */
    @Override
    public boolean isTimeToExplore(FireBrigadeTarget fireBrigadeTarget) {
        boolean returnVal = false;
        Area area = exploreDecisionMaker.getNextArea();

        if (lastFireBrigadeTarget != null) {
            Integer lastClusterId = lastFireBrigadeTarget.getCluster().getId();
            Integer exploringClusterId = (exploringTarget == null ? null : exploringTarget.getCluster().getId());
            Integer newClusterId = (fireBrigadeTarget == null ? null : fireBrigadeTarget.getCluster().getId());
            boolean targetChanged = !lastClusterId.equals(newClusterId) || (exploringClusterId != null && !exploringClusterId.equals(newClusterId));
            boolean fireIsNear = fireBrigadeTarget != null && world.getDistance(fireBrigadeTarget.getMrlBuilding().getID(), world.getSelfPosition().getID()) <= world.getViewDistance();


            if (!reachedToFirstTarget && area != null && !fireIsNear) {
                if (preFBTargetArea != null && preFBTargetArea.getID().equals(world.getSelfPosition().getID())) {
                    reachedToFirstTarget = true;
//                    world.printData("reachedToFirstTarget " + preFBTargetArea);
                } else {
                    lastFireBrigadeTarget = fireBrigadeTarget;
                    preFBTargetArea = area;
                    return true;
                }
            }

            if (targetChanged && area != null && !fireIsNear) {
                lastFireBrigadeTarget = fireBrigadeTarget;
//                world.printData("continue explore goto " + area);
                return true;
            }

            EntityID clusterLeaderId = leaderSelector.findLeader(lastFireBrigadeTarget);
            if (clusterLeaderId != null && world.getSelf().getID().equals(clusterLeaderId)) {
                if (fireBrigadeTarget == null || targetChanged || timeToExtinguish == 0) {
                    timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
                    if (world.isCommunicationLess()) {
                        timeToExtinguish = MAX_TIME_TO_EXTINGUISH_CL;
                    }
                    exploreDecisionMaker.setTargetFire(lastFireBrigadeTarget, fireBrigadeTarget == null);
                    preFBTargetArea = exploreDecisionMaker.getNextArea();
                    exploringTarget = lastFireBrigadeTarget;
                    reachedToFirstTarget = false;
                    returnVal = true;
//                    LOGGER.debug("now I'm goto exploring cluster " + lastFireBrigadeTarget.getCluster().getId() + " places:" + exploreTargets);
//                    world.printData("now I'm goto exploring cluster " + lastFireBrigadeTarget.getCluster().getId() + "  places: " + exploreDecisionMaker.getExploreTargets());
                } else if (world.getPlatoonAgent().getLastCommand().equalsIgnoreCase("Extinguish")) {
                    timeToExtinguish--;
                } else if (!world.getPlatoonAgent().getLastCommand().equalsIgnoreCase("Extinguish") && !world.isCommunicationLess()) {
                    timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
                }
            }
        } else if (area != null) {
            returnVal = true;
        }
        lastFireBrigadeTarget = fireBrigadeTarget;
        MrlPersonalData.VIEWER_DATA.setExploreBuildings(world.getSelf().getID(), new FastSet<MrlBuilding>());

        return returnVal;
    }
}
