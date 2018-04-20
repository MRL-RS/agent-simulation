package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;

import static mrl.platoon.genericsearch.SearchStatus.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/26/13
 * Time: 2:29 PM
 *
 * @Author: Mostafa Shabani
 */
public class ExploreAroundFireSearchManager extends SearchManager {
    public ExploreAroundFireSearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
    }

    static int tryCount = 0;
    static int lastTime = 0;
    /**
     * @throws mrl.common.CommandException
     * @see SearchManager#execute()
     */
    @Override
    public void execute() throws CommandException {
        if (lastTime != world.getTime()) {
            tryCount = 0;
            lastTime = world.getTime();
        }
        tryCount++;
        if (tryCount >= 5) {
            tryCount = 0;
            return;
        }
        decisionMaker.update();

        targetArea = decisionMaker.getNextArea();
        if (targetArea == null) {
//            LOGGER.debug("targetArea is null");
            return;
        }

        SearchStatus status = searchStrategy.manualMoveToArea(targetArea);

        if (status == CANCELED) {
            getDecisionMaker().removeUnreachableArea(targetArea.getID());
            execute();
//            LOGGER.debug("unreachable area for search: " + targetArea);
        }

        if (status == FINISHED) {
//            LOGGER.debug("explored area = " + targetArea);
            world.printData("explored area = " + targetArea);
            execute();
        } else if (status == SEARCHING) {
            //Do Nothing
        }
    }

    /**
     * @see SearchManager#getDecisionMaker()
     */
    @Override
    public ExploreAroundFireDecisionMaker getDecisionMaker() {
        return (ExploreAroundFireDecisionMaker) decisionMaker;
    }
}
