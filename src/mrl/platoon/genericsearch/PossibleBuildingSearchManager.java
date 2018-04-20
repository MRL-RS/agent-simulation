package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;

import static mrl.platoon.genericsearch.SearchStatus.*;

/**
 * @author Siavash
 */
public class PossibleBuildingSearchManager extends SearchManager {

    private static Log logger = LogFactory.getLog(PossibleBuildingSearchManager.class);

    public PossibleBuildingSearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
        targetArea = null;
    }

    private Area targetArea = null;

    /**
     * @throws mrl.common.CommandException
     * @see SearchManager#execute()
     */
    @Override
    public void execute() throws CommandException {
        logger.debug(world.getSelf() + " time: " + world.getTime() + "Execute.");
        decisionMaker.update();
        if (targetArea == null) {

            targetArea = decisionMaker.getNextArea();
            logger.debug(world.getSelf() + " time: " + world.getTime() + "targetArea was null and now is set to: " + targetArea);
        }

        if (targetArea != null) {
            logger.debug(world.getSelf() + " time: " + world.getTime() + " targetArea to search : " + targetArea.getID());
        } else {
            logger.debug(world.getSelf() + " time: " + world.getTime() + " targetArea to search : NULL");

        }

        SearchStatus status = searchStrategy.searchBuilding((Building) targetArea);


        if (status == CANCELED) {
            status = tryNewTarget(status);
        }

        logger.debug(world.getSelf() + " time: " + world.getTime() + "Search Status: " + status);

        if (status == FINISHED) {
            targetArea = null;
            execute();
        } else if (status == SEARCHING) {
            //Do Nothing
        }
    }

    private SearchStatus tryNewTarget(SearchStatus status) throws CommandException {
        logger.debug(world.getSelf() + " time: " + world.getTime() + "Search is canceled, trying to acquire new targetArea.");
        Area oldArea = targetArea;
        targetArea = decisionMaker.getNextArea();

        if (oldArea != targetArea) {
            status = searchStrategy.searchBuilding((Building) targetArea);
            if (status == CANCELED) {
                tryNewTarget(status);
            }
        } else {
            logger.debug(world.getSelf() + " time: " + world.getTime() + "Possible search strategy failed, exiting search procedure.");
        }
        return status;
    }
}
