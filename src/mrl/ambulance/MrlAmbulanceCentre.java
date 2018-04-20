package mrl.ambulance;

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
public class MrlAmbulanceCentre extends MrlCentre {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlAmbulanceCentre.class);

    @Override
    public String toString() {
        return "MRL AmbulanceCenter ID: " + this.getID().getValue();
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        for (Command next : heard) {
            Logger.debug("Heard " + next);
        }
        sendRest(time);
    }

    @Override
    public void act() throws CommandException, TimeOutException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.AMBULANCE_CENTRE);
    }
}