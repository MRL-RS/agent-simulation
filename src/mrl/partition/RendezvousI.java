package mrl.partition;

import rescuecore2.standard.entities.Road;

import java.util.ArrayList;

/**
 * Created by P.D.G.
 * User: root
 * Date: Jan 28, 2010
 * Time: 12:47:50 PM
 */
public interface RendezvousI {

    void setX(int x);

    void setY(int y);

    int getX();

    int getY();

    int getPriority();

    void setPriority(int priority);

    // Has the leader ever gone to his partition rendezvous

    boolean getHasGoneTo();

    void setHasGoneTo(boolean hasGoneTo);

    ArrayList<Integer> getPartitionsId();

    ArrayList<Road> getRodes();

    @Override
    boolean equals(Object o);
}
