package mrl.communication2013.message.property;

import mrl.communication2013.message.IDConverter;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 3/26/14
 *         Time: 11:46 PM
 */
public class AgentIdIndexProperty extends AbstractProperty {

    public AgentIdIndexProperty(int value) {
        super(value);
    }

    /**
     * define how many bits need for this property.
     * exp: setPropertyBitSize(needed_bits);
     */
    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(IDConverter.getAgentsBitSize());
    }
}
