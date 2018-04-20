/*
package mrl.communication2013.message.type;

import mrl.communication2013.entities.ClearedRoad;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;

*/
/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 1:03 PM
 * Author: Mostafa Movahedi
 *//*

public class ClearedRoadMessage extends AbstractMessage<ClearedRoad> {
    int roadIndex;

    public ClearedRoadMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(1);
    }

    public ClearedRoadMessage(ClearedRoad clearedRoad) {
        super(clearedRoad);
        setDefaultSayTTL(1);
        setSayTTL();
    }

    public ClearedRoadMessage() {
        super();
        setDefaultSayTTL(1);
        setSayTTL();
        createProperties();
    }

    @Override
    public ClearedRoad read(int sendTime) {
        return new ClearedRoad(IDConverter.getRoadID(propertyValues.get(PropertyTypes.RoadIndex)), sendTime);
    }

    @Override
    public void setFields(ClearedRoad clearedRoad) {
        this.roadIndex = IDConverter.getRoadKey(clearedRoad.getRoadID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.RoadIndex, new RoadIndexProperty(roadIndex));
    }

    @Override
    protected void setSendTypes() {
//        sendTypes.add(SendType.Say);
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
        setMessageType(MessageTypes.ClearedRoad);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
//        if (!super.equals(o)) return false;

        ClearedRoadMessage that = (ClearedRoadMessage) o;

        if (roadIndex != that.roadIndex) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return roadIndex;
    }
}
*/
