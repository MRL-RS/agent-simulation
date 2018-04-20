package mrl.la;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 7:33:25 PM
 */
public class DefaultAction implements Action {
    private int index;
    private String actionName;

    public DefaultAction(int index, String actionName) {
        this.index = index;
        this.actionName = actionName;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }


    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public int compareTo(Action o) {
        if (o.getIndex() < index)
            return 1;
        if (o.getIndex() == index)
            return 0;
        return -1;
    }

    @Override
    public String toString() {
        return actionName;
    }
}
