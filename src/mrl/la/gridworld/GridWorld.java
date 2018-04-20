package mrl.la.gridworld;

import mrl.common.Util;
import mrl.la.Action;
import mrl.la.LA_LRP;
import mrl.la.State;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 11:07:24 PM
 */
public class GridWorld {
    GridState[][] states;
    LA_LRP la;
    GridState destination;
    int width;
    int height;
    HashSet<State> visitedStates = new HashSet<State>();

    public GridWorld(int width, int height, double rewardParameter, double penaltyParameter) {
        this.width = width;
        this.height = height;


        states = new GridState[width][height];
        initializeStates(width, height);
        la = new LA_LRP(rewardParameter, penaltyParameter);

    }

    public List<GridState> learning(int curX, int curY, int desX, int desY, int epoch) {
        List<GridState> gridStates = new ArrayList<GridState>();
        la.setCurrentState(states[curX][curY]);
        gridStates.add(states[curX][curY]);
        destination = states[desX][desY];
        visitedStates.add(la.getCurrentState());
        while (!la.getCurrentState().equals(destination)) {
            Action action = null;
            int d_old = Util.distance(((GridState) la.getCurrentState()).getX(), ((GridState) la.getCurrentState()).getY(), destination.getX(), destination.getY());
            for (int i = 0; i < epoch; i++) {
                action = la.action();
                int[] xy = getNextXY(action);

                if (xy[0] < 0 || xy[1] < 0 || xy[0] >= width || xy[1] >= height) {
                    la.penalize(action, 1);    // out of bound so a penalty
                    i--;
                } else {
                    if (visitedStates.contains(states[xy[0]][xy[1]])) {
                        la.penalize(action, 1);// loop so penalty
                    } else {
                        if (states[xy[0]][xy[1]].equals(destination)) {
                            la.reward(action, 10);
                        } else {
                            int d_new = Util.distance(xy[0], xy[1], destination.getX(), destination.getY());
                            if (d_new < d_old) {
                                la.reward(action); // got nearer so a reward
                            } else if (d_new == d_old) {
                                if (action.equals(la.getLastAction())) {
                                    la.penalize(action);// repetitive action so a penalty
                                } else {
                                    la.penalize(action); // no change on distance can be a reward
                                }
                            } else {
                                la.penalize(action);// got further so a penalty
                            }
                        }
                    }


                }


            }
            State state = la.getCurrentState();
            goToNextState(action);
            gridStates.add((GridState) la.getCurrentState());
            System.out.println("Action Taken:" + action + " -- " + state + " -> " + la.getCurrentState());
            visitedStates.add(la.getCurrentState());
        }
        return gridStates;

    }

    private void initializeStates(int width, int height) {
        int count = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                states[i][j] = new GridState(count++, i, j);
            }
        }

    }

    private int[] getNextXY(Action action) {
        int x, y;
        switch (action.getIndex()) {
            case 1:// up
                x = ((GridState) la.getCurrentState()).getX();
                y = ((GridState) la.getCurrentState()).getY() + 1;

                break;
            case 2:// right
                x = ((GridState) la.getCurrentState()).getX() + 1;
                y = ((GridState) la.getCurrentState()).getY();

                break;
            case 3:// down
                x = ((GridState) la.getCurrentState()).getX();
                y = ((GridState) la.getCurrentState()).getY() - 1;

                break;
            case 4:// left
                x = ((GridState) la.getCurrentState()).getX() - 1;
                y = ((GridState) la.getCurrentState()).getY();

                break;
            default:
                throw new RuntimeException("WTF");
        }
        return new int[]{x, y};
    }

    private void goToNextState(Action action) {
        int[] xy = getNextXY(action);
        la.setCurrentState(states[xy[0]][xy[1]]);
    }

    public static void main(String[] args) {
        GridWorld grid = new GridWorld(5, 5, 0.2, 0.25);
        grid.learning(0, 0, 3, 2, 1000);
    }
}
