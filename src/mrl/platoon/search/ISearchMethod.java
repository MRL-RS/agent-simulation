package mrl.platoon.search;

import mrl.common.CommandException;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.routing.path.Path;

/**
 * Created by Mostafa Shabani.
 * Date: 6/10/11
 * Time: 2:41 PM
 */
public interface ISearchMethod {
    /**
     * this method get a path and search in this path buildings.
     *
     * @param path      : target for search
     * @param inside    : agent should be go in building or not.
     * @param unvisited : random search or search in unvisited buildings.
     * @return : search status: searching - finished - canceled
     * @throws mrl.common.CommandException : send a move act to kernel
     */
    public SearchStatus searchIn(Path path, boolean inside, boolean unvisited) throws CommandException;

    /**
     * this method get a zone and search all building in this.
     *
     * @param zone      : target for search.
     * @param inside    : agent should be go in building or not.
     * @param unvisited : random search or search in unvisited buildings.
     * @return : search status: searching - finished - canceled
     * @throws mrl.common.CommandException : send a move act to kernel
     */
    public SearchStatus searchIn(MrlZone zone, boolean inside, boolean unvisited) throws CommandException;

}
