package mrl.police.strategies;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.helper.RoadHelper;
import mrl.police.ClearHereHelper;
import mrl.police.MrlPoliceForceWorld;
import mrl.police.PoliceConditionChecker;
import mrl.police.clear.ClearActManager;
import mrl.police.moa.PoliceForceUtilities;
import mrl.task.PoliceActionStyle;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 11:07 PM
 */
public class LegacyActionStrategy extends DefaultActionStrategy {

    protected ClearHereHelper clearHereHelper;
    private MrlPoliceForceWorld policeForceWorld;
    protected Path targetPath;


    public LegacyActionStrategy(MrlWorld world, ClearActManager clearActManager, PoliceMessageHelper policeMessageHelper, PoliceForceUtilities utilities, PoliceConditionChecker conditionChecker) {
        super(world, clearActManager, policeMessageHelper, utilities, conditionChecker);

        clearHereHelper = new ClearHereHelper(world, policeMessageHelper);//old clear helper version
        this.policeForceWorld = (MrlPoliceForceWorld) super.world;
    }

    @Override
    public void execute() throws CommandException, TimeOutException {
  /*      //        clearHere(CLEAR_HERE_STRATEGY);
//        loopBetweenTwoArea(world.getEntity(new EntityID(32749)), world.getEntity(new EntityID(1221)));


        policeForceWorld.getDecision().update();
        me.isThinkTimeOver("world.getDecision().update()");
//        updateUnexploredBuildings(changed);

        if ((targetPath == null && world.getTime() > 6) || world.getTime() % 10 == 0) {
            if (world.getTime() % 10 == 0) {
                policeForceWorld.getDecision().getTaskAssignment().clear();
            }
            targetPath = policeForceWorld.getDecision().assign();
            if (targetPath != null) {
                if (MRLConstants.DEBUG_POLICE_FORCE) {
                    world.printData(" Target Selected  " + "  targetPath: " + targetPath + "  value: " + targetPath.getValue());
                }
            }
        }
        me.isThinkTimeOver("assign");

        clearHere(MRLConstants.CLEAR_HERE_STRATEGY);
        me.isThinkTimeOver("clearHere");


//        rendezvousAction();

        if (targetPath != null) {
            selectRoadToMove();
        }
        me.isThinkTimeOver("selectRoadToMove");

        if (MRLConstants.DEBUG_POLICE_FORCE) {
            world.printData(" SEARCHING ....");
        }

        search();

        //searchZones();
        me.isThinkTimeOver("searchZones");

        if (MRLConstants.DEBUG_POLICE_FORCE) {
            world.printData(" REST AT REFUGE.");
        }
        me.restAtRefuge();
        me.isThinkTimeOver("restAtRefuge");*/
    }

    /**
     * This method handles works that should be done after each  aftershock
     */
    @Override
    public void doAftershockWork() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public void clearHere(PoliceActionStyle clearType) throws CommandException {
        clearHereHelper.clear(targetRoad, clearType);
    }

    private void selectRoadToMove() throws CommandException {
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);

        if (targetRoad != null && roadHelper.isPassable(targetRoad.getID())) {
            if (MRLConstants.DEBUG_POLICE_FORCE) {
                world.printData(" target road is passable :" + targetRoad);
            }
            targetRoad = null;
        }

        if (targetPath.isEmpty()) {
            //System.out.println("self: "+world.getSelfHuman().getID()+"  time: "+world.getTime()+"  --> Error: Target Path is NULL !!!");
            return;
        }

        if (targetRoad == null) {
            //System.out.println("self: "+world.getSelfHuman().getID()+"  time: "+world.getTime()+"  value: "+targetPath.getValue()+"  targetPath: " + targetPath+"\n"+"  getImportantRoads: "+targetPath.getImportantRoads());
            for (Road road : targetPath.getImportantRoads()) {
                if (!roadHelper.isPassable(road.getID())) {
                    targetRoad = road;
                    break;
                }
            }
            if (targetRoad == null) {
                for (Road road : targetPath) {
                    if (!roadHelper.isPassable(road.getID())) {
                        targetRoad = road;
                        break;
                    }
                }
            }
            if (MRLConstants.DEBUG_POLICE_FORCE) {
                world.printData("  targetRoad: " + targetRoad);
            }
        }
        if (targetRoad == null) {
            //System.out.println("self: "+world.getSelfHuman().getID()+"  time: "+world.getTime()+"  --> Error: Target Road is NULL !!!");
//            world.printData(" road in path set null :"+targetPath);
            targetPath = null;
            return;
        }

        if (!selfHuman.getPosition().equals(targetRoad.getID()) && !roadHelper.isPassable(targetRoad.getID())) {
//            if (DEBUG_POLICE_FORCE) {
//                System.out.println("         self: " + world.getSelfHuman().getID() + "  time: " + world.getTime() + "  keep moving to " + targetRoad.getID().getValue());
//            }
            if (MRLConstants.DEBUG_POLICE_FORCE) {
                world.printData(" move to target road : " + targetRoad);
            }
            me.move(targetRoad, MRLConstants.IN_TARGET, false);
        }
        if (selfHuman.getPosition().equals(targetRoad.getID())) {
//            System.out.println(targetPath.getTargetsInThisPath());
            for (Area area : targetPath.getImportantRoads()) {
                if (area.getNeighbours().contains(targetRoad.getID())) {
                    if (MRLConstants.DEBUG_POLICE_FORCE) {
                        System.out.println(me.getDebugString() + " MOVE TO TARGET : " + area);
                    }
                    me.move(area, MRLConstants.IN_TARGET, false);
                }
            }
        }
    }

}
