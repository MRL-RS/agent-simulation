package mrl.platoon;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.LaunchMRL;
import mrl.MrlPersonalData;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.common.*;
import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.message.MessageManager;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.helper.HumanHelper;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.mrlPersonal.viewer.layers.MrlHumanLayer;
import mrl.partition.Partition;
import mrl.platoon.genericsearch.*;
import mrl.police.MrlPoliceForce;
import mrl.police.MrlPoliceForceWorld;
import mrl.police.clear.GuidelineFactory;
import mrl.world.MrlWorld;
import mrl.world.object.MrlRoad;
import mrl.world.routing.pathPlanner.AverageTools;
import mrl.world.routing.pathPlanner.IPathPlanner;
import mrl.world.routing.pathPlanner.PathPlanner;
import org.apache.log4j.Logger;
import rescuecore2.Constants;
import rescuecore2.config.Config;
import rescuecore2.messages.Command;
import rescuecore2.messages.control.KASense;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * User: mrl
 * Date: Apr 28, 2010
 * Time: 10:56:00 PM
 */
public abstract class MrlPlatoonAgent<T extends StandardEntity> extends StandardAgent<T> implements MRLConstants {
    private static final long TIMEOUT_THRESHOLD = 150000;
    public static final String INTEGER_DATA = "integer";
    private Logger Logger = org.apache.log4j.Logger.getLogger(this.getClass());
    protected MrlWorld world;
    protected IPathPlanner pathPlanner;
    protected MessageManager messageManager;
    //    protected ISearchHelper searchHelper;

    private String lastCommand;
    protected State agentState = State.SEARCHING;
    private List<EntityID> visitedBuildings;

    protected boolean isHardWalking = false;
    //    protected boolean isStuck = false;
    protected boolean isWorking = false;
    private boolean movingRendezvous = false;
    private boolean onRendezvous = false;

    public DefaultSearchManager defaultSearchManager;
    public StupidSearchManager stupidSearchManager;
    public SimpleSearchManager simpleSearchManager;
    public SimpleSearchDecisionMaker simpleSearchDecisionMaker;
    //    public CheckBlockadesManager checkBlockadesManager;
    //    public ManualSearchManager manualSearchManager;
    private List<StandardEntity> roads;
    public ExploreAroundFireSearchManager exploreAroundFireSearchManager;
    public HeatTracerSearchManager heatTracerSearchManager;
    public CivilianSearchManager civilianSearchManager;
    public CivilianSearchBBDecisionMaker civilianSearchBBDecisionMaker;
    public SenseSearchStrategy senseSearchStrategy;
    public CivilianSearchStrategy civilianSearchStrategy;
    public StupidSearchDecisionMaker stupidSearchDecisionMaker;
    public ExploreAroundFireDecisionMaker exploreAroundFireDecisionMaker;
    public LegacyHeatTracerDecisionMaker legacyHeatTracerDecisionMaker;
    public HeatTracerDecisionMaker heatTracerDecisionMaker;
    //    public CheckBlockadesDecisionMaker checkBlockadesDecisionMaker;
    public ManualSearchDecisionMaker manualSearchDecisionMaker;
    public AverageTools averageTools;

    private Set<StandardEntity> unReachablePositions = new FastSet<StandardEntity>();
    private Map<EntityID, Integer> unReachablePositionTime = new FastMap<EntityID, Integer>();

    public StuckChecker stuckChecker;
    private boolean timeout;

    public MrlPlatoonAgent() {
        super();
        timeout = false;
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                timeout = true;
            }
        };
        timer.schedule(timerTask, TIMEOUT_THRESHOLD);
    }

    public void sendSubscribe(int... channel) {
        if (channel != null) {
            sendSubscribe(world.getTime(), channel);
        }
    }

    public void sendMessage(int channel, byte[] message) {
        sendSpeak(world.getTime(), channel, message);
    }

    public void sendMoveAct(int time, List<EntityID> path) throws CommandException {
        super.sendMove(time, path);
        throw new CommandException("Move");
    }

    public void sendMoveAct(int time, List<EntityID> path, int destinationX, int destinationY) throws CommandException {
        ((PathPlanner)pathPlanner).setThisCycleMoveToPoint(true);
        super.sendMove(time, path, destinationX, destinationY);
        throw new CommandException("Move To Point");
    }

    public void sendRandomWalkAct(int time, List<EntityID> path) throws CommandException {
        super.sendMove(time, path);
        throw new CommandException("Random Walk");
    }

    public void sendRestAct(int time) throws CommandException {
        super.sendRest(time);
        throw new CommandException("Rest");
    }

    protected void sendRescueAct(int time, EntityID target) throws CommandException {
        super.sendRescue(time, target);
        throw new CommandException("Rescue");
    }

    protected void sendLoadAct(int time, EntityID target) throws CommandException {
        super.sendLoad(time, target);
        throw new CommandException("Load");
    }

    protected void sendUnloadAct(int time) throws CommandException {
        super.sendUnload(time);
        throw new CommandException("Unload");
    }

    public void sendClearAct(int time, EntityID target) throws CommandException {
        super.sendClear(time, target);
        throw new CommandException("Clear");
    }

    public void sendClearAct(int time, int destinationX, int destinationY) throws CommandException {
        super.sendClear(time, destinationX, destinationY);
        throw new CommandException("Clear");
    }

    public void sendExtinguishAct(int time, EntityID target, int water) throws CommandException {
        super.sendExtinguish(time, target, water);
        throw new CommandException("Extinguish");
    }

    protected void updateSelfPosition(ChangeSet changeSet) {
        for (EntityID entity : changeSet.getChangedEntities()) {
            if (getID().equals(entity)) {
                Human human = (Human) world.getEntity(entity);
                for (Property p : changeSet.getChangedProperties(entity)) {
                    human.getProperty(p.getURN()).takeValue(p);
                }
                return;
            }
        }
    }

    public void restAtRefuge() throws CommandException {
        setAgentState(State.RESTING);
        if (world.getSelfHuman().getBuriedness() > 0) {
            throw new CommandException("I'm Buried");
        }
        if (world.getSelfPosition() instanceof Refuge) {
            sendRestAct(world.getTime());
        } else if (!world.getRefuges().isEmpty()) {
            if (world.getSelfHuman() instanceof PoliceForce) {
//                ((MrlPoliceForce) world.getSelf()).clearHere(PoliceActionStyle.CLEAR_NORMAL);
                List<EntityID> refugePath = pathPlanner.getRefugePath((Area) world.getSelfPosition(), true);
                if (!refugePath.isEmpty()) {
                    EntityID refugeID = refugePath.get(refugePath.size() - 1);
                    ((MrlPoliceForce) world.getSelf()).getClearActManager().clearWay(refugePath, refugeID);
                }
            }
            moveToRefuge();
        }
    }

    public boolean move(Area target, int maxDistance, boolean force) throws CommandException {
        pathPlanner.move(target, maxDistance, force);
        return false;
    }

    protected boolean move(Collection<? extends Area> targets, int maxDistance, boolean force) throws CommandException {
//        pathPlanner.move(targets, maxDistance, force);
        throw new UnsupportedOperationException();
    }

    protected void moveToRefuge() throws CommandException {
        pathPlanner.moveToRefuge();
    }

    public void moveToHydrant() throws CommandException {
        pathPlanner.moveToHydrant();
    }

    public void moveToPoint(EntityID area, int destX, int destY) throws CommandException {
        pathPlanner.moveToPoint(area, destX, destY);
    }

    public Config getConfig() {
        return config;
    }

    public IPathPlanner getPathPlanner() {
        return pathPlanner;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public Partition getMyPartition() {
        return world.getPartitions().getMyPartition();
    }

    public List<EntityID> getVisitedBuildings() {
        return visitedBuildings;
    }

    /**
     * TAVAJOH: baraye gereftane blockade ha hargez az in estefade nashe.
     *
     * @param entity e
     * @param range  r
     * @return c
     */
    public Collection<StandardEntity> getObjectsInRange(StandardEntity entity, int range) {
        return model.getObjectsInRange(entity, range);
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public State getAgentState() {
        return agentState;
    }

    public void setAgentState(State state) {
        world.getAgentStateMap().put(world.getTime(), state);
        world.getHelper(HumanHelper.class).setAgentSate(me().getID(), state);
        agentState = state;
    }

    public String getDebugString() {
        return "Time:" + world.getTime() + " Me:" + me() + " ";
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.POLICE_FORCE);
    }

    @Override
    protected void postConnect() {
        System.out.println(this + "  connecting...");
        super.postConnect();

        Logger.debug("Communication model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(MRLConstants.SPEAK_COMMUNICATION_MODEL_KEY) ? "Using speak model" : "Using say model");

        if (this instanceof MrlPoliceForce) {
            world = new MrlPoliceForceWorld(this, model.getAllEntities(), config);
        } else if (this instanceof MrlFireBrigade) {
            world = new MrlFireBrigadeWorld(this, model.getAllEntities(), config);
        } else if (this instanceof MrlAmbulanceTeam) {
            world = new MrlAmbulanceTeamWorld(this, model.getAllEntities(), config);
        } else {
            world = new MrlWorld(this, model.getAllEntities(), config);
        }

        MrlPersonalData.VIEWER_DATA.setPlatoonAgents(this.getID(), this);

        updateAgentStates(0);

//        world.setKernelTimeSteps(getConfig().getIntValue(KERNEL_TIMESTEPS));
        model = world;
        this.pathPlanner = new PathPlanner(world);

        GuidelineFactory guidelineFactory = new GuidelineFactory(world);
        guidelineFactory.generateGuidelines();

        if (HIGHWAY_STRATEGY) {
            pathPlanner.getGraph().setHighWayStrategy();
        }

        //No usage in 2015 codes, and it seems to be a huge consumer of memory and cpu
//        world.preRoutingPartitions();

        this.messageManager = new MessageManager(world, config);
        //messageManager.setMyOwnBW();
//        dataToFile = new LearnerIO("data/" + me().getID() + "data.txt", true);
//        dataToFile2 = new LearnerIO("data/civilianFound.txt", true);

        this.random = new Random(System.currentTimeMillis());
        visitedBuildings = new ArrayList<EntityID>();
//        visitedBuildingsHelp = new ArrayList<EntityID>();
        stuckChecker = new StuckChecker(world, 80, 6);

        senseSearchStrategy = new SenseSearchStrategy(this, world);
        civilianSearchStrategy = new CivilianSearchStrategy(this, world);
//        checkBlockadesDecisionMaker = new CheckBlockadesDecisionMaker(world);
//        checkBlockadesManager = new CheckBlockadesManager(world, this, checkBlockadesDecisionMaker, senseSearchStrategy);
        roads = new ArrayList<StandardEntity>(world.getRoads());
        averageTools = new AverageTools(world);
    }

    @Override
    protected void processSense(KASense sense) {
        long start = System.currentTimeMillis();
        Collection<Command> heard = sense.getHearing();
        try {
            think(sense.getTime(), sense.getChangeSet(), heard);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long end = System.currentTimeMillis();
        if (end - start > world.getThinkTime()) {
//            Logger.warn("Time:" + sense.getTime() + " cycle needed:" + (end - start) + "ms");
            System.err.println("Time:" + sense.getTime() + " Agent:" + this + " cycle needed:" + (end - start) + "ms");
        }
    }

    protected void storeNumberOfAgents(String extension, int size) {
        if(LaunchMRL.shouldPrecompute) {
            String filePath = String.format("%s%s%s", MRLConstants.PRECOMPUTE_DIRECTORY, INTEGER_DATA, extension);
            File file = new File(filePath);
            if(file.exists()){
                file.delete();
            }
            try {
                file.createNewFile();
                Util.writeObject(size, filePath);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        world.setThinkStartTime_(System.currentTimeMillis());
        isWorking = false;

        world.setTime(time);
        MrlHumanLayer.setTime(time);

        if (getID().equals(MrlViewer.CHECK_ID) || getID().equals(MrlViewer.CHECK_ID2)) {
            System.out.print(""); //todo
        }

        if (isDead()) {
            return;
        }

        updateAgentStates(time);
        updateSelfPosition(changed);

        sendSubscribe(messageManager.getChannelsToSubscribe());
        messageManager.receive(time, heard);
        world.updateBeforeSense();

        world.merge(changed);

        world.updateAfterSense();

        if (time < world.getIgnoreCommandTime()) {
            return;
        }
        stuckChecker.amITotallyStuck();
        messageManager.initializePlatoonMessages();
        messageManager.repeatEmergencyMessages();
        messageManager.sendEmergencyMessages();

        if (shouldRestAtRefuge()) {
            try {
                restAtRefuge();
            } catch (CommandException e) {
                lastCommand = e.getMessage();
                Logger.info(getDebugString() + " - ACT: " + e.getMessage());
                setAgentState(State.RESTING);

                // age ye agent damage dasht va nemitoonest kari bokone hade aghal message bede.
                messageManager.sendMessages();
                return;
            }
        }
//        civilianLogManager.execute();
        try {
            act();
//            randomWalk();
//            setAgentState(State.Inactive);
//            world.getHelper(HumanHelper.class).setAmIStuck(true);
        } catch (CommandException e) {

            lastCommand = e.getMessage();
            Logger.info(getDebugString() + " - ACT: " + e.getMessage());
            isWorking = true;
        } catch (TimeOutException e) {
            System.err.println(getDebugString() + "TimeOutException" + e.getMessage());
            isWorking = false;
            setAgentState(State.THINKING);
        } catch (Exception e) {
            e.printStackTrace();
            world.printData("I was crashed in this cycle, so going to walk random...");
            randomWalk();
            isWorking = false;
            setAgentState(State.CRASH);
        }

        if (world.getSelfHuman().getBuriedness() > 0) {
            isWorking = false;
            setAgentState(State.BURIED);
        }

//        messageManager.sendMessages(onRendezvous);
        messageManager.sendMessages();
//        messageManager.sendSayMessages();
//        world.getThisCycleVisitedBuildings().clear();
    }

    private void updateAgentStates(int time) {

        Human human;
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        if (time == world.getLastAfterShockTime()) {
            for (StandardEntity entity : world.getPlatoonAgents()) {
                human = (Human) entity;
                // if it is in building at the beginning of simulation, so it might be buried

                world.getAgentPositionMap().put(entity.getID(), human.getPosition());
                world.getAgentFirstPositionMap().put(entity.getID(), human.getPosition());

                if (human.getPosition(world) instanceof Building) {
                    world.getBuriedAgents().add(entity.getID());

//                    humanHelper.setLockedByBlockade(entity.getID(), true);

//                    System.out.println("time:"+world.getTime() + " me:"+world.getSelf().getID()+" think this is Buried:"+entity.getID());

                } else if (!(entity instanceof PoliceForce)) {// if position is instance of Road , so it might be blocked
                    humanHelper.setLockedByBlockade(entity.getID(), true);
//                    System.out.println("time:"+world.getTime() + " me:"+world.getSelf().getID()+" think this is BLOCKED:"+entity.getID());
                }

            }
        } else {
            for (StandardEntity entity : world.getPlatoonAgents()) {
                human = (Human) entity;
                if (humanHelper.isBlocked(entity.getID())) {
                    // if it is in building at the beginning of simulation, so it might be buried
                    if (!stuckChecker.isBlocked(entity)) {

                        world.getBuriedAgents().remove(entity.getID());
                        humanHelper.setLockedByBlockade(entity.getID(), false);
                        world.getAgentPositionMap().put(entity.getID(), human.getPosition());
//                        System.out.println("time:"+world.getTime() + " me:"+world.getSelf().getID()+" BAD Thinking, it was free:"+entity.getID());
                    }
                }

                if (!human.getPosition().equals(world.getAgentFirstPositionMap().get(human.getID()))) {
                    world.getBuriedAgents().remove(entity.getID());
                }


            }
        }


    }

    private void updateUnreachablePositions() {

        PathPlanner pp = (PathPlanner) pathPlanner;
        EntityID positionID = null;
        if (pp.escapeHere.getTryCount() > 2) {

            positionID = pp.getPreviousTarget();
            if (positionID != null) {
                int postponeTime = random.nextInt(6) + 15;

                unReachablePositionTime.put(positionID, postponeTime);
                if (!unReachablePositions.contains(world.getEntity(positionID))) {
                    unReachablePositions.add(world.getEntity(positionID));
                }

            }
        }

        ArrayList<StandardEntity> toRemove = new ArrayList<StandardEntity>();
        int postponeTime = 0;
        for (StandardEntity standardEntity : unReachablePositions) {

            postponeTime = unReachablePositionTime.get(standardEntity.getID());
            postponeTime--;
            if (postponeTime <= 0) {
                toRemove.add(standardEntity);
                unReachablePositionTime.remove(standardEntity.getID());

            } else {
                unReachablePositionTime.put(standardEntity.getID(), postponeTime);

            }

        }
        unReachablePositions.removeAll(toRemove);


    }

    public void postAftershockAction() {
        world.printData("New aftershock occurred! Time: " + world.getTime() + " Total: " + world.getAftershockCount());

        for (MrlRoad mrlRoad : world.getMrlRoads()) {
            mrlRoad.getParent().undefineBlockades();
        }
    }

    public abstract void act() throws CommandException, TimeOutException;

    public abstract void processMessage(MessageEntity messageEntity);

    public boolean isThinkTimeOver(String s) throws TimeOutException {
        if (!LaunchMRL.DEBUG_MODE && ((System.currentTimeMillis() - world.getThinkStartTime_()) > world.getThinkTimeThreshold())) {
            throw new TimeOutException("  Timeout(" + world.getThinkTimeThreshold() + ") on: " + s);
        }
        return false;
    }

    private boolean shouldRestAtRefuge() {
        Human self = world.getSelfHuman();
        return (self.getBuriedness() > 0) || (self.getBuriedness() == 0
                && ((self.getDamage() > 5 && self.getHP() < 7000)
                || self.getDamage() > 20
                || (self.getDamage() > 0 && self.getHP() < 5000)));
    }

    private void scanChannel(Collection<Command> commands) {
        if (world.getTime() < world.getIgnoreCommandTime()) {
            return;
        }
        //messageManager.scanChannels(commands);
    }

    public boolean isMovingRendezvous() {
        return movingRendezvous;
    }

    public void setMovingRendezvous(boolean movingRendezvous) {
        this.movingRendezvous = movingRendezvous;
    }

    public void setIAmOnRendezvousPlace(boolean onRendezvous) {
        this.onRendezvous = onRendezvous;
    }

    public Random getRandom() {
        return random;
    }

    public boolean isStuck() {
        return stuckChecker.isStuck();
    }

    public StuckState getStuckState() {
        return stuckChecker.getStuckState();
    }

    public AverageTools getAverageTools() {
        return averageTools;
    }

    public void setHardWalking(boolean hardWalking) {
        isHardWalking = hardWalking;
    }

    public boolean isHardWalking() {
        return isHardWalking;
    }

    StandardEntity loopTarget;

    public void loopBetweenTwoArea(StandardEntity area1, StandardEntity area2) throws CommandException {
        if (world.getSelfPosition().getID().equals(area2.getID()) || loopTarget == null) {
            loopTarget = area1;
        } else if (world.getSelfPosition().getID().equals(area1.getID())) {
            loopTarget = area2;
        }
        move((Area) loopTarget, 0, true);
    }

    Pair<Integer, Integer> loopTargetPoint;

    public void loopBetweenTwoPoint(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) throws CommandException {
        if (world.getSelfLocation().equals(p2) || loopTargetPoint == null) {
            loopTargetPoint = p1;
        } else if (world.getSelfLocation().equals(p1)) {
            loopTargetPoint = p2;
        }
        moveToPoint(world.getSelfPosition().getID(), loopTargetPoint.first(), loopTargetPoint.second());
    }

    public Set<StandardEntity> getUnReachablePositions() {
        return unReachablePositions;
    }

    public boolean randomWalk() {

        int randomRoadIndex = random.nextInt(roads.size() - 1);
        List<EntityID> plan = pathPlanner.planMove((Area) world.getSelfPosition(), (Area) roads.get(randomRoadIndex), IN_TARGET, true);
        sendMove(world.getTime(), plan);
        return false;
    }

    public boolean isDead() {
        return world.getSelfHuman().isHPDefined() && world.getSelfHuman().getHP() == 0;
    }

    protected boolean isTimeout(){
        return timeout;
    }
}