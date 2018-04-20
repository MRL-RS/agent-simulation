package mrl.communication2013.message.type;

import mrl.communication2013.entities.BuriedAgentSay;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;

/**
 * @author Pooya Deldar Gohardani
 */
public class BuriedAgentSayMessage extends AbstractMessage<BuriedAgentSay> {
    int buildingIndex;
    int hp;
    int buriedness;
    int damage;
    int buriedAgentIDIndex;

    public BuriedAgentSayMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(5);
    }

    public BuriedAgentSayMessage(BuriedAgentSay buriedAgent) {
        super(buriedAgent);
        setDefaultSayTTL(5);
        setSayTTL();
    }

    public BuriedAgentSayMessage() {
        super();
        setDefaultSayTTL(5);
        setSayTTL();
        createProperties();
    }


    @Override
    public BuriedAgentSay read(int sendTime) {
        BuriedAgentSay buriedAgent = new BuriedAgentSay();
        buriedAgent.setBuildingID(IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex)));
        buriedAgent.setHp(propertyValues.get(PropertyTypes.HealthPoint));
        buriedAgent.setDamage(propertyValues.get(PropertyTypes.Damage));
        buriedAgent.setBuriedness(propertyValues.get(PropertyTypes.Buriedness));
        buriedAgent.setBuriedAgentID(IDConverter.getAgentEntityID(propertyValues.get(PropertyTypes.AgentIdIndex)));
        buriedAgent.setSendTime(sendTime);
        return buriedAgent;
    }

    @Override
    protected void setFields(BuriedAgentSay buriedAgentSay) {
        buildingIndex = IDConverter.getBuildingKey(buriedAgentSay.getBuildingID());
        hp = buriedAgentSay.getHp();
        damage = buriedAgentSay.getDamage();
        buriedness = buriedAgentSay.getBuriedness();
        buriedAgentIDIndex = IDConverter.getAgentsKey(buriedAgentSay.getBuriedAgentID());
//        System.out.println("BuriedIDIndex: " + buriedAgentIDIndex + " building index: " + buriedAgentSay.getBuildingID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.HealthPoint, new HealthPointProperty(hp));
        properties.put(PropertyTypes.Damage, new DamageProperty(damage));
        properties.put(PropertyTypes.Buriedness, new BuildingIndexProperty(buriedness));
        properties.put(PropertyTypes.AgentIdIndex, new AgentIdIndexProperty(buriedAgentIDIndex));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
    }

    @Override
    protected void setReceivers() {
        receivers.add(Receiver.AmbulanceTeam);
        receivers.add(Receiver.PoliceForce);
        receivers.add(Receiver.FireBrigade);
    }

    @Override
    protected void setChannelConditions() {
        channelConditions.add(ChannelCondition.High);
        channelConditions.add(ChannelCondition.Medium);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.BuriedAgentSay);
    }

//    @Override
//    protected void setSayTTL() {
//        setSayTTL(defaultSayTTL);
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuriedAgentSayMessage that = (BuriedAgentSayMessage) o;

        return buriedAgentIDIndex == that.buriedAgentIDIndex;

    }

    @Override
    public int hashCode() {
        return buriedAgentIDIndex;
    }
}
