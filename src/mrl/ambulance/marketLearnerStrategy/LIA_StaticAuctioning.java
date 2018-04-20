package mrl.ambulance.marketLearnerStrategy;

import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.communication2013.helper.AmbulanceMessageHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: pooyad
 * Date: 5/4/11
 * Time: 5:45 PM
 */
public class LIA_StaticAuctioning extends LeaderInitiatorAuction {
    public LIA_StaticAuctioning(MrlAmbulanceTeamWorld world, AmbulanceMessageHelper ambulanceMessageHelper, AmbulanceUtilities ambulanceUtilities) {
        super(world, ambulanceMessageHelper, ambulanceUtilities);
    }


    @Override
    public void startAuction(Set<StandardEntity> goodHumans) {
        ambulanceUtilities.chooseLeader();

        if (!amILeader()) {
            return;
        }
        if (!isAuctionTime()) {
            return;
        }

        //get m most valuable victims to bid
        List<VictimImportance> victimsToSell = getVictimsToSell(goodHumans);

        List<Pair<EntityID, Integer>> vics = new ArrayList<Pair<EntityID, Integer>>();
        for (VictimImportance vic : victimsToSell) {
            vics.add(new Pair<EntityID, Integer>(vic.getVictim().getID(), vic.getImportance()));
        }
        System.out.println("LLLLLLLLLLLLLGGGGGGG " + world.getTime() + " " + world.getSelf().getID() + " I am LEADER Goods To bid: " + vics);
//        ambulanceMessageHelper.sendVictimsToSell(victimsToSell);

    }

    /**
     * Is it time to send bids? Often after one cycle of starting an auction, it is time to send bids.
     *
     * @return returns true if it is time to send bids otherwise returns false.
     */
    @Override
    public boolean isBidTime() {
        if ((world.getTime() - 1) % auctionPeriod == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Estimate cost of each leader bids and send it as a bid
     *
     * @param myCurrentTarget my currently rescuing target
     */
    @Override
    public void bidding(Human myCurrentTarget) {
        if (!isBidTime()) {
            return;
        }

        List<AmbulanceTeamBid> bids = new ArrayList<AmbulanceTeamBid>();

        Human human;
        for (EntityID victimID : world.getLeaderBids()) {

            human = (Human) world.getEntity(victimID);
            if (human == null || !human.isPositionDefined()) {
                continue; //TODO : what should we do here? now we don't bid for it
            }
            AmbulanceTeamBid bid = new AmbulanceTeamBid();
            bid.setBidderID(world.getSelf().getID());
            bid.setHumanID(victimID);
            bid.setBidValue(computeCost(myCurrentTarget, victimID));
            bids.add(bid);

        }

        world.getLeaderBids().clear();

        List<Pair<EntityID, Integer>> vics = new ArrayList<Pair<EntityID, Integer>>();
        for (AmbulanceTeamBid ambulanceTeamBid : bids) {
            vics.add(new Pair<EntityID, Integer>(ambulanceTeamBid.getHumanID(), ambulanceTeamBid.getBidValue()));
        }
        System.out.println("BBBBBBBBBBBB " + world.getTime() + " " + world.getSelf().getID() + " " + vics);

//        ambulanceMessageHelper.sendBidMessage(bids);


    }

    @Override
    public boolean isRecieveBidsTime() {
        System.err.println("Currect Function call");

        if ((world.getTime() - 2) % auctionPeriod == 0 || (world.getTime() - 3) % auctionPeriod == 0) {
            return true;
        } else {
            return false;
        }

    }


}
