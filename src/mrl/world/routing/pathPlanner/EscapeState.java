package mrl.world.routing.pathPlanner;

/**
 * Created By Mahdi Taherian
 * Date: 5/27/12
 * Time: 12:48 PM
 */
public enum EscapeState {
    FAILED,
    UNREACHABLE,
    STUCK,
    BURIED,
    DEFAULT,
    ESCAPE_BY_COORDINATION,
    TRY_NEW_A_STAR,
    ROLL_BACK,
    HARD_WALKING,
}
