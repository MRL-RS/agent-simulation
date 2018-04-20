package mrl.ambulance.marketLearnerStrategy;

import rescuecore2.worldmodel.EntityID;

/**
 * User: pooyad
 * Date: 4/1/11
 * Time: 1:39 PM
 */
public class VictimAllocation implements Comparable {
    private EntityID victimID;
    private int minNeededAgents;
    private int maxNeededAgents;
    private int numberOfAllocated;

    public VictimAllocation(EntityID victimID, int minNeededAgents, int maxNeededAgents) {
        this.victimID = victimID;
        this.minNeededAgents = minNeededAgents;
        this.maxNeededAgents = maxNeededAgents;
    }

    public EntityID getVictimID() {
        return victimID;
    }

    public void setVictimID(EntityID victimID) {
        this.victimID = victimID;
    }

    public int getMinNeededAgents() {
        return minNeededAgents;
    }

    public void setMinNeededAgents(int minNeededAgents) {
        this.minNeededAgents = minNeededAgents;
    }

    public int getMaxNeededAgents() {
        return maxNeededAgents;
    }

    public void setMaxNeededAgents(int maxNeededAgents) {
        this.maxNeededAgents = maxNeededAgents;
    }

    public int getNumberOfAllocated() {
        return numberOfAllocated;
    }

    public void setNumberOfAllocated(int numberOfAllocated) {
        this.numberOfAllocated = numberOfAllocated;
    }

    @Override
    public int compareTo(Object o) {
        if (minNeededAgents < ((VictimAllocation) o).getMinNeededAgents()) //decrease
            return 1;
        if (minNeededAgents == ((VictimAllocation) o).getMinNeededAgents()) {
            if (maxNeededAgents < ((VictimAllocation) o).getMaxNeededAgents()) {
                return 1;
            }
            if (maxNeededAgents == ((VictimAllocation) o).getMaxNeededAgents())
                return 0;
        }

        return -1;


    }
}
