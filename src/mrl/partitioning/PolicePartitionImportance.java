package mrl.partitioning;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/5/12
 * Time: 11:41 AM
 */

/**
 * An Enum to define weight of partition parameters
 */
public enum PolicePartitionImportance {


    //    BUILDING(1),
//    UNVISITED_BUILDING(2),
//    BURNING_BUILDING(3),
    REFUGE_BUILDING(3),
    //    RENDEZVOUS_POINT(3),
    BURIED_FB_AGENT(1),
    BURIED_AT_AGENT(1),
    BURIED_PF_AGENT(2),
    BLOCKED_AT_AGENT(3),
    BLOCKED_FB_AGENT(5),;


    private int importance;

    private PolicePartitionImportance(int importance) {
        this.importance = importance;
    }

    public int getImportance() {
        return importance;
    }

}
