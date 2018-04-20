package mrl.firebrigade;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.common.clustering.ConvexObject;
import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.helper.FireBrigadeMessageHelper;
import mrl.firebrigade.sterategy.*;
import mrl.firebrigade.tools.ProcessExtinguishAreas;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.genericsearch.*;
import mrl.world.MrlWorld;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.properties.IntProperty;

import java.util.Collection;
import java.util.EnumSet;

/**
 * MRL fire brigade agent.
 */
public class MrlFireBrigade extends MrlPlatoonAgent<FireBrigade> {

    public static final String FIRE_BRIGADE_COUNT_EXTENSION = ".fbc";
    protected MrlFireBrigadeWorld world;
    private FireBrigadeMessageHelper fireBrigadeMessageHelper;


//    public FBCLStrategy getFBCLStrategy() {
//        return FBCLStrategy;
//    }

    //    protected MrlZone targetMrlZone;
//    private FireBrigadeDecisionManager decisionManager;
    private MrlFireBrigadeDirectionManager fireBrigadeDirectionManager;
    private ConvexObject convexObject;
    //    private FBCLStrategy FBCLStrategy;
    public BurningBuildingSearchManager fireSearcher;
    @SuppressWarnings("FieldCanBeLocal")
    private BurningBuildingSearchDecisionMaker decisionMaker;

    private IFireBrigadeActionStrategy actionStrategy;
    private DefaultFireBrigadeActionStrategy defaultFireBrigadeActionStrategy;
    private SimpleFireBrigadeActionStrategy simpleFireBrigadeActionStrategy;
    private StuckFireBrigadeActionStrategy stuckFireBrigadeActionStrategy;
    private FBLegacyActionStrategy legacyActionStrategy;
    private FireBrigadeActionStrategyType actionStrategyType;
    private FireBrigadeUtilities fireBrigadeUtilities;

    public MrlFireBrigade() {
        actionStrategyType = FireBrigadeActionStrategyType.DEFAULT;
    }


    @Override
    public String toString() {
        return "MRL fire brigade ID: " + this.getID().getValue();
    }

    @Override
    protected void postConnect() {
        long startTime = System.currentTimeMillis();
        super.postConnect();
        this.world = (MrlFireBrigadeWorld) super.world;
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE);

        setFireBrigadeMessageHelper(new FireBrigadeMessageHelper(world, this, messageManager));
//        directionManager = new DirectionManager(world, maxWater);
//        this.decisionManager = new FireBrigadeDecisionManager(world, directionManager);
        this.fireBrigadeDirectionManager = new MrlFireBrigadeDirectionManager(world);

//        FBCLStrategy = new FBCLStrategy(world, this, pathPlanner);

        //--------------SEARCH INITIATION-------------------

        decisionMaker = new BurningBuildingSearchDecisionMaker(world);
//        possibleBuildingSearchDecisionMaker = new PossibleBuildingSearchDecisionMaker(world);
        stupidSearchDecisionMaker = new StupidSearchDecisionMaker(world);
        exploreAroundFireDecisionMaker = new ExploreAroundFireDecisionMaker(world);
        legacyHeatTracerDecisionMaker = new LegacyHeatTracerDecisionMaker(world);
        heatTracerDecisionMaker = new HeatTracerDecisionMaker(world);
        fireSearcher = new BurningBuildingSearchManager(world, world.getPlatoonAgent(), decisionMaker, senseSearchStrategy);

//        possibleBuildingSearchManager = new PossibleBuildingSearchManager(world, this, possibleBuildingSearchDecisionMaker, senseSearchStrategy);
        stupidSearchManager = new StupidSearchManager(world, this, stupidSearchDecisionMaker, senseSearchStrategy);
        simpleSearchDecisionMaker = new SimpleSearchDecisionMaker(world);
        simpleSearchManager = new SimpleSearchManager(world, this, simpleSearchDecisionMaker, senseSearchStrategy);
//        civilianSearchBBDecisionMaker = new CivilianSearchBBDecisionMaker(world);
//        civilianSearchManager = new CivilianSearchManager(world, this, civilianSearchBBDecisionMaker, civilianSearchStrategy);
        exploreAroundFireSearchManager = new ExploreAroundFireSearchManager(world, this, exploreAroundFireDecisionMaker, senseSearchStrategy);
        heatTracerSearchManager = new HeatTracerSearchManager(world, this, heatTracerDecisionMaker, senseSearchStrategy);
        defaultSearchManager = new DefaultSearchManager(world, this, stupidSearchDecisionMaker, senseSearchStrategy);
        //--------------SEARCH INITIATION-------------------
        //--------------Border Initiation------------------
        world.setBorderBuildings();
        //--------------Border Initiation------------------

        //============Mostafa PreProcesses=================
        fireBrigadeUtilities = new FireBrigadeUtilities(world);

//        ProcessAdvantageRatio processAdvantageRatio;
//        processAdvantageRatio = new ProcessAdvantageRatio(world, config);
//        processAdvantageRatio.process();
        ProcessExtinguishAreas processExtinguishAreas;
        processExtinguishAreas = new ProcessExtinguishAreas(world);
        processExtinguishAreas.process();

        //=================================================

        defaultFireBrigadeActionStrategy = new DefaultFireBrigadeActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
        simpleFireBrigadeActionStrategy = new SimpleFireBrigadeActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
        stuckFireBrigadeActionStrategy = new StuckFireBrigadeActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
        legacyActionStrategy = new FBLegacyActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);

        storeNumberOfAgents(FIRE_BRIGADE_COUNT_EXTENSION, world.getFireBrigadeList().size());

//        chooseActionStrategy();
        long endTime = System.currentTimeMillis();
        while(true){
            if(!isTimeout() && !world.isPrecomputedDataLoaded()){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                break;
            }
        }
        System.out.println(this + "  connected ---->   total(" + (endTime - startTime) + "ms)");
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {

        super.think(time, changed, heard);
    }

    @Override
    public void processMessage(MessageEntity messageEntity) {
        getFireBrigadeMessageHelper().processMessage(messageEntity);
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
    }

    @Override
    public void act() throws CommandException, TimeOutException {
        chooseActionStrategy();
        actionStrategy.execute();
    }

    private void chooseActionStrategy() {
        if (world.getPlatoonAgent().isStuck()) {
            actionStrategyType = FireBrigadeActionStrategyType.STUCK_SITUATION;
        } else {
            if (world.isPrecomputedDataLoaded()) {
                actionStrategyType = FireBrigadeActionStrategyType.DEFAULT;
//                System.out.println("time: " + world.getTime() + " " + this.getID() + " <<<DEFAULT>>> action strategy");
            } else {
                actionStrategyType = FireBrigadeActionStrategyType.SIMPLE;
//                System.out.println("time: " + world.getTime() + " " + this.getID() + " >>>simple<<< action strategy");
            }
        }

        switch (actionStrategyType) {
            case LEGACY:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.LEGACY)) {
                    actionStrategy = legacyActionStrategy;
                }
                break;
//            case FIRE_CLUSTER_CONDITION_BASED:
//                if (shouldBeRenewed(FireBrigadeActionStrategyType.FIRE_CLUSTER_CONDITION_BASED)) {
//                    actionStrategy = new FireClusterConditionBasedActionStrategy(world, fireBrigadeUtilities, fireBrigadeDirectionManager);
//                }
//                break;
            case DEFAULT:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.DEFAULT)) {
                    actionStrategy = defaultFireBrigadeActionStrategy;
                }
                break;
            case SIMPLE:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.SIMPLE)) {
                    actionStrategy = simpleFireBrigadeActionStrategy;
                }
                break;
            case STUCK_SITUATION:
                if (shouldBeRenewed(FireBrigadeActionStrategyType.STUCK_SITUATION)) {
                    actionStrategy = stuckFireBrigadeActionStrategy;
                }
                break;
        }
    }

    private boolean shouldBeRenewed(FireBrigadeActionStrategyType actionStrategyType) {
        return this.actionStrategy == null || !this.actionStrategy.getType().equals(actionStrategyType);
    }

    public ConvexObject getConvexObject() {
        return convexObject;
    }

    public MrlFireBrigadeDirectionManager getFireBrigadeDirectionManager() {
        return fireBrigadeDirectionManager;
    }

    public int getWater() {
        return me().getWater();
    }

    @SuppressWarnings("UnusedDeclaration")
    public IntProperty getWaterProperty() {
        return me().getWaterProperty();
    }

    public Boolean isWaterDefined() {
        return me().isWaterDefined();
    }

    public void setWater(int water) {
        me().setWater(water);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void undefinedWater() {
        me().undefineWater();
    }

    public EntityID getPosition() {
        return me().getPosition();
    }

    public StandardEntity getPosition(MrlWorld world) {
        return me().getPosition(world);
    }

    public void setConvexObject(ConvexObject convexObject) {
        this.convexObject = convexObject;
    }


    public FireBrigadeMessageHelper getFireBrigadeMessageHelper() {
        return fireBrigadeMessageHelper;
    }

    public FireBrigadeUtilities getFireBrigadeUtilities() {
        return fireBrigadeUtilities;
    }

    public void setFireBrigadeMessageHelper(FireBrigadeMessageHelper fireBrigadeMessageHelper) {
        this.fireBrigadeMessageHelper = fireBrigadeMessageHelper;
    }
}