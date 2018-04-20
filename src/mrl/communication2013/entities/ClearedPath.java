//package mrl.communication2013.entities;
//
//import mrl.communication2013.message.type.MessageTypes;
//import rescuecore2.worldmodel.EntityID;
//
///**
// * Created with IntelliJ IDEA.
// * User: MRL
// * Date: 5/23/13
// * Time: 3:54 PM
// *
// * @Author: Mostafa Movahedi
// */
//public class ClearedPath extends MessageEntity {
//    EntityID pathID;
//
//    public ClearedPath(EntityID pathID, int sendTime) {
//        super(sendTime);
//        this.pathID = pathID;
//    }
//
//    public ClearedPath() {
//        super();
//    }
//
//    public EntityID getPathID() {
//        return pathID;
//    }
//
//    public void setPathID(EntityID pathID) {
//        this.pathID = pathID;
//    }
//
//    @Override
//    protected void setMessageEntityType() {
//        setMessageEntityType(MessageTypes.ClearedPath);
//    }
//}
