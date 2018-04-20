package mrl.la.gridworld;

import mrl.la.DefaultAction;
import mrl.la.DefaultState;

import java.util.Arrays;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 11:33:55 PM
 */
public class GridState extends DefaultState {
    int x;
    int y;

    public GridState(int stateId, int x, int y) {
        super(Arrays.asList(
                new DefaultAction(1, "UP"),
                new DefaultAction(2, "RIGHT"),
                new DefaultAction(3, "DOWN"),
                new DefaultAction(4, "LEFT")),
                "[" + x + "," + y + "]",
                stateId);
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "GridState[" + x + "," + y + "]";
    }
}
