package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;

import static mrl.platoon.genericsearch.SearchStatus.*;

/**
 * @author Siavash
 */
public class StupidSearchManager extends SearchManager {

    private static Log logger = LogFactory.getLog(StupidSearchManager.class);

    public StupidSearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
    }

//    private int tryCounter = 0;

    /**
     * @throws mrl.common.CommandException
     * @see mrl.platoon.genericsearch.SearchManager#execute()
     */
    @Override
    public void execute() throws CommandException {
//        tryCounter++;
//        if(tryCounter > 100){
//            return;
//        }
        decisionMaker.update();
        if (targetArea == null) {

            targetArea = decisionMaker.getNextArea();
            logger.debug("targetArea was null and now is set to: " + targetArea);
        }

        if (targetArea != null && !(targetArea instanceof Road)) {
            logger.fatal("Incompatible type for manual move to road");
            return;
        }
        if (allowMove) {
            SearchStatus status = searchStrategy.manualMoveToRoad((Road) targetArea);

            if (status == CANCELED) {
                status = tryNewTarget(status);
            }

            logger.debug("Search Status: " + status);

            if (status == FINISHED) {
                targetArea = null;
                tryNewTarget(status);
            } else if (status == SEARCHING) {
                //Do Nothing
            }
        }
//        tryCounter = 0;
    }

    private SearchStatus tryNewTarget(SearchStatus status) throws CommandException {
        logger.debug("Search is canceled, trying to acquire new targetArea.");
        Area oldArea = targetArea;
        targetArea = decisionMaker.getNextArea();

        if (oldArea != targetArea) {
            status = searchStrategy.manualMoveToRoad((Road) targetArea);
            if (status == CANCELED) {
                tryNewTarget(status);
            }
        } else {
            logger.debug("Stupid search strategy failed, exiting search procedure.");
        }
        return status;
    }
}
