package mrl.ambulance.marketLearnerStrategy;

import rescuecore2.standard.entities.StandardEntity;

/**
 * User: pooyad
 * Date: 5/3/11
 * Time: 5:16 PM
 */
public class VictimImportance implements Comparable {

    StandardEntity victim;
    int stateType;// it is a value which is used for Learning Automata
    Integer Importance;

    public VictimImportance(StandardEntity victim, Integer importance) {
        this.victim = victim;
        Importance = importance;
    }

    public StandardEntity getVictim() {
        return victim;
    }

    public void setVictim(StandardEntity victim) {
        this.victim = victim;
    }

    public Integer getImportance() {
        return Importance;
    }

    public void setImportance(Integer importance) {
        Importance = importance;
    }


    public int getStateType() {
        return stateType;
    }

    public void setStateType(int stateType) {
        this.stateType = stateType;
    }

    @Override
    public int compareTo(Object o) {

        VictimImportance victimImportance = (VictimImportance) o;

        if (victimImportance.getImportance() < this.Importance) { //increase
            return 1;
        } else {
            return 0;
        }


    }
}
