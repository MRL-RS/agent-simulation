package mrl.la;

import java.io.Serializable;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 3:55:32 PM
 */
public interface Action extends Comparable<Action>, Serializable {
    public int getIndex();

    public String getActionName();
}
