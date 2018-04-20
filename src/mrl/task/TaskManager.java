package mrl.task;

import mrl.strategy.Strategy;

/**
 * @author Siavash
 */
public abstract class TaskManager {
    private Strategy strategy;

    public abstract Strategy selectStrategy(State state, ActionStatus actionStatus);


    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
}
