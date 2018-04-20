package mrl.communication2013.message.channel;

import mrl.common.Util;
import mrl.communication2013.message.property.Receiver;
import mrl.world.MrlWorld;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * dar in raveshe subscribe ma az channel haei ke dar ekhtiar darim zir majmue haei dorost mikonim ke size anha be mizane maximum subscribe ha bashad.
 * yani agar max platoon = 2 bashad zir majmue haye ma hamegi 2taei hastand.
 * dar enteha majmuei ke bozorgtarin meghdar ra dashte bashad be onvane channel haye agent entekhab mishavad.
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 1/5/14
 * Time: 7:08 PM
 *
 * @Author: Mostafa Shabani
 */
public class ChannelManager2014 extends AbstractChannelManager {
    public ChannelManager2014(MrlWorld world, Config config) {
        super(world, config);

        this.channelsContributionMap = new HashMap<Integer, Double>();
        this.channelsMatrixMap = new HashMap<List<Integer>, Double>();

        if (maxPlatoonChannels > 0) {
            subscribe(Util.sortByValueDec(allChannelsBandwidthMap));
        }

        finishSubscribe();

    }

    @Override
    protected void setWorldCommunicationCondition() {
        if (channelsCount == 1) {
            world.setCommunicationLess(true);
            return;
        }
        int size = 0;
        for (int ch : channelBandwidth4SendMessage.keySet()) {
            size += channelBandwidth4SendMessage.get(ch);
        }
//        if (size <= 256) {
//            world.setCommunicationLow(true);
//        } else if (size <= 1024) {
//            world.setCommunicationMedium(true);
//        } else {
//            world.setCommunicationHigh(true);
//        }
        if (size < 100) {
            world.setCommunicationLow(true);
        } else if (size < 320) {
            world.setCommunicationMedium(true);
        } else {
            world.setCommunicationHigh(true);
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

    private int pos, index;
    private Map<List<Integer>, Double> channelsMatrixMap;
    private Map<Integer, Double> channelsContributionMap;//id, Contribution

    private void subscribe(List<Integer> sortedChannels) {
        int[] agentChannels = new int[maxPlatoonChannels];
        int ATSize = world.getAmbulanceTeams().size();
        int FBSize = world.getFireBrigades().size();
        int PFSize = world.getPoliceForces().size();
        int centreSize = world.getCentres().size();
        int totalAgents = (((ATSize + FBSize + PFSize) * 5) / (7)) + 1;
        int myIndex;
        int ATExtra = (ATSize) % maxPlatoonChannels;
        int FBATExtra = (ATSize + FBSize) % maxPlatoonChannels;
        int PFFBATExtra = (ATSize + FBSize + PFSize) % maxPlatoonChannels;

        createMatrix(sortedChannels);

        //find max CO
        List<Integer> maxCoChannels = new ArrayList<Integer>();
        double maxCo = 0;
        for (List<Integer> list : channelsMatrixMap.keySet()) {
            if (maxCo < channelsMatrixMap.get(list)) {
                maxCo = channelsMatrixMap.get(list);
                maxCoChannels = list;
            }
        }

        double totalBW = 0;
        for (Integer ch : maxCoChannels) {
            totalBW += allChannelsBandwidthMap.get(ch);
        }

        double[] channelsPer = new double[maxPlatoonChannels];
        int i = 0;
        for (Integer ch : maxCoChannels) {
            agentChannels[i] = ch;

            channelsPer[i] = allChannelsBandwidthMap.get(ch) / totalBW;
            i++;
        }

        if (world.getSelfHuman() instanceof AmbulanceTeam) {
            myIndex = findIndex(ATSize, world.getAmbulanceTeamList().indexOf(world.getSelfHuman()), channelsPer);
        } else if (world.getSelfHuman() instanceof FireBrigade) {
            myIndex = findIndex(FBSize, world.getFireBrigadeList().indexOf(world.getSelfHuman()), channelsPer);
            myIndex = (myIndex + ATExtra) % maxPlatoonChannels;
        } else if (world.getSelfHuman() instanceof PoliceForce) {
            myIndex = findIndex(PFSize, world.getPoliceForceList().indexOf(world.getSelfHuman()), channelsPer);
            myIndex = (myIndex + FBATExtra) % maxPlatoonChannels;
        } else {
            int in = 0;
            for (StandardEntity entity : world.getCentres()) {
                if (world.getSelf().getID().equals(entity.getID())) {
                    break;
                }
                in++;
            }
            myIndex = findIndex(centreSize, in, channelsPer);
            myIndex = (myIndex + PFFBATExtra) % maxPlatoonChannels;
        }

        Map<Receiver, Integer> agentsChannelsMap = new HashMap<Receiver, Integer>();
        agentsChannelsMap.put(Receiver.AmbulanceTeam, agentChannels[myIndex]);
        agentsChannelsMap.put(Receiver.FireBrigade, agentChannels[myIndex]);
        agentsChannelsMap.put(Receiver.PoliceForce, agentChannels[myIndex]);

        Map<Integer, Integer> channels4SendMessage = new HashMap<Integer, Integer>();
        for (Integer ch : agentsChannelsMap.values()) {
            int mineBW = allChannelsBandwidthMap.get(ch) / 3;
//            int mineBW = (int) (allChannelsBandwidthMap.get(ch) / (totalAgents * channelsPer[myIndex]));
//            if (mineBW < 60) {
//                mineBW = 60;
//            }
            channels4SendMessage.put(ch, mineBW);
        }

        setMineListenChannels(agentChannels);
        setChannelBandwidth4SendMessage(channels4SendMessage);
        setAgentChannelMap(agentsChannelsMap);

    }

    private int findIndex(int size, int myIndex, double[] channelsPer) {
        int tt = 0;
        for (int i = 0; i < channelsPer.length; i++) {
            double d = channelsPer[i];
            int v = (int) Math.round(size * d);
            if (myIndex >= tt && myIndex < tt + v) {
                return i;
            }
            tt += v;
        }
        return 0;
    }

    private void createMatrix(List<Integer> sortedChannels) {

        List<Integer> channels = fillChannelsContribution(sortedChannels);
        List<Integer> ch = new ArrayList<Integer>();
        ch.add(channels.get(0));
        pos = 0;
        index = 0;
        replace(channels, ch);

    }

    private List<Integer> fillChannelsContribution(List<Integer> sortedChannels) {
        List<Integer> channels = new ArrayList<Integer>();
        int totalBW = 0;
        for (Integer ch : sortedChannels) {
            if (ch == 0) {
                continue;
            }
            channels.add(ch);
            int bw = allChannelsBandwidthMap.get(ch);
            totalBW += bw;
        }
        double eachContribution = totalBW / (AT_CONTRIBUTION + FB_CONTRIBUTION + PF_CONTRIBUTION);
        for (Integer ch : channels) {
            int bw = allChannelsBandwidthMap.get(ch);
            BigDecimal bd = new BigDecimal(bw / eachContribution);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            channelsContributionMap.put(ch, bd.doubleValue());
        }
        return channels;
    }

    private void replace(List<Integer> channels, List<Integer> ch) {
        int d = maxPlatoonChannels - pos;
        if (pos < maxPlatoonChannels - 1) {
            index++;
            ch.add(channels.get(index));
            pos++;
            replace(channels, ch);
        } else {
            {// add created list to map
                double total = 0;
                List<Integer> finalList = new ArrayList<Integer>();
                for (Integer c : ch) {
                    total += channelsContributionMap.get(c);
                    finalList.add(c);
                }
                channelsMatrixMap.put(finalList, total);
            }

            if (index + d < channels.size()) {
                index++;
                ch.remove(pos);
                ch.add(channels.get(index));
                replace(channels, ch);
            } else {
                back(channels, ch);
                if (pos < 0) {
                    return;
                }
                replace(channels, ch);
            }
        }
    }

    private void back(List<Integer> channels, List<Integer> ch) {
        int d = maxPlatoonChannels - pos;
        ch.remove(pos);
        pos--;
        if (pos < 0) {
            return;
        }
        int cc = ch.get(pos);
        index = channels.indexOf(cc);
        index++;
        if (index + d < channels.size()) {
            ch.remove(pos);
            ch.add(channels.get(index));
        } else {
            back(channels, ch);
        }
    }

}
