package mrl.ambulance.marketLearnerStrategy;

import mrl.ambulance.MrlAmbulanceTeamWorld;

/**
 * User: pooya deldar gohardani
 * Date: 3/23/11
 * Time: 10:46 PM
 */
public class AmbulanceTeamAuction {

    private MrlAmbulanceTeamWorld world;
    private int auctionPeriod = 10;

    public AmbulanceTeamAuction(MrlAmbulanceTeamWorld world) {
        this.world = world;
    }

    public boolean isAuctionTime() {
        return world.getTime() % auctionPeriod == 0;
    }

    public void startAuction() {

    }


}
