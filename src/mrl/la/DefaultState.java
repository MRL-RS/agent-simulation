package mrl.la;

import java.util.Collection;
import java.util.List;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 10:54:45 PM
 */
public class DefaultState implements State {
    public static final long serialVersionUID = -11123445454700L;
    protected Probability probability;
    protected Collection<? extends Action> availableActions;
    protected String stateName;
    protected int stateId;

    public DefaultState(Collection<? extends Action> availableActions, String stateName, int stateId) {
        this.availableActions = availableActions;
        this.stateName = stateName;
        this.stateId = stateId;
        this.probability = new Probability(stateId + System.currentTimeMillis());
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

    public void setProbability(Probability probability) {
        this.probability = probability;
    }

    public Collection<? extends Action> getAvailableActions() {
        return availableActions;
    }

    public void setAvailableActions(Collection<? extends Action> availableActions) {
        this.availableActions = availableActions;
    }

    public void setAvailableActions(List<Action> availableActions) {
        this.availableActions = availableActions;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public int getStateId() {
        return stateId;
    }

    public void setStateId(int stateId) {
        this.stateId = stateId;
    }

    @Override
    public Action action() {
        return probability.chooseAction();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultState) {
            return this.stateId == ((DefaultState) o).stateId;
        }
        return false;
    }
}
