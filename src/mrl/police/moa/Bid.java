package mrl.police.moa;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/27/12
 * Time: 7:25 PM
 */

import rescuecore2.worldmodel.EntityID;

/**
 * It is structure to keep bid properties
 */
public class Bid {
    private EntityID bidder;    // the Agent who sent this bid
    private Target target;      // the target which this bid is for
    private Integer value;      // this value can be cost or benefit

    public Bid(Target target, Integer value) {
        this.target = target;
        this.value = value;
    }

    public Bid(EntityID bidder, Target target, Integer value) {
        this.bidder = bidder;
        this.target = target;
        this.value = value;
    }

    public EntityID getBidder() {
        return bidder;
    }

    public void setBidder(EntityID bidder) {
        this.bidder = bidder;
    }

    public Target getTarget() {
        return target;
    }

    public Integer getValue() {
        return value;
    }
}
