package mrl.communication2013.message.property;

import mrl.platoon.State;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 2:22 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class AgentStateProperty extends AbstractProperty {

    public AgentStateProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(State.getStateBitNum());
    }
}
