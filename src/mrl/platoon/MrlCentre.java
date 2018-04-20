package mrl.platoon;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.message.MessageManager;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.IPathPlanner;
import mrl.world.routing.pathPlanner.PathPlanner;
import rescuecore2.config.Config;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Random;

/**
 * A sample centre agent.
 */
public abstract class MrlCentre extends StandardAgent<Building> implements MRLConstants {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlCentre.class);

    protected MrlWorld world;
    protected IPathPlanner pathPlanner;
    protected MessageManager messageManager;
    protected int thinkTime;
    protected Date thinkStartTime_;
    protected int ignoreCommandTime;

    @Override
    public String toString() {
        return "MrlCentre";
    }

    public void sendSubscribe(int... channel) {
        if (channel != null) {
            sendSubscribe(world.getTime(), channel);
        }
    }

    public void sendMessage(int channel, byte[] message) {
        sendSpeak(world.getTime(), channel, message);
    }

    public int getIgnoreCommandTime() {
        return ignoreCommandTime;
    }

    protected void postConnect() {
        super.postConnect();
        System.out.print(this);
        this.ignoreCommandTime = getConfig().getIntValue(MRLConstants.IGNORE_AGENT_COMMANDS_KEY);
        this.thinkTime = config.getIntValue(THINK_TIME_KEY);
        world = new MrlWorld(this, model.getAllEntities(), config);

        world.retrieveConfigParameters(config);

        this.pathPlanner = new PathPlanner(world);
//        world.preRoutingPartitions();

        int seed = getID().getValue() % 100;
        seed *= seed;
        this.random = new Random(System.currentTimeMillis() + seed);

        this.messageManager = new MessageManager(world, config);
        System.out.println(this + "  connected");
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        long start = System.currentTimeMillis();
        thinkStartTime_ = new Date();
        world.setTime(time);


        if (time < getIgnoreCommandTime()) {
            return;
        }

//        scanChannel(heard);
        sendSubscribe(messageManager.getChannelsToSubscribe());
//        messageManager.receive(time, heard);
        messageManager.receive(time, heard);

        world.merge(changed);
        world.updateAfterSense();

        messageManager.initializeCenterMessages();
        messageManager.repeatEmergencyMessages();
        messageManager.sendEmergencyMessages();

        try {
            act();

        } catch (CommandException e) {
            Logger.info("ACT:" + e.getMessage());
        } catch (TimeOutException e) {
            Logger.error("Time Up:", e);
        }

//        It should send messages after all works, if had time
        messageManager.sendMessages();

//        setAndSendDebugData();
        sendRest(time);
        long end = System.currentTimeMillis();
        if (end - start > thinkTime) {
            Logger.warn("Time:" + time + " cycle needed:" + (end - start) + "ms");
            System.err.println("Time:" + time + " Agent:" + this + " cycle needed:" + (end - start) + "ms");
        }
    }

    public abstract void act() throws CommandException, TimeOutException;

    public String getDebugString() {
        return "Time:" + world.getTime() + " Me:" + me() + " ";
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        if (world != null && world.getSelfBuilding() != null) {
            if (world.getSelfBuilding() instanceof FireStation) {
                return EnumSet.of(StandardEntityURN.FIRE_STATION);
            } else if (world.getSelfBuilding() instanceof PoliceOffice) {
                return EnumSet.of(StandardEntityURN.POLICE_OFFICE);
            } else {
                return EnumSet.of(StandardEntityURN.AMBULANCE_CENTRE);
            }
        } else {

            return EnumSet.of(StandardEntityURN.FIRE_STATION,
                    StandardEntityURN.AMBULANCE_CENTRE,
                    StandardEntityURN.POLICE_OFFICE);
        }
    }

    public MrlWorld getWorld() {
        return world;
    }

    public Config getConfig() {
        return config;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public void createMessages() {

    }

    public IPathPlanner getPathPlanner() {
        return pathPlanner;
    }

    public Random getRandom() {
        return random;
    }

    public void processMessage(MessageEntity messageEntity) {
        //throw new NotImplementedException();
    }
}