package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;

import static mrl.platoon.genericsearch.SearchStatus.FINISHED;
import static mrl.platoon.genericsearch.SearchStatus.SEARCHING;

/**
 * @author Siavash
 */
public class CheckBlockadesManager extends SearchManager {

    private static Log logger = LogFactory.getLog(CheckBlockadesManager.class);

    public CheckBlockadesManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
    }

    private Area targetArea = null;

    /**
     * Preforms search action
     * <b>Note:</b> Make sure you have set all parameters you need before calling this method
     *
     * @throws mrl.common.CommandException
     * @see ISearchStrategy , ISearchDecisionMaker
     */
    @Override
    public void execute() throws CommandException {
        decisionMaker.update();
        if (targetArea == null) {

            targetArea = decisionMaker.getNextArea();
            logger.debug("targetArea was null and now is set to: " + targetArea);
        }

        if (targetArea == null) {
            return;
        }

        if (!(targetArea instanceof Road)) {
            logger.fatal("Incompatible type for manual move to road");
            return;
        }

        SearchStatus status = searchStrategy.manualMoveToRoad((Road) targetArea);

        logger.debug("Search Status: " + status);

        if (status == FINISHED) {
            targetArea = null;
            execute();
        } else if (status == SEARCHING) {
            //Do Nothing
        }
    }
}
