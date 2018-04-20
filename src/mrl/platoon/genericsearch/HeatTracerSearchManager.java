package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.Set;

import static mrl.platoon.genericsearch.SearchStatus.*;

/**
 * @author Siavash
 */
public class HeatTracerSearchManager extends SearchManager {

    private static Log logger = LogFactory.getLog(HeatTracerSearchManager.class);

    public HeatTracerSearchManager(MrlWorld world, MrlPlatoonAgent agent, ISearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
    }

    private Area targetArea = null;
    private int lastUpdateTime = 0;
    private int lastExecuteTime = 0;
    private int tryCount = 0;
    private Set<EntityID> blackList;

    /**
     * @throws mrl.common.CommandException
     * @see SearchManager#execute()
     */
    @Override
    public void execute() throws CommandException {
//        world.printData("Execute heat tracer search");
        if (tryCount > 10) {
            world.printData("try count > 10");
            tryCount = 0;

            return;
        } else if (world.getTime() == lastExecuteTime) {
            tryCount++;
        }
        lastExecuteTime = world.getTime();

        if (isNeedUpdateDecisionMaker()) {
//            world.printData("update decision maker...");
            decisionMaker.update();
            lastUpdateTime = world.getTime();
        }
        if (targetArea == null) {

            targetArea = decisionMaker.getNextArea();
            if (targetArea != null) {
                world.printData("targetArea was null and now is set to: " + targetArea);
            }
        }

        if (targetArea == null) {
            return;
        }
        if (!(targetArea instanceof Building)) {
            world.printData("Incompatible type for searchBuilding");
            return;
        }

        SearchStatus status = searchStrategy.searchBuilding((Building) targetArea);


        if (status == CANCELED) {
            targetArea = null;

            execute();
        }

        if (status == FINISHED) {
            targetArea = null;
            execute();
        } else if (status == SEARCHING) {
            //Do Nothing
        }
    }

//    private SearchStatus tryNewTarget(SearchStatus status) throws CommandException {
//        logger.debug("Search is canceled, trying to acquire new targetArea.");
//        Area oldArea = targetArea;
//        targetArea = decisionMaker.getNextArea();
//
//        if (oldArea != targetArea) {
//            status = searchStrategy.manualMoveToRoad((Road) targetArea);
//            if (status == CANCELED) {
//                tryNewTarget(status);
//            }
//        } else {
//            logger.debug("Heat tracer search strategy failed, exiting search procedure.");
//        }
//        return status;
//    }


    private boolean isNeedUpdateDecisionMaker() {
        if (lastUpdateTime == world.getTime()) {
            return false;
        }

        return true;
    }
}
