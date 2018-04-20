package mrl.common;

/**
 * User: mrl
 * Date: May 5, 2010
 * Time: 4:35:08 PM
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

//saeed 85-2-3
public abstract class Condition {
    public abstract boolean eval(Object obj);

    public ArrayList extract(Collection col) {
        ArrayList result = new ArrayList();
        for (Object obj : col) {
            if (eval(obj))
                result.add(obj);
        }
        return result;
    }

    public Condition and(final Condition rhs) {
        final Condition lhs = this;
        return new Condition() {
            public boolean eval(Object obj) {
                return lhs.eval(obj) && rhs.eval(obj);
            }
        };
    }

    public Condition or(final Condition rhs) {
        final Condition lhs = this;
        return new Condition() {
            public boolean eval(Object obj) {
                return lhs.eval(obj) || rhs.eval(obj);
            }
        };
    }

    public Condition not() {
        final Condition cond = this;
        return new Condition() {
            public boolean eval(Object obj) {
                return !cond.eval(obj);
            }
        };
    }

    public ArrayList extractMAR(Collection col) {
        ArrayList result = new ArrayList();
        for (Iterator it = col.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if (eval(obj)) {
                result.add(obj);
                it.remove();
            }
        }
        return result;
    }

}
