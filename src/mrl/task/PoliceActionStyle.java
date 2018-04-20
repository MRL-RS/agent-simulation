package mrl.task;

/**
 * @author Siavash
 */
public enum PoliceActionStyle {
    NO_ACTION,
    FULL_PATH_CLEAR,
    JUST_TARGET_CLEAR,
    CLEAR_NORMAL,
    CLEAR_IMPORTANT,
    CLEAR_Way,

    CLEAR_TARGET,               // for opening movement path
    CLEAR_ENTRANCE_AND_AROUND,  // for Refuges
    CLEAR_ENTRANCE,             // for Buildings with humans
    CLEAR_HUMAN,                // for humans on the road
    CLEAR_AROUND,   // for burning buildings
    CLEAR_ALL,                   // for Roads specially rendezvous roads

    CLEAR_DIRECTION_HEAD,
    CLEAR_DIRECTION_TAIL,
    CLEAR_DIRECTION_NORMAL, // don't care the direction, just clear

    CLEAR_ROUTE, // clearing a route from start to end


    DEFAULT
}
