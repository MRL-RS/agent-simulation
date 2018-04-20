package mrl.communication2013.message.type;

import mrl.communication2013.entities.CivilianSeen;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:47 PM
 * Author: Mostafa Movahedi
 */
public class CivilianSeenMessage extends AbstractMessage<CivilianSeen> {
    int civilianID;
    int buriedness;
    int damage;
    int hp;
    int buildingIndex;
    int timeToRefuge;

    public CivilianSeenMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(30);
    }

    public CivilianSeenMessage(CivilianSeen civilianSeen) {
        super(civilianSeen);
        setDefaultSayTTL(30);
        setSayTTL();
    }

    public CivilianSeenMessage() {
        super();
        setDefaultSayTTL(30);
        setSayTTL();
        createProperties();
    }

    @Override
    public CivilianSeen read(int sendTime) {
        EntityID civilianID = new EntityID(propertyValues.get(PropertyTypes.HumanID));
        int buriedness = propertyValues.get(PropertyTypes.Buriedness);
        int damage = propertyValues.get(PropertyTypes.Damage);
        int hp = propertyValues.get(PropertyTypes.HealthPoint);
        EntityID buildingID = IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex));
        int timeToRefuge = propertyValues.get(PropertyTypes.TimeToRefuge);
        return new CivilianSeen(civilianID, buriedness, damage, hp, buildingID, timeToRefuge, sendTime);
    }

    @Override
    protected void setFields(CivilianSeen civilianSeen) {
        this.civilianID = civilianSeen.getCivilianID().getValue();
        this.buriedness = civilianSeen.getBuriedness();
        this.damage = civilianSeen.getDamage();
        this.hp = civilianSeen.getHp();
        this.buildingIndex = IDConverter.getBuildingKey(civilianSeen.getBuildingID());
        this.timeToRefuge = civilianSeen.getTimeToRefuge();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.HumanID, new HumanIDProperty(civilianID));
        properties.put(PropertyTypes.Buriedness, new BuriednessProperty(buriedness));
        properties.put(PropertyTypes.Damage, new DamageProperty(damage));
        properties.put(PropertyTypes.HealthPoint, new HealthPointProperty(hp));
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.TimeToRefuge, new TimeToRefugeProperty(timeToRefuge));

    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Emergency);
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
        channelConditions.add(ChannelCondition.Low);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.CivilianSeen);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CivilianSeenMessage)) {
            return false;
        }
        CivilianSeenMessage message = (CivilianSeenMessage) obj;
        if (message.getPropertyValues().get(PropertyTypes.HumanID).equals(propertyValues.get(PropertyTypes.HumanID))) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return propertyValues.get(PropertyTypes.HumanID);
//        return super.hashCode();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
