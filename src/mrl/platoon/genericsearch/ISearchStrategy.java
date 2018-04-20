package mrl.platoon.genericsearch;

import mrl.common.CommandException;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;

/**
 * Interface for Search Method
 *
 * @author Siavash
 * @see SearchStrategy
 */
public interface ISearchStrategy {

    /**
     * Plan move to {@code targetArea}.
     *
     * @param targetArea target area
     * @return search result
     * @throws mrl.common.CommandException after sending a command to kernel
     */
    public SearchStatus manualMoveToArea(Area targetArea) throws CommandException;

    /**
     * Plan move to {@code targetRoad}.
     *
     * @param targetRoad target road
     * @return search result
     * @throws mrl.common.CommandException after sending a command to kernel
     */
    public SearchStatus manualMoveToRoad(Road targetRoad) throws CommandException;

    /**
     * Plan move to {@code moveToSearchBuilding} considering option parameters
     *
     * @param targetBuildingToSearch   target building
     * @param searchUnvisitedBuildings search unvisited building only
     * @param searchInside             search inside building
     * @return //TODO @Siavash fix this.
     * @throws mrl.common.CommandException after sending a command to kernel
     */
    public SearchStatus manualMoveToSearchingBuilding(Building targetBuildingToSearch, boolean searchInside, boolean searchUnvisitedBuildings) throws CommandException;

    /**
     * search map with options to search within a partition or all over the map and search only unvisited buildings or all buildings.
     * <b>Warning:</b> this only checks unvisited buildings when {@code searchUnvisited} is true
     *
     * @param inPartition     search only in current partition if true
     * @param searchUnvisited search only unvisited buildings if true
     * @return 1 if search in progress, 0 if search done
     * @throws mrl.common.CommandException after sending a command to kernel
     */
    public SearchStatus search(boolean inPartition, boolean searchUnvisited) throws CommandException;

    /**
     * search {@code path} considering option whether to search unvisited buildings or not
     * <p/>
     * <b>Warning:</b> be aware that you have to call {@code setSearchingPath} first
     * <b>Warning:</b> this only checks unvisited buildings when {@code searchUnvisited} is true
     *
     * @return 1 if search in progress, 0 if search done
     * @throws mrl.common.CommandException after sending a command to kernel
     */
    public SearchStatus searchPath() throws CommandException;

    /**
     * search inside a specific building
     * <p/>
     * <b>Warning:</b> be aware that you may need to call {@code setSearchingBuilding} first
     *
     * @param building //TODO @Siavash Fix this.
     * @return 1 if search in progress, 0 if search done
     * @throws mrl.common.CommandException after sending a command to kernel
     */
    public SearchStatus searchBuilding(Building building) throws CommandException;

    /**
     * performs a basic default search
     *
     * @return 1 if search in progress, 0 if search done
     * @throws mrl.common.CommandException after sending a command to kernel
     */
    public SearchStatus search() throws CommandException;

    /**
     * Sets {@code searchingPath}
     *
     * @param searchingPath   path to search in
     * @param searchUnvisited option to search only unvisited buildings
     */
    public void setSearchingPath(Path searchingPath, boolean searchUnvisited);

    /**
     * Sets {@code searchingBuilding}
     *
     * @param searchingBuilding building to search in
     */
    public void setSearchingBuilding(Building searchingBuilding);

    /**
     * Mahdi:
     * search unvisited buildings only.<br/>
     * if want to search buildings that visited before , {@code SearchStatus} will be <I>FINISHED</I>
     *
     * @param searchUnvisited true means buildings which agent wants to search should not be visited, otherwise search Finished!
     */
    public void setSearchUnvisited(boolean searchUnvisited);

}
