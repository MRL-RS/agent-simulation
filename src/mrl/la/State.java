package mrl.la;

import java.io.Serializable;
import java.util.Collection;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 3:55:42 PM
 */
public interface State extends Serializable {
    public Probability getProbability();

    public Collection<? extends Action> getAvailableActions();

    public String getStateName();

    public int getStateId();

    public Action action();
}
