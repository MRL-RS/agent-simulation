package mrl.police.moa;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/27/12
 * Time: 7:09 PM
 */

/**
 * An Enum to handle the importance values. Note that lower value has more importance.
 */
public enum Importance {
    TARGET_TO_GO_FIRE_BRIGADE(300),
    BLOCKED_FIRE_BRIGADE(250),
    REFUGE_ENTRANCE(200),
    PARTITION_CENTER(195),
    NEIGHBOUR_PARTITION_CENTER(193),

    FIERY_BUILDING_1(190),
    BLOCKED_AMBULANCE_TEAM(187),
    BUILDING_WITH_HEALTHY_HUMAN(186),
    FIERY_BUILDING_2(185),
    FIERY_BUILDING_3(180),
    BURIED_FIRE_BRIGADE(177),
    BURIED_POLICE_FORCE(175),
    BURIED_AMBULANCE_TEAM(140),
    RENDEZVOUS_POINT(135),
    //    BLOCKED_CIVILIAN(100),
    //    BURIED_CIVILIAN(90),
    TARGET_TO_GO_AMBULANCE_TEAM(120),
    BUILDING_WITH_DAMAGED_CIVILIAN(110), //Building with civilians

    DEFAULT(1),;


    private int importance;

    private Importance(int importance) {
        this.importance = importance;
    }

    public int getImportance() {
        return importance;
    }
}
