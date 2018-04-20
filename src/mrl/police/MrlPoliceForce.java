package mrl.police;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.common.comparator.ConstantComparators;
import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.partitioning.IPartitionManager;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.search.SearchHelper;
import mrl.police.clear.ClearActManager;
import mrl.police.moa.PoliceForceUtilities;
import mrl.police.strategies.*;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;


/**
 * MRL police force agent.
 * <p/>
 * Since 1.1: Added multiple Operation Mode Support
 *
 * @author Legacy
 * @author BrainX
 * @version 1.1
 */
public class MrlPoliceForce extends MrlPlatoonAgent<PoliceForce> {
    public static final String POLICE_FORCE_COUNT_EXTENSION = ".pfc";
    protected MrlPoliceForceWorld world;
    protected ClearHereHelper clearHereHelper;
    protected PoliceMessageHelper policeMessageHelper;
    private Collection<EntityID> unexploredBuildings;


    private PoliceForceUtilities utilities;
    private PoliceConditionChecker pcc;
    //    List<IBehavior> myBehaviors;
//    private AbstractBehaviorHelper policeBehaviorsHelper;
    private ActionStrategyType actionStrategyType;
    private IPoliceActionStrategy actionStrategy;

    //    private ClearHelper clearHelper;
    private ClearActManager clearActManager;
    private IPartitionManager targetsPartitionManager;
    private SearchHelper searchHelper;


    @Override
    public String toString() {
        return "MRL police force ID: " + this.getID().getValue();
    }

    @Override
    protected void postConnect() {
        long startTime = System.currentTimeMillis();
        super.postConnect();
        this.world = (MrlPoliceForceWorld) super.world;
        model.indexClass(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
//        clearHelper = new ClearHelper(world);
        clearActManager = new ClearActManager(world);
        policeMessageHelper = new PoliceMessageHelper(world, this, messageManager);
        Collections.sort(world.getPoliceForceList(), ConstantComparators.ID_COMPARATOR);
        actionStrategyType = ActionStrategyType.TARGET_CLUSTERING;

//        unexploredBuildings = new HashSet<EntityID>(getMyPartition().getBuildingsID());

        utilities = new PoliceForceUtilities(world, config);

        pcc = new PoliceConditionChecker(world, utilities, policeMessageHelper);
//        behaviorHelper = new PoliceBehaviorsHelper(world);
//        myBehaviors = behaviorHelper.getBehaviors();
//        policeBehaviorsHelper = new PoliceBehaviorsHelper(world, this, config, policeMessageHelper);


        utilities.updateHealthyPoliceForceList();


        chooseActionStrategy();


        //TODO: This method was for making clusters for important targets of policeForces in firs Cycles
//        initializeTargetsPartitionManager();

        searchHelper = new SearchHelper(world, world.getPlatoonAgent());
        storeNumberOfAgents(POLICE_FORCE_COUNT_EXTENSION, world.getPoliceForceList().size());

        long endTime = System.currentTimeMillis();
        System.out.println(this + "  connected ---->   total(" + (endTime - startTime) + "ms)");
    }

    private void chooseActionStrategy() {
        switch (actionStrategyType) {
            case PRIORITIZATION:
                actionStrategy = new TaskPrioritizationActionStrategy(world, clearActManager, policeMessageHelper, utilities, pcc);
                break;
            case TARGET_CLUSTERING:
                actionStrategy = new PoliceTargetClusteringActionStrategy(world, clearActManager, policeMessageHelper, utilities, pcc);
                break;
            case AUCTIONING:
                actionStrategy = new AuctionBasedActionStrategy(world, clearActManager, policeMessageHelper, utilities, pcc);
                break;
            case LEGACY:
                actionStrategy = new LegacyActionStrategy(world, clearActManager, policeMessageHelper, utilities, pcc);
                break;
        }

    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        super.think(time, changed, heard);
    }

    @Override
    public void act() throws CommandException, TimeOutException {
        actionStrategy.execute();
    }


    @Override
    public void postAftershockAction() {
        super.postAftershockAction();
        actionStrategy.doAftershockWork();
//        world.printData("Last aftershock time = " + world.getLastAfterShockTime() + " Total: " + world.getAftershockCount());
    }

    @Override
    public void processMessage(MessageEntity messageEntity) {
        try {
            policeMessageHelper.processMessage(messageEntity);
        } catch (Exception ex) {
            //todo: Handle it
            ex.printStackTrace();
        }

    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_FORCE);
    }

    public ClearActManager getClearActManager() {
        return clearActManager;
    }
}