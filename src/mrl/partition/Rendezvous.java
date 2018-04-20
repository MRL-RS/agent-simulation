package mrl.partition;


import rescuecore2.standard.entities.Road;

import java.util.Comparator;
import java.util.List;

/**
 * Created by P.D.G.
 * User: pooyaD
 * Date: Jan 28, 2010
 * Time: 1:18:00 PM
 */
public class Rendezvous {
    int x;
    int y;
    int priority = 0;
    boolean hasGoneto = false;

    List<Road> roadList; // as nodeList in previouse version
    List<Integer> partitions;

    public Rendezvous(List<Road> roadList, List<Integer> partitions) {
        this.roadList = roadList;
        this.partitions = partitions;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rendezvous))
            return false;
        Rendezvous rendezvous = (Rendezvous) o;
        return rendezvous.x == x && rendezvous.y == y && rendezvous.roadList == roadList;
    }

    public static Comparator RendezvousPriorityComparator = new Comparator() {
        public int compare(Object o1, Object o2) {

            Rendezvous r1 = (Rendezvous) o1;
            Rendezvous r2 = (Rendezvous) o2;

            if (r1.priority > r2.priority)
                return 1;
            if (r1.priority == r2.priority)
                return 0;

            return -1;

        }
    };

    public List<Road> getRoadList() {
        return roadList;
    }

    public List<Integer> getPartitions() {
        return partitions;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setHasGoneto(boolean hasGoneto) {
        this.hasGoneto = hasGoneto;
    }
}
