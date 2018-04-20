package mrl.firebrigade.exploration;

import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.firebrigade.targetSelection.FireBrigadeTarget;
import mrl.platoon.genericsearch.ExploreAroundFireDecisionMaker;
import mrl.world.MrlWorld;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/26/13
 * Time: 7:55 PM
 *
 * @Author: Mostafa Shabani
 */
public abstract class ExploreManager {
    protected ExploreManager(MrlFireBrigadeWorld world) {
        this.world = world;
        leaderSelector = new IdBasedExploreLeaderSelector(world);
        exploreDecisionMaker = world.getPlatoonAgent().exploreAroundFireSearchManager.getDecisionMaker();
    }

    public Logger LOGGER = Logger.getLogger(ExploreManager.class);
    protected MrlWorld world;
    protected ExploreAroundFireDecisionMaker exploreDecisionMaker;
    protected IExploreLeaderSelector leaderSelector;
    protected FireBrigadeTarget lastFireBrigadeTarget = null;


    /**
     * This method say when agent can go to exploring in fire
     *
     * @param fireBrigadeTarget target of this agent
     * @return is time to explore or not
     */
    public abstract boolean isTimeToExplore(FireBrigadeTarget fireBrigadeTarget);
}
