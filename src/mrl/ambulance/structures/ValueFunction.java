package mrl.ambulance.structures;


/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 23, 2010
 * Time: 6:39:42 PM
 */
public class ValueFunction {
    private int ambulanceTeamID;
    private int time;
    private double valueFunction;

    public int getAmbulanceTeamID() {
        return ambulanceTeamID;
    }

    public void setAmbulanceTeamID(int ambulanceTeamID) {
        this.ambulanceTeamID = ambulanceTeamID;
    }


    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public double getValueFunction() {
        return valueFunction;
    }

    public void setValueFunction(double valueFunction) {
        this.valueFunction = valueFunction;
    }
}