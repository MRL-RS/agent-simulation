package mrl.ambulance.marketLearnerStrategy;

import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.common.MRLConstants;
import mrl.common.comparator.ConstantComparators;
import mrl.helper.HumanHelper;
import mrl.world.routing.pathPlanner.IPathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 27, 2010
 * Time: 7:31:01 PM
 */
public class AmbulanceConditionChecker {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(AmbulanceConditionChecker.class);

    MrlAmbulanceTeamWorld world;
    private AmbulanceTeam ambulanceTeam;
    private AmbulanceUtilities ambulanceUtilities;
    private int time;
    private int numberOfCoops = 0;
    private int myTargetLastBuriedness = 0;
    private IPathPlanner pathPlanner;


    public AmbulanceConditionChecker(MrlAmbulanceTeamWorld world, AmbulanceTeam ambulanceTeam) {
        this.world = world;
        this.ambulanceTeam = ambulanceTeam;
        this.ambulanceUtilities = new AmbulanceUtilities(world);
        this.pathPlanner = world.getPlatoonAgent().getPathPlanner();
    }


    //////////////////     Getters & Setters
    ////////////////////////////////////////

    public void setTime(int time) {
        this.time = time;
    }

    public int getMyTargetLastBuriedness() {
        return myTargetLastBuriedness;
    }

    public void setMyTargetLastBuriedness(int myTargetLastBuriedness) {
        this.myTargetLastBuriedness = myTargetLastBuriedness;
    }

    //////////////////     Public Functions
    ///////////////////////////////////////

    public boolean loadCondition(Human human) {
        return isCivilian(human)
                && world.isVisible(human)
                && checkCivilianPosition(human)
                && checkSelfPosition()
                && human.getBuriedness() == 0
                && checkSelfBuriedness()
                && isAlive(human)
                && !isReleaseCondition(human.getID(), world.getSelfPosition())
//                && amILoader(human.getID())
                && isLoader(human)
                ;

    }

    public boolean loadCondition_messageBased(Human human) {
        return isCivilian(human)
                && world.isVisible(human)
                && checkCivilianPosition(human)
                && checkSelfPosition()
                && human.getBuriedness() == 0
                && checkSelfBuriedness()
                && isAlive(human)
                && !isReleaseCondition(human.getID(), world.getSelfPosition())
//                && amILoader(human.getID())
                && isLoader_MessageBased((Civilian) human)
                ;

    }

    public boolean isLoader_MessageBased(Civilian civilian) {
        if (civilian == null) {
            return false;
        }

        for (Pair<EntityID, EntityID> pair : world.getLoaders()) {
            if (pair.second().equals(civilian.getID())) {
                if (pair.first().getValue() < world.getSelf().getID().getValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkCivilianPosition(Human human) {
        return (world.getEntity(human.getPosition()) instanceof Area)
                && ambulanceTeam.getPosition().equals(human.getPosition());
    }

    public boolean isLoader(Human human) {
//        return isLoader;
        ArrayList<AmbulanceTeam> availableAmbulanceTeams = new ArrayList<AmbulanceTeam>(); //3289

        for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
            AmbulanceTeam at = (AmbulanceTeam) standardEntity;
            if (at.getPosition().equals(world.getSelfPosition().getID())) {
                availableAmbulanceTeams.add(at);
            }
        }

        //todo use search instead of sort if there is any smaller id, so I am not loader
        Collections.sort(availableAmbulanceTeams, ConstantComparators.ID_COMPARATOR);

        return availableAmbulanceTeams.indexOf(ambulanceTeam) == 0 ||
                world.getHelper(HumanHelper.class).getNumberOfATsRescuing(human.getID()) == 1;
    }

    private boolean amILoader(EntityID humanID) {

        Pair<Integer, Integer> myStartCurrentTimePair =
                world.getAmbulanceCivilianMap().get(new Pair<EntityID, EntityID>(ambulanceTeam.getID(), humanID));

        ArrayList<EntityID> possibleLoders = new ArrayList<EntityID>();
        ArrayList<StandardEntity> availableAmbulanceTeams = new ArrayList<StandardEntity>(); //3289

        for (StandardEntity at : world.getAmbulanceTeams()) {
            if (isVisible(at)) {
                availableAmbulanceTeams.add(at);
            }
        }

        for (StandardEntity a : availableAmbulanceTeams) {
            Pair<Integer, Integer> startCurrentTimePair =
                    world.getAmbulanceCivilianMap().get(new Pair<EntityID, EntityID>(a.getID(), humanID));

            if (startCurrentTimePair == null) {
                System.out.println(time + " startCurrentTimePair==null " + a.getID() + "  " + humanID);
            }
            if (startCurrentTimePair != null &&
                    (world.getTime() - startCurrentTimePair.second() < 3) && (a.getID().getValue() < ambulanceTeam.getID().getValue()))
                return false;
        }

//            if (startCurrentTimePair != null && startCurrentTimePair.second() == world.getTime() - 1 &&
//                    (world.getTime()-startCurrentTimePair.first())>1) {
//                if (a.getID().getValue() < ambulanceTeam.getID().getValue()) {
//                    return false;
//                }
//            }

//            if(startCurrentTimePair.first()<myStartCurrentTimePair.first())
//                return false;
//            else if(startCurrentTimePair.first().equals(myStartCurrentTimePair.first()))
//            {
//                possibleLoders.add();
//            }
//
//        }
//
//        for(Integer i:possibleLoders)
//        {
//             if(i)
//        }
        return true;
    }

    public boolean rescueCondition(Human human) {
        return human.isBuriednessDefined()
                && human.getBuriedness() > 0
                && checkSelfBuriedness()
                && (needToBeHere(human)/*|| world.getLoaders().isEmpty()*/)
                && isAlive(human)
                && isVisible(human); //todo Why?
    }

    public Civilian someoneOnBoard() {
//        for (StandardEntity next : world.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
//            if (((Human) next).getPosition().equals(ambulanceTeam.getID())) {
//                Logger.debug(next + " is on board");
//                return true;
//            }
//        }
//        return false;
        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) standardEntity;
            if (civilian.isPositionDefined() && civilian.getPosition().equals(world.getSelf().getID())) {
                Logger.debug(civilian + " is on board");
                if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
                    System.out.println(world.getSelf().getID() + " " + civilian + " is on board");
                }
                return civilian;
            }
        }
        return null;

    }

    public boolean needToGoRefuge() {
        return ((ambulanceTeam.getHP() < 5000 && ambulanceTeam.getDamage() > 4) || ambulanceTeam.getDamage() >= 300);
    }


    //////////////////     Private Functions
    /////////////////////////////////////////

    public boolean isVisible(StandardEntity human) {
        return world.isVisible(human);
    }

    public boolean isVisible(EntityID humanId) {
        return world.isVisible(humanId);
    }

    private boolean isCivilian(Human human) {
        return human instanceof Civilian;
    }

    public boolean needToBeHere(Human human) {

        int timeToRescue = 0;

        ArrayList<StandardEntity> visibleAmbulanceTeams = new ArrayList<StandardEntity>();

//        numberOfCoops = myTargetLastBuriedness - human.getBuriedness();
//        myTargetLastBuriedness = human.getBuriedness();
        int numberOfCoops = world.getHelper(HumanHelper.class).getNumberOfATsRescuing(human.getID());

        if (world.getHelper(HumanHelper.class).getNumberOfATsRescuing(human.getID()) <= 1) {
            return true;
        }
        if (numberOfCoops <= 0) {
            numberOfCoops = 1;
        }
        Human h;
        for (StandardEntity at : world.getAmbulanceTeams()) {
            h = (Human) at;
            if (isInTheSamePlace(at)/* isVisible(at)*/ && h.isBuriednessDefined() && h.getBuriedness() == 0) {
                visibleAmbulanceTeams.add(at);
            }
        }
//
        Collections.sort(visibleAmbulanceTeams, ConstantComparators.ID_COMPARATOR);
//        timeToRescue = human.getBuriedness() / numberOfCoops;

//        return visibleAmbulanceTeams.indexOf(ambulanceTeam) <= timeToRescue;
        return visibleAmbulanceTeams.indexOf(ambulanceTeam) < human.getBuriedness() || visibleAmbulanceTeams.indexOf(ambulanceTeam) == 0;
    }

    private boolean isInTheSamePlace(StandardEntity at) {

        return ((Human) at).getPosition().equals(world.getSelfPosition().getID());
    }


    private boolean checkSelfPosition() {
        try {
            return !(ambulanceTeam.getPosition(world) instanceof Refuge) && ambulanceTeam.getPosition(world) instanceof Area;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return false;
        }

    }

    private boolean checkSelfBuriedness() {
        try {
            return ambulanceTeam.getBuriedness() == 0;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean isAlive(Human human) {
        return human.getHP() > 0;
    }


    public boolean isArrivedTo(Civilian shouldRescueCivilian) {
// todo  is it enough for starting rescue act or it is needed to be on civilian's x&y
        return ambulanceTeam.getPosition().equals(shouldRescueCivilian.getPosition());

    }


    /**
     * is it time to release the healty loaded civilian in a simple road
     *
     * @param civToRefugeId the healty loaded civilian by me
     * @param selfPosition  my position
     * @return time to release healty loaded civilian
     */
    public boolean isReleaseCondition(EntityID civToRefugeId, StandardEntity selfPosition) {
        Human civToRefuge = (Human) world.getEntity(civToRefugeId);
        if (world.getHelper(HumanHelper.class).getFirstBuriedness(civToRefugeId) <= 0 && ((civToRefuge.getDamage() == 0 && civToRefuge.getHP() == 10000) || world.getRefuges().isEmpty()) && amIInASimpleRoad(selfPosition)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Am I in a road which is not an entrance?
     *
     * @param selfPosition my position
     * @return true if ia am in a simple road
     */
    private boolean amIInASimpleRoad(StandardEntity selfPosition) {
        if (selfPosition instanceof Road) {
            Road road = (Road) selfPosition;
            for (EntityID entityID : road.getNeighbours()) {
                if (world.getEntity(entityID) instanceof Building) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    public boolean isPassable(EntityID firstPosition, EntityID secondPosition) {

        if (firstPosition.equals(secondPosition)) {
            return true;
        }

        Area source = world.getEntity(firstPosition, Area.class);
        Area goal = world.getEntity(secondPosition, Area.class);
        List<EntityID> path;

        path = pathPlanner.planMove(source, goal, 0, false);

        if (path == null || path.isEmpty()) {
            return false;
        } else {
            return true;

        }

    }
}
