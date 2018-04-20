package mrl.communication2013.message.channel;

import javolution.util.FastSet;
import mrl.communication2013.message.property.ChannelCondition;
import mrl.communication2013.message.property.Receiver;
import mrl.communication2013.message.type.AbstractMessage;
import mrl.communication2013.message.type.MessageTypes;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import rescuecore2.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * dar in class tedadi az parametr haye morede niaz baraye subscribe kardane agent ha por mishavad.
 * va dar class haei ke az in extend shode and raveshe subscribe piade saze shode ast.
 * baraye har agent sahmi az channel ha dar nazar gerefte shode ast ke dar barkhi sharayet az aan baraye taghsim kardane channel ha estefade mishavad.
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 1/9/14
 * Time: 11:42 AM
 *
 * @Author: Mostafa Shabani
 */
public abstract class AbstractChannelManager {
    public static final String COMMS_KEY = "comms";
    public static final String CHANNELS_KEY = "channels";
    public static final String COMMUNICATIONS_KEY = COMMS_KEY + "." + CHANNELS_KEY;
    public static final String MAX_KEY = "max";
    public static final String PLATOON_KEY = "platoon";
    public static final String CENTRE_KEY = "centre";
    public static final String MAX_PLATOON_CHANNELS_KEY = COMMUNICATIONS_KEY + "." + MAX_KEY + "." + PLATOON_KEY;
    public static final String MAX_CENTRE_CHANNELS_KEY = COMMUNICATIONS_KEY + "." + MAX_KEY + "." + CENTRE_KEY;
    public static final String MESSAGES_KEY = "messages";
    public static final String COUNT_KEY = "count";
    public static final String SIZE_KEY = "size";
    public static final String BANDWIDTH_KEY = "bandwidth";
    public static final String CHANNEL_NO_KEY = "NO";
    protected static final int AT_CONTRIBUTION = 5;
    protected static final int FB_CONTRIBUTION = 4;
    protected static final int PF_CONTRIBUTION = 3;
    public static final int SAY_CHANNEL = 0;
    /*
    2(9)	NOP
    5(36)	BurningBuilding
    3(24)	AgentInfo
    7(51)	BuriedAgent
    3(20)	ClearedRoad
    3(19)	EmptyBuilding
    3(23)	ExtinguishedBuilding
    3(22)	FullBuilding
    3(20)	WaterMessage
    11(88)	HeardCivilian
    5(40)	Loader
    5(40)	RescuedCivilian
    5(40)	TransportingCivilian
    11(88)	CivilianSeen
    3(20)	ClearedPath
    3(19)	WarmBuilding
     */


    public AbstractChannelManager(MrlWorld world, Config config) {
        this.world = world;

        this.allChannelsBandwidthMap = new HashMap<Integer, Integer>();
        this.messageChannelsMap = new HashMap<MessageTypes, Set<Integer>>();

        String channelCountKey = COMMUNICATIONS_KEY + "." + COUNT_KEY;
        String voiceSizeKey = COMMUNICATIONS_KEY + "." + SAY_CHANNEL + "." + MESSAGES_KEY + "." + SIZE_KEY;
        String channelBandwidthKey = COMMUNICATIONS_KEY + "." + CHANNEL_NO_KEY + "." + BANDWIDTH_KEY;

        channelsCount = config.getIntValue(channelCountKey);
        channelConditions = new ChannelCondition[channelsCount];
        voiceChannelBandwidth = config.getIntValue(voiceSizeKey);
        allChannelsBandwidthMap.put(0, voiceChannelBandwidth);

        int size;
        for (int i = 1; i < channelsCount; i++) {
            size = config.getIntValue(channelBandwidthKey.replace(CHANNEL_NO_KEY, String.valueOf(i)));
            allChannelsBandwidthMap.put(i, size);
        }

        maxPlatoonChannels = config.getIntValue(MAX_PLATOON_CHANNELS_KEY);
        maxCentreChannels = config.getIntValue(MAX_CENTRE_CHANNELS_KEY);
        if (world.getSelf() instanceof MrlCentre) {
            if (maxPlatoonChannels > maxCentreChannels) {
                maxPlatoonChannels = maxCentreChannels;
            }
        }
        setChannelConditions();
    }

    protected MrlWorld world;
    protected Map<Integer, Integer> allChannelsBandwidthMap;//id, size
    protected Map<MessageTypes, Set<Integer>> messageChannelsMap;
    protected ChannelCondition[] channelConditions;
    protected int[] mineListenChannels;
    protected Map<Integer, Integer> channelBandwidth4SendMessage;
    protected Map<Receiver, Integer> agentChannelMap;

    public int channelsCount;
    public int maxPlatoonChannels;
    public int maxCentreChannels;
    public int voiceChannelBandwidth;

    protected abstract void setWorldCommunicationCondition();

    protected abstract void setMineListenChannels(int[] mineListenChannels);

    protected abstract void setChannelBandwidth4SendMessage(Map<Integer, Integer> channelBandwidth4SendMessage);

    protected abstract void setAgentChannelMap(Map<Receiver, Integer> agentChannelMap);

    protected void finishSubscribe() {
        setWorldCommunicationCondition();
        if (channelsCount > 1) {
            setMessagesChannelMap();
            if (MrlPlatoonAgent.DEBUG_MESSAGING) {
                String c = "";
                for (int mineListenChannel : mineListenChannels) {
                    c += mineListenChannel + ",";
                }
                c = c.substring(0, c.length() - 1);
                world.printData("subs on [" + c + "]"
                        + " AT = " + agentChannelMap.get(Receiver.AmbulanceTeam) + " BW = " + channelBandwidth4SendMessage.get(agentChannelMap.get(Receiver.AmbulanceTeam))
                        + " FB = " + agentChannelMap.get(Receiver.FireBrigade) + " BW = " + channelBandwidth4SendMessage.get(agentChannelMap.get(Receiver.FireBrigade))
                        + " PF = " + agentChannelMap.get(Receiver.PoliceForce) + " BW = " + channelBandwidth4SendMessage.get(agentChannelMap.get(Receiver.PoliceForce)));
            }
        }
    }

    public Set<Integer> getChannels(MessageTypes messageType) {
        Set<Integer> channels;

        channels = messageChannelsMap.get(messageType);
        if (channels == null) {
            channels = new FastSet<Integer>();
            if (channelsCount > 1) {
                channels.add(1);
            }
        }
        return channels;
    }

    private void setChannelConditions() {
        for (int i = 0; i < channelsCount; i++) {
            if (allChannelsBandwidthMap.get(i) <= 256) {
                channelConditions[i] = ChannelCondition.Low;
            } else if (allChannelsBandwidthMap.get(i) <= 1024) {
                channelConditions[i] = ChannelCondition.Medium;
            } else {
                channelConditions[i] = ChannelCondition.High;
            }
        }
    }

    private void setMessagesChannelMap() {

        for (MessageTypes messageType : MessageTypes.values()) {
            Set<Integer> channels = new FastSet<Integer>();

            Class<? extends AbstractMessage> messageClass = messageType.abstractMessageClass;
            try {
                AbstractMessage message = messageClass.newInstance();
                Set<Receiver> receivers = message.getReceivers();
                for (Receiver receiver : receivers) {
                    switch (receiver) {
                        case AmbulanceTeam:
                            channels.add(agentChannelMap.get(Receiver.AmbulanceTeam));
                            break;
                        case FireBrigade:
                            channels.add(agentChannelMap.get(Receiver.FireBrigade));
                            break;
                        case PoliceForce:
                            channels.add(agentChannelMap.get(Receiver.PoliceForce));
                            break;
                    }
                }
                messageChannelsMap.put(messageType, channels);
            } catch (InstantiationException ignore) {
            } catch (IllegalAccessException ignore) {
            }
        }
    }

    public int[] getChannelsToSubscribe() {
        return mineListenChannels;
    }

    public ChannelCondition getChannelCondition(int channel) {
        return channelConditions[channel];
    }

    public int getMineBandwidths(int channel) {
        return channelBandwidth4SendMessage.get(channel);
    }
}
