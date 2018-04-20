package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Building;

import java.util.ArrayList;
import java.util.List;

import static mrl.platoon.genericsearch.SearchStatus.*;

/**
 * @author Mahdi
 */
public class SimpleSearchManager extends SearchManager {
    private static Log logger = LogFactory.getLog(PossibleBuildingSearchManager.class);
    private int lastUpdateTime;
    private boolean shouldChangePath;
    List<Path> unreachablePaths;
    List<Building> unreachableBuildings;
    private Path targetPath;
    private int thisCycleExecute;
    private int lastExecuteTime;
//    private List<Path> notSearchedPaths;

    public SimpleSearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
        this.decisionMaker = decisionMaker;
        targetPath = null;
        shouldChangePath = false;
        lastUpdateTime = 0;
        lastExecuteTime = 0;
        thisCycleExecute = 0;
        unreachablePaths = new ArrayList<Path>();
        unreachableBuildings = new ArrayList<Building>();
//        notSearchedPaths = new ArrayList<Path>();
        decisionMaker.initialize();
    }

    @Override
    public void execute() throws CommandException {
        if (world.getTime() == lastExecuteTime) {
            thisCycleExecute++;
        } else {
            lastExecuteTime = world.getTime();
            thisCycleExecute = 0;
        }

        if (thisCycleExecute > 10) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("This cycle had too much execute... search failed!");
            }
            shouldChangePath = true;
            return;
        }
        logger.debug("Execute.");

        if (isNeedToEvaluateTarget()) {
            if (isNeedUpdateDecisionMaker()) {
                lastUpdateTime = world.getTime();
                decisionMaker.update();
            }
            targetPath = decisionMaker.getNextPath();
            shouldChangePath = false;
            logger.debug("targetPath was null and now is set to: " + targetPath);
        }
        searchStrategy.setSearchingPath(targetPath, true);
        SearchStatus status = searchStrategy.searchPath();

        if (status.equals(CANCELED)) {
            shouldChangePath = true;
            targetPath = null;
            execute();
        }
        if (status == FINISHED) {
            targetPath = null;
            shouldChangePath = true;
            execute();
        } else if (status == SEARCHING) {
            //Do Nothing
        }
    }

    private boolean isNeedToEvaluateTarget() {
        boolean need = false;
        if (targetPath == null || shouldChangePath) {
            need = true;
        }
        if (isPartitionChanged()) {
            need = true;
        }
        return need;
    }

    private boolean isNeedUpdateDecisionMaker() {
        boolean need = false;
        if (lastUpdateTime < world.getTime() || shouldChangePath) {
            need = true;
        }
        return need;
    }
}
