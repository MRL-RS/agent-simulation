package mrl.helper.info;

import rescuecore2.misc.Pair;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 6:36:25 PM
 */
public class EdgeInfo {

    public EdgeInfo() {
    }

    protected Integer length;
    protected Pair<Integer, Integer> middle;
    protected Boolean onEntrance = false;

    public void setLength(Integer length) {
        this.length = length;
    }

    public void setMiddle(Pair<Integer, Integer> middle) {
        this.middle = middle;
    }

    public void setOnEntrance(Boolean onEntrance) {
        this.onEntrance = onEntrance;
    }

    public Integer getLength() {
        return length;
    }

    public Pair<Integer, Integer> getMiddle() {
        return middle;
    }

    public Boolean isOnEntrance() {
        return onEntrance;
    }
}
