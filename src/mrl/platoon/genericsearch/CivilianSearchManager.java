package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;

import static mrl.platoon.genericsearch.SearchStatus.*;

/**
 * @author Mahdi
 */
public class CivilianSearchManager extends SearchManager {
    private static Log logger = LogFactory.getLog(PossibleBuildingSearchManager.class);
    private Area targetArea;
    private CivilianSearchDecisionMaker decisionMaker;
    private int lastUpdateTime;
    boolean greedyStrategy;
    private int thisCycleExecute;
    private int lastExecuteTime;
    private int stuckTime;
    private boolean allowMove;
    private boolean needChange;


    public CivilianSearchManager(MrlWorld world, MrlPlatoonAgent agent, CivilianSearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
        this.decisionMaker = decisionMaker;
        targetArea = null;
        greedyStrategy = false;
        lastUpdateTime = 0;
        lastExecuteTime = 0;
        thisCycleExecute = 0;
        stuckTime = 0;
        allowMove = true;
        needChange = false;
    }

    @Override
    public void execute() throws CommandException {

        logger.debug("Execute.");

        //this condition useful for prevent in cycle loop....
        if (world.getTime() == lastExecuteTime) {
            thisCycleExecute++;
        } else {
            lastExecuteTime = world.getTime();
            thisCycleExecute = 0;
        }
        if (thisCycleExecute > 20) {

            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("This cycle had too much execute... search failed!");
            }
            return;
        }

        if (isNeedToChangeTarget()) {
            if (isNeedUpdateDecisionMaker()) {
                lastUpdateTime = world.getTime();
                decisionMaker.update();
            }
            needChange = false;
            targetArea = decisionMaker.getNextArea();
            if (targetArea == null) {
                if (MRLConstants.DEBUG_SEARCH) {
                    world.printData("targetArea is set to: " + null);
                }
                return;
            }
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("targetArea is set to: " + targetArea);
            }
        } else if (!greedyStrategy /*&& !decisionMaker.searchInPartition*/) {
            if (isNeedUpdateDecisionMaker()) {
                lastUpdateTime = world.getTime();
                decisionMaker.update();
            }
            Area betterTarget = decisionMaker.getBetterTarget(targetArea);
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("betterTarget is: " + targetArea);
            }
            if (betterTarget != null) {
                targetArea = betterTarget;
            }
        }

        if (targetArea != null) {
            decisionMaker.setBuildingInProgress(targetArea.getID());
        } else {
            decisionMaker.setBuildingInProgress(null);
        }
        searchStrategy.setSearchUnvisited(true);
        if (allowMove) {
            SearchStatus status = searchStrategy.searchBuilding((Building) targetArea);

            if (status == CANCELED) {
                if (targetArea == null) {
                    decisionMaker.result(CivilianSearchDecisionMaker.SearchResult.NO_VALID_TARGET);
                } else {
                    decisionMaker.result(CivilianSearchDecisionMaker.SearchResult.UNREACHABLE);
                }
                needChange = true;
                execute();
            }
            if (status == FINISHED) {
                targetArea = null;
                decisionMaker.result(CivilianSearchDecisionMaker.SearchResult.SUCCESSFUL);
                execute();
            } else if (status == SEARCHING) {
                //Do Nothing
            }
        }
    }

    @Override
    public void allowMove(boolean allowMove) {
        this.allowMove = allowMove;
    }

    @Override
    public Area getTargetArea() {
        return targetArea;
    }

    /**
     * this method check need to change target or not
     *
     * @return return true if should change target.
     */
    private boolean isNeedToChangeTarget() {
        boolean need = false;
        if (targetArea == null || world.getMrlBuilding(targetArea.getID()).isVisited() || needChange) {
            need = true;
        }
        return need;
    }

    /**
     * check need for update decision maker or not!
     *
     * @return return true if need to update
     */
    private boolean isNeedUpdateDecisionMaker() {
        boolean need = false;
        if (lastUpdateTime < world.getTime() || needChange) {
            need = true;
        }
        return need;
    }
}
