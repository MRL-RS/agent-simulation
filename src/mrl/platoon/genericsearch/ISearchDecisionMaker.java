package mrl.platoon.genericsearch;

import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;

import java.util.List;

/**
 * @author Siavash
 *         <p/>
 *         Inteface for Search Disicion maker
 */
public interface ISearchDecisionMaker {
    /**
     * Initializes values for this class depending on implementation
     * <p/>
     * <b>Note:</b> usage of this method is optional
     */
    public void initialize();

    /**
     * Updates values for decision making evaluation
     */
    public void update();

    /**
     * Evaluates targets
     *
     * @return List of {@link rescuecore2.standard.entities.Area} to search
     */
    public List<Area> evaluateTargets();

    /**
     * returns next path for search.
     * <p/>
     * <b>Warning:</b> this method returns different values each time called. you should consider keeping the returning
     * value if you want to use it several times.
     *
     * @return {@link mrl.world.routing.path.Path} to search
     */
    public Path getNextPath();

    /**
     * returns next area to search.
     * <p/>
     * <b>Warning:</b> this method returns different values each time called. you should consider keeping the returning
     * value if you want to use it several times.
     *
     * @return
     */
    public Area getNextArea();
}
