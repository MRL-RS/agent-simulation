package mrl.la;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 3:30:27 PM
 */
public abstract class LA {
    State currentState;
    double learningParameter;
    double epsilon;
    Action lastAction;

    public Action action() {
        return currentState.action();
    }

    public abstract void reward(Action action);

    public abstract void penalize(Action action);

    public abstract void update(Action action, double beta);


    public State getCurrentState() {
        return currentState;
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    public double getLearningParameter() {
        return learningParameter;
    }

    public void setLearningParameter(double learningParameter) {
        this.learningParameter = learningParameter;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public Action getLastAction() {
        return lastAction;
    }

    public void setLastAction(Action lastAction) {
        this.lastAction = lastAction;
    }
}
