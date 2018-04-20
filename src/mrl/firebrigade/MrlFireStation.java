package mrl.firebrigade;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.platoon.MrlCentre;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

import java.util.Collection;
import java.util.EnumSet;

/**
 * A sample centre agent.
 */
public class MrlFireStation extends MrlCentre {
    @Override
    public String toString() {
        return "MRL FireStation ID: " + this.getID().getValue();
    }

    @Override
    protected void postConnect() {
        super.postConnect();
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        super.think(time, changed, heard);
    }

    @Override
    public void act() throws CommandException, TimeOutException {

    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_STATION);
    }
}