package mrl.communication2013.message.channel;

import mrl.communication2013.message.property.Receiver;
import mrl.world.MrlWorld;
import rescuecore2.config.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 1/9/14
 * Time: 11:51 AM
 *
 * @Author: Mostafa Shabani
 */
public class ChannelManager2013 extends AbstractChannelManager {
    public ChannelManager2013(MrlWorld world, Config config) {
        super(world, config);

        maxBandwidthChannel = 1;
        subscribe();

        finishSubscribe();
    }

    @Override
    protected void setWorldCommunicationCondition() {
        if (channelsCount == 1) {
            world.setCommunicationLess(true);
            return;
        }
        switch (channelConditions[maxBandwidthChannel]) {
            case Low:
                world.setCommunicationLow(true);
                break;
            case Medium:
                world.setCommunicationMedium(true);
                break;
            case High:
                world.setCommunicationHigh(true);
                break;
        }
    }

    @Override
    protected void setMineListenChannels(int[] mineListenChannels) {
        super.mineListenChannels = mineListenChannels;
    }

    @Override
    protected void setChannelBandwidth4SendMessage(Map<Integer, Integer> channels4SendMessage) {
        super.channelBandwidth4SendMessage = channels4SendMessage;
    }

    @Override
    protected void setAgentChannelMap(Map<Receiver, Integer> agentsChannelsMap) {
        super.agentChannelMap = agentsChannelsMap;
    }

    private int maxBandwidthChannel;

    private void subscribe() {
        int maxBW = 0;
        for (Integer ch : allChannelsBandwidthMap.keySet()) {
            if (ch == 0) {
                continue;
            }
            if (allChannelsBandwidthMap.get(ch) > maxBW) {
                maxBandwidthChannel = ch;
                maxBW = allChannelsBandwidthMap.get(ch);
            }
        }

        Map<Integer, Integer> channels4SendMessage = new HashMap<Integer, Integer>();
        channels4SendMessage.put(maxBandwidthChannel, maxBW);

        Map<Receiver, Integer> agentsChannelsMap = new HashMap<Receiver, Integer>();
        agentsChannelsMap.put(Receiver.AmbulanceTeam, maxBandwidthChannel);
        agentsChannelsMap.put(Receiver.FireBrigade, maxBandwidthChannel);
        agentsChannelsMap.put(Receiver.PoliceForce, maxBandwidthChannel);

        setMineListenChannels(new int[]{maxBandwidthChannel});
        setChannelBandwidth4SendMessage(channels4SendMessage);
        setAgentChannelMap(agentsChannelsMap);

    }
}
