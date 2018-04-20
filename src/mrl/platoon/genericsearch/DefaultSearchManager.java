package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static mrl.platoon.genericsearch.SearchStatus.*;

/**
 * @author Siavash
 * @see SearchManager
 */
public class DefaultSearchManager extends SearchManager {

    private static Log logger = LogFactory.getLog(DefaultSearchManager.class);

    public DefaultSearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
        targetPath = null;
    }

    private Path targetPath;

    /**
     * @throws mrl.common.CommandException
     * @see SearchManager#execute()
     */
    @Override
    public void execute() throws CommandException {
        logger.debug("Execute.");
        if (targetPath == null) {
            targetPath = decisionMaker.getNextPath();
            searchStrategy.setSearchingPath(targetPath, true);
//            logger.debug("targetPath was null and now is set to: " + targetPath.toString());
        }

        SearchStatus status = searchStrategy.searchPath();
        logger.debug("Search Status: " + status);
        if (status == FINISHED) {
            targetPath = null;
            tryNewTarget(status);
        } else if (status == SEARCHING) {
            //Do nothing
        }
    }

    private SearchStatus tryNewTarget(SearchStatus status) throws CommandException {
        logger.debug("Search is canceled, trying to acquire new targetArea.");
        Path oldPath = targetPath;
        targetPath = decisionMaker.getNextPath();

        if (oldPath != targetPath && targetPath != null) {
            searchStrategy.setSearchingPath(targetPath, true);
            status = searchStrategy.searchPath();
            if (status == CANCELED) {
                tryNewTarget(status);
            }
        } else {
            logger.debug("Default search strategy failed, exiting search procedure.");
        }
        return status;
    }

}
