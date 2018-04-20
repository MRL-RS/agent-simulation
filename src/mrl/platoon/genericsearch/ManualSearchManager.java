package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Set;

import static mrl.platoon.genericsearch.SearchStatus.FINISHED;
import static mrl.platoon.genericsearch.SearchStatus.SEARCHING;

/**
 * Manual search class
 * <p/>
 * <b>Note:</b> set targets using {@code setTarget} method
 *
 * @author Siavash
 */
public class ManualSearchManager extends SearchManager {

    private static Log logger = LogFactory.getLog(ManualSearchManager.class);
    private boolean searchInside;
    private boolean searchUnvisited;
    ManualSearchDecisionMaker decisionMaker;

    public ManualSearchManager(MrlWorld world, MrlPlatoonAgent agent, ManualSearchDecisionMaker decisionMaker, ISearchStrategy searchStrategy) {
        super(world, agent, decisionMaker, searchStrategy);
        searchUnvisited = true;
        targetArea = null;
        this.decisionMaker = decisionMaker;
    }

    Area targetArea;

    @Override
    public void execute() throws CommandException {
        if (targetArea == null) {
            //unnecessary assignment just to be more readable.
            if (searchUnvisited) {
                targets = decisionMaker.filterTargets(targets);
            }
            if (!targets.isEmpty()) {
                targetArea = targets.get(0);
                targets.remove(0);
                logger.debug("targetArea was null and now is set to: " + targetArea);
            } else {
                logger.debug("Targets is empty.");
            }
        }

        SearchStatus status = searchStrategy.manualMoveToSearchingBuilding((Building) targetArea, false, searchUnvisited);
        logger.debug("Search Status: " + status);

        if (status == FINISHED) {
            targetArea = null;
            execute();
        } else if (status == SEARCHING) {
            //Do Nothing
        }
    }

    /**
     * set targets
     * <b>Warning:</b> Set only a list of {@code Building} as target parameters.
     *
     * @param targets         List<Building> of targets (Buildings only)
     * @param searchUnvisited option to search unvisited buildings
     */
    public void setTargets(List<Area> targets, boolean searchUnvisited) {
        this.targets = targets;
        this.searchUnvisited = searchUnvisited;
    }

    public void setTargets(Set<EntityID> targets) {
        this.targets.clear();
        for (EntityID entityID : targets) {
            this.targets.add((Area) world.getEntity(entityID));
        }
    }

    public List<Area> getTargets() {
        return targets;
    }
}
