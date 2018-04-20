package mrl.communication2013.message.type;

import mrl.communication2013.entities.BuriedAgent;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 4:57 PM
 * Author: Mostafa Movahedi
 */
public class BuriedAgentMessage extends AbstractMessage<BuriedAgent> {

    int buildingIndex;
    int hp;
    int buriedness;
    int damage;

    public BuriedAgentMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
//        setDefaultSayTTL(5);
    }

    public BuriedAgentMessage(BuriedAgent buriedAgent) {
        super(buriedAgent);
//        setDefaultSayTTL(5);
//        setSayTTL();
    }

    public BuriedAgentMessage() {
        super();
//        setDefaultSayTTL(5);
//        setSayTTL();
        createProperties();
    }

    @Override
    public BuriedAgent read(int sendTime) {
        BuriedAgent buriedAgent = new BuriedAgent();
        buriedAgent.setBuildingID(IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex)));
        buriedAgent.setHp(propertyValues.get(PropertyTypes.HealthPoint));
        buriedAgent.setDamage(propertyValues.get(PropertyTypes.Damage));
        buriedAgent.setBuriedness(propertyValues.get(PropertyTypes.Buriedness));
        buriedAgent.setSendTime(sendTime);
        return buriedAgent;
    }

    @Override
    public void setFields(BuriedAgent buriedAgent) {
        buildingIndex = IDConverter.getBuildingKey(buriedAgent.getBuildingID());
        hp = buriedAgent.getHp();
        damage = buriedAgent.getDamage();
        buriedness = buriedAgent.getBuriedness();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.HealthPoint, new HealthPointProperty(hp));
        properties.put(PropertyTypes.Damage, new DamageProperty(damage));
        properties.put(PropertyTypes.Buriedness, new BuildingIndexProperty(buriedness));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Speak);
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
        setMessageType(MessageTypes.BuriedAgent);
    }

    @Override
    public int hashCode() {
        return sender == null ? -1 : sender.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuriedAgentMessage that = (BuriedAgentMessage) o;

        return getSender().equals(that.getSender());

    }
}
