package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.partitioning.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Area;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Siavash
 *         <p/>
 *         This class manages agents search decision making process
 * @see mrl.platoon.genericsearch.ISearchDecisionMaker , ISearchStrategy
 */
public abstract class SearchManager {

    protected List<Area> targets;
    protected boolean allowMove;
    protected Area targetArea = null;
    protected Partition targetPartition;

    protected SearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        this.world = world;
        this.agent = agent;
        this.decisionMaker = decisionMaker;
        this.searchStrategy = searchStrategy;
        this.targets = new ArrayList<Area>();
        decisionMaker.initialize();
        allowMove = true;
    }

    protected MrlWorld world;

    /**
     * Corresponding searching Agent
     */
    protected MrlPlatoonAgent agent;

    /**
     * Decision making plugin
     */
    protected ISearchDecisionMaker decisionMaker;

    /**
     * Search Method plugin
     */
    protected ISearchStrategy searchStrategy;

    /**
     * Preforms search action
     * <b>Note:</b> Make sure you have set all parameters you need before calling this method
     *
     * @throws mrl.common.CommandException
     * @see ISearchStrategy , ISearchDecisionMaker
     */
    public abstract void execute() throws CommandException;


    public MrlPlatoonAgent getAgent() {
        return agent;
    }

    public void setAgent(MrlPlatoonAgent agent) {
        this.agent = agent;
    }

    public ISearchDecisionMaker getDecisionMaker() {
        return decisionMaker;
    }

    public void setDecisionMaker(ISearchDecisionMaker decisionMaker) {
        this.decisionMaker = decisionMaker;
    }

    public ISearchStrategy getSearchStrategy() {
        return searchStrategy;
    }

    public void setSearchStrategy(ISearchStrategy searchStrategy) {
        this.searchStrategy = searchStrategy;
    }

    public Area getTargetArea() {
        return targetArea;
    }

    public void allowMove(boolean allowMove) {
        this.allowMove = allowMove;
    }

    public boolean isPartitionChanged() {
        Partition myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
        if (targetPartition == null || !targetPartition.equals(myPartition)) {
            targetPartition = myPartition;
            return true;
        }
        return false;
    }
}
