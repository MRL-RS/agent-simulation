//package mrl.communication2013.message.type;
//
//import mrl.communication2013.entities.ClearedPath;
//import mrl.communication2013.message.IDConverter;
//import mrl.communication2013.message.Message;
//import mrl.communication2013.message.property.*;
//
///**
// * Created with IntelliJ IDEA.
// * User: MRL
// * Date: 5/23/13
// * Time: 3:54 PM
// *
// * @Author: Mostafa Movahedi
// */
//public class ClearedPathMessage extends AbstractMessage<ClearedPath> {
//    int pathIndex;
//
//    public ClearedPathMessage(Message msgToRead, SendType sendType) {
//        super(msgToRead, sendType);
//        setDefaultSayTTL(1);
//    }
//
//    public ClearedPathMessage(ClearedPath clearedPath) {
//        super(clearedPath);
//        setDefaultSayTTL(1);
//        setSayTTL();
//    }
//
//    public ClearedPathMessage() {
//        super();
//        setDefaultSayTTL(1);
//        setSayTTL();
//        createProperties();
//    }
//
//    @Override
//    public ClearedPath read(int sendTime) {
//        return new ClearedPath(IDConverter.getRoadID(propertyValues.get(PropertyTypes.RoadIndex)), sendTime);
//    }
//
//    @Override
//    protected void setFields(ClearedPath clearedPath) {
//        pathIndex = IDConverter.getRoadKey(clearedPath.getPathID());
//    }
//
//    @Override
//    protected void createProperties() {
//        properties.put(PropertyTypes.RoadIndex, new RoadIndexProperty(pathIndex));
//    }
//
//    @Override
//    protected void setSendTypes() {
//        sendTypes.add(SendType.Say);
//        sendTypes.add(SendType.Speak);
//    }
//
//    @Override
//    protected void setReceivers() {
//        receivers.add(Receiver.AmbulanceTeam);
//        receivers.add(Receiver.PoliceForce);
//        receivers.add(Receiver.FireBrigade);
//    }
//
//    @Override
//    protected void setChannelConditions() {
//        channelConditions.add(ChannelCondition.High);
//        channelConditions.add(ChannelCondition.Medium);
//    }
//
//    @Override
//    protected void setMessageType() {
//        setMessageType(MessageTypes.ClearedPath);
//    }
//
//    @Override
//    protected void setSayTTL() {
//        setSayTTL(defaultSayTTL);
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        if (!super.equals(o)) return false;
//
//        ClearedPathMessage that = (ClearedPathMessage) o;
//
//        if (pathIndex != that.pathIndex) return false;
//
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        return pathIndex;
//    }
//}
