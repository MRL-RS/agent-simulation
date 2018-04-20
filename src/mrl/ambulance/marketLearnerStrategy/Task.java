package mrl.ambulance.marketLearnerStrategy;

import rescuecore2.worldmodel.EntityID;

/**
 * User: pooyad
 * Date: 5/11/11
 * Time: 3:23 PM
 */
public class Task {
    private EntityID victimID;
    private int nextTTM = -1; // time To Move To Next Target
    private int ttf = -1;// time to free
    private boolean done = false;


    public Task(EntityID victimID) {
        this.victimID = victimID;
    }

    public EntityID getVictimID() {
        return victimID;
    }

    public void setVictimID(EntityID victimID) {
        this.victimID = victimID;
    }

    public int getNextTTM() {
        return nextTTM;
    }

    public void setNextTTM(int nextTTM) {
        this.nextTTM = nextTTM;
    }

    public int getTTF() {
        return ttf;
    }

    public void setTTF(int ttf) {
        this.ttf = ttf;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
