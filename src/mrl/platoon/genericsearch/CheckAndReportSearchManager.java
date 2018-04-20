package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;

import static mrl.platoon.genericsearch.SearchStatus.FINISHED;
import static mrl.platoon.genericsearch.SearchStatus.SEARCHING;

/**
 * Check and Report manager class
 *
 * @author Siavsh
 */
public class CheckAndReportSearchManager extends SearchManager {
    private static Log logger = LogFactory.getLog(CheckAndReportSearchManager.class);
    private Area targetArea = null;

    public CheckAndReportSearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
    }

    @Override
    public void execute() throws CommandException {
        if (targetArea == null) {
            decisionMaker.update();
            targetArea = decisionMaker.getNextArea();
            logger.debug("targetArea was null and now is set to: " + targetArea);
        }

        SearchStatus status = searchStrategy.searchBuilding((Building) targetArea);
        logger.debug("Search Status: " + status);

        if (status == FINISHED) {
            targetArea = null;
        } else if (status == SEARCHING) {
            //Do Nothing
        }
    }
}
