package mrl.la;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 6:41:09 PM
 */
public class LA_LRP extends LA {
    double rewardParameter;
    double penaltyParameter;

    public LA_LRP(double rewardParameter, double penaltyParameter) {
        this.rewardParameter = rewardParameter;
        this.penaltyParameter = penaltyParameter;
    }

    public void reward(Action action) {
        currentState.getProbability().updateRange(action, rewardParameter, true);
    }

    public void penalize(Action action) {
        currentState.getProbability().updateRange(action, penaltyParameter, false);
    }

    public void reward(Action action, double r) {
        currentState.getProbability().updateRange(action, r, true);
    }

    public void penalize(Action action, double p) {
        currentState.getProbability().updateRange(action, p, false);
    }

    public void update(Action action, double beta) {
    }
}
