package mrl.platoon.search.la;

import mrl.la.Action;
import mrl.la.Probability;
import mrl.la.State;
import mrl.la.ValueRange;
import mrl.world.object.mrlZoneEntity.MrlZone;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: roohola
 * Date: 5/10/11
 * Time: 2:51 PM
 */
public class ZoneState implements State {
    protected Probability probability;
    protected Collection<ZoneAction> availableActions = new ArrayList<ZoneAction>();
    protected MrlZone zone;

    public ZoneState(Collection<MrlZone> zoneCollection, MrlZone zone) {
        for (MrlZone zoneEntity : zoneCollection) {
            availableActions.add(new ZoneAction(zoneEntity));
        }

        this.zone = zone;

        this.probability = new Probability(zone.getId() + System.currentTimeMillis());
        int index = 0;
        double width = 1.0 / (double) availableActions.size();
        for (Action action : availableActions) {
            index++;
            probability.put(action, new ValueRange(index, (index - 1.0) * width, ((double) index) * width));
        }

    }

    public Probability getProbability() {
        return probability;
    }

    public Collection<? extends Action> getAvailableActions() {
        return availableActions;
    }

    @Override
    public String getStateName() {
        return "ZoneState[" + getStateId() + "]";
    }

    public int getStateId() {
        return zone.getId();
    }


    @Override
    public Action action() {
        return probability.chooseAction();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ZoneState && getStateId() == ((ZoneState) o).getStateId();
    }
}
