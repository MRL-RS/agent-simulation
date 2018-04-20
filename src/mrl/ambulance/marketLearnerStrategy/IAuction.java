package mrl.ambulance.marketLearnerStrategy;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.Set;

/**
 * User: pooyad
 * Date: 5/3/11
 * Time: 4:06 PM
 */
public interface IAuction {

    void startAuction(Set<StandardEntity> goodHumans);

    int computeCost(Human myCurrentTarget, EntityID victimID);

    void bidding(Human myTarget);

    boolean isBidTime();

    boolean isRecieveBidsTime();

    void taskAllocation();

    EntityID getMyTask();

    boolean haveIBiggestIDInAssignedTask();
}
