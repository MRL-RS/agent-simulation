package mrl.platoon;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 27, 2010
 * Time: 11:57:21 AM
 */
public enum State {

    WORKING(0),
    RESTING(1),
    BURIED(2),
    DEAD(3),
    STUCK(4),
    THINKING(5),
    CRASH(6),
    SEARCHING(7);

    public int index;

    private State(int index) {
        this.index = index;
    }

    public static State getState(int typeIndex) {
        State state;

        if (typeIndex < values().length) {
            state = values()[typeIndex];
        } else {
            state = null;
        }
        return state;
    }

    public static State indexToEnum(int index) {
        for (State state : State.values()) {
            if (state.ordinal() == index)
                return state;
        }
        return null;
    }

    public static int getStateBitNum() {
        int res = 1;
        int size = State.values().length - 1;
        while ((size >>= 1) != 0)
            res++;
        return res;
    }
}
