package mrl.ambulance.marketLearnerStrategy;

/**
 * User: pooyad
 * Date: Mar 4, 2011
 * Time: 11:53:04 AM
 */
public class LearnerVariables {

    private double[][] qTable = new double[5][5];
    private int state = 0;
    private int time = 0;
    private int action = 0;
    private double reward = 0;
    private double valueFunction = 0;


    public double[][] getQTable() {
        return qTable;
    }

    public void setQTable(double[][] qTable) {
        this.qTable = qTable;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public double getValueFunction() {
        return valueFunction;
    }

    public void setValueFunction(double valueFunction) {
        this.valueFunction = valueFunction;
    }
}
