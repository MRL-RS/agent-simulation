package mrl.firebrigade;

import java.util.Comparator;
import java.util.Map;

/**
 * User: vahid
 * Date: 5/24/11
 * Time: 12:34 PM
 */
public class ValueComparator implements Comparator {
    Map base;

    public ValueComparator(Map base) {
        this.base = base;
    }

    @Override
    public int compare(Object o1, Object o2) {
        if ((Integer) base.get(o1) < (Integer) base.get(o2)) {
            return 1;
        } else if ((Integer) base.get(o1) == (Integer) base.get(o2)) {
            return 0;
        } else {
            return -1;
        }
    }
}
