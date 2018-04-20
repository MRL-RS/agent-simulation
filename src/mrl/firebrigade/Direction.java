package mrl.firebrigade;

/**
 * User: roohola
 * Date: 6/5/11
 * Time: 11:43 PM
 */
public enum Direction {
    UP(0),
    RIGHT(1),
    DOWN(2),
    LEFT(3);

    private final int dir; // in meters

    Direction(int dir) {
        this.dir = dir;
    }

    public int dir() {
        return dir;
    }
}
