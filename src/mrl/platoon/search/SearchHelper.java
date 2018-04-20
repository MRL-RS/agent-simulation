package mrl.platoon.search;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.partition.Partition;
import mrl.partition.Partitionable;
import mrl.partition.RendezvousConstants;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.simpleSearch.BreadthFirstSearch;
import mrl.platoon.simpleSearch.DistanceInterface;
import mrl.platoon.simpleSearch.Graph;
import mrl.platoon.simpleSearch.SearchAlgorithm;
import mrl.world.MrlWorld;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 20, 2010
 * Time: 5:26:20 PM
 */
public class SearchHelper implements ISearchHelper, MRLConstants {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(SearchHelper.class);

    protected MrlWorld world;
    protected MrlPlatoonAgent agent;
    protected ISearchMethod searchMethod;
    protected SearchingZoneDeciderI searchingZoneDecider;
    protected Random random;
    private int zonesSize;

    // check and report civilian
    private Queue<Civilian> shouldCheckCiviliansQueue = new LinkedList<Civilian>();
    private ArrayList<Civilian> oneTimeCheckedCivilians = new ArrayList<Civilian>();

    /**
     * the connectivity graph of all places in the world
     */
    protected Graph connectivityGraph;
    /**
     * a matrix containing the pre-computed distances between each two areas in the world
     */
    protected DistanceInterface distanceMatrix;
    /**
     * The search algorithm.
     */
    protected SearchAlgorithm search;
    private List<EntityID> unexploredBuildings;
    private mrl.partitioning.Partition targetPartition;


    public SearchHelper(MrlWorld world, MrlPlatoonAgent agent) {
        this.world = world;
        this.agent = agent;
        random = agent.getRandom();
        searchMethod = new SearchMethod(world);
        searchingZoneDecider = new LinearSearchingZoneDecider(world);

        mrl.partitioning.Partition myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
        targetPartition = myPartition;
        if (myPartition != null) {
            unexploredBuildings = new ArrayList<EntityID>(myPartition.getBuildingIDs());

        } else {
            unexploredBuildings = new ArrayList<EntityID>(world.getUnvisitedBuildings());

        }
        // load correct search algorithm
        search = new BreadthFirstSearch();
        connectivityGraph = new Graph(world);
        distanceMatrix = new DistanceInterface(world);
//        }


        rnd = new Random((345 * System.currentTimeMillis()) + world.getSelf().getID().getValue());
//        zonesSize = world.getZones().size();
    }

    @Override
    public void zoneSearch() throws CommandException {
//        searchUnvisitedZoneStrategy();
    }

    @Override
    public void breadthFirstSearch(boolean inPartition) throws CommandException {

        searching_BreadthFirst(inPartition);
    }

    @Override
    public List<EntityID> listBasedBreadthFirstSearch(boolean inPartition) throws CommandException {
        List<EntityID> path;

        if (shuffle) {
//            Collections.shuffle(world.getUnvisitedBuildings());
            shuffle = false;
        }


        if (inPartition) {
            updateUnvisitedBuildings();


            path = search.search(world.getSelfPosition().getID(), unexploredBuildings, connectivityGraph, distanceMatrix);

        } else {
            // Nothing to do
            updateUnvisitedBuildings();
            path = search.search(world.getSelfPosition().getID(), world.getUnvisitedBuildings(), connectivityGraph, distanceMatrix);
        }

        if (path != null) {
            Logger.info("Searching buildings");
//            agent.move((Area) world.getEntity(path.get(path.size() - 1)), IN_TARGET, false);
//            addToUnreachableTargets(unReachableTargets, path.get(path.size() - 1));


        }
//        Logger.info("Moving randomly");
//        agent.sendRandomWalkAct(world.getTime(), randomWalk());

        return path;
    }

    // -------------------------- search zones ------------------------------

    public void searchUnvisitedZoneStrategy() throws CommandException {
        MrlZone zone;
        for (int i = 0; i < zonesSize; i++) {
            zone = searchingZoneDecider.decideZone();
            if (zone == null) {
                return;
            }
            searchMethod.searchIn(zone, false, zone.getSearchValue() > 0);
        }
    }


    //------------------------- SIMPLE SEARCH ------------------------------------

    private List<EntityID> unReachableTargets = new ArrayList<EntityID>();
    private List<EntityID> prevPath = new ArrayList<EntityID>();
    protected Map<EntityID, Integer> unreachableTargetTime = new FastMap<EntityID, Integer>();
    private Random rnd;
    private boolean shuffle = true;

    private void searching_BreadthFirst(boolean inPartition) throws CommandException {
        List<EntityID> path;
//        updateUnexploredBuildings(world.getChanges());

//        if (world.isCommunicationLess()) {
//            if (MRLConstants.RENDEZVOUS_ACTION) {
//                rendezvousAction();
//            }
//        }

//
//        if(shuffle){
//            Collections.shuffle(world.getUnvisitedBuildings());
//            shuffle=false;
//        }


        if (inPartition) {
            if (isPartitionChanged()) {
                resetSearch();
            }
            updateUnvisitedBuildings();

            path = search.search(world.getSelfPosition().getID(), unexploredBuildings, connectivityGraph, distanceMatrix);

        } else {
            // Nothing to do
            updateUnvisitedBuildings();
            path = search.search(world.getSelfPosition().getID(), world.getUnvisitedBuildings(), connectivityGraph, distanceMatrix);
        }

        if (path != null) {
            Logger.info("Searching buildings");
//            if (!prevPath.equals(path)) {
//                prevPath.clear();
//                prevPath.addAll(path);
//            } else {
//                addToUnreachableTargets(unReachableTargets, path.get(path.size() - 1));
//            }
            agent.move((Area) world.getEntity(path.get(path.size() - 1)), IN_TARGET, false);
            addToUnreachableTargets(unReachableTargets, path.get(path.size() - 1));


        }
        Logger.info("Moving randomly");
        agent.sendRandomWalkAct(world.getTime(), randomWalk());

    }

    private boolean isPartitionChanged() {
        mrl.partitioning.Partition myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
        if (targetPartition == null || !targetPartition.equals(myPartition)) {
            targetPartition = myPartition;
            return true;
        }
        return false;
    }

    private void resetSearch() {
        if (targetPartition != null) {
            unexploredBuildings = new ArrayList<EntityID>(targetPartition.getBuildingIDs());
        }
    }

    // put a value between 5-10 to postpone considering of this human
    public void addToUnreachableTargets(List<EntityID> unReachableTargets, EntityID targetID) {
        int postponeTime = rnd.nextInt(6) + 5;
        unreachableTargetTime.put(targetID, postponeTime);
        if (!unReachableTargets.contains(targetID)) {
            unReachableTargets.add(targetID);
        }
        if (MRLConstants.DEBUG_AMBULANCE_TEAM)
            System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + ">>>>> PostPoned >>>> " + targetID + " " + postponeTime);
    }


    private void updateUnvisitedBuildings() {
        unexploredBuildings.removeAll(unReachableTargets);
        world.getUnvisitedBuildings().removeAll(unReachableTargets);
        world.getVisitedBuildings().addAll(unReachableTargets);

        unexploredBuildings.removeAll(world.getVisitedBuildings());
        List<EntityID> toRemove = new ArrayList<EntityID>();

        Building building;
        for (EntityID buildingId : unexploredBuildings) {
            building = (Building) world.getEntity(buildingId);
            if (building.isFierynessDefined()) {
                if (building.getFieryness() != 0 && building.getFieryness() != 4) {
                    //do nothing
                } else {
                    toRemove.add(buildingId);
                }
            }
        }

        unexploredBuildings.removeAll(toRemove);

//        for (MrlZone zone : world.getZones().getBurningZones()) {
//            unexploredBuildings.removeAll(zone);
//
//            for(MrlBuilding building : zone){
//                world.getUnvisitedBuildings().remove(building.getID());
////                world.getVisitedBuildings().add(building.getID());
//            }
//        }
//
//        for (MrlZone zone : world.getZones()) {
//            if (zone.isBurned()) {
//                for(MrlBuilding building : zone){
//                    world.getUnvisitedBuildings().remove(building.getID());
//                    world.getVisitedBuildings().add(building.getID());
//                    unexploredBuildings.remove(building.getID());
//                }
//            }
//        }

    }


    /**
     * Construct a random walk starting from this agent's current location to a random building.
     *
     * @return A random walk.
     */
    protected List<EntityID> randomWalk() {
        List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
        Set<EntityID> seen = new HashSet<EntityID>();
        EntityID current = world.getSelfPosition().getID();
        for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
            result.add(current);
            seen.add(current);
            List<EntityID> possible = new ArrayList<EntityID>(connectivityGraph.getNeighbors(current));
            Collections.shuffle(possible, random);
            boolean found = false;
            for (EntityID next : possible) {
                if (seen.contains(next)) {
                    continue;
                }
                current = next;
                found = true;
                break;
            }
            if (!found) {
                // We reached a dead-end.
                break;
            }
        }
        return result;
    }

    private void updateUnexploredBuildings(Set<EntityID> changed) {
        if (changed == null) {
            return;
        }
        for (EntityID next : changed) {
            unexploredBuildings.remove(next);
        }
    }

    // ---------------------------- RENDEZVOUS --------------------------------------------
    public void rendezvousAction() throws CommandException {

        if (!world.isCommunicationLess()) {
            return;
        }

        if (RENDEZVOUS_ACTION) {
            Partition myCurrentPartition = world.getPartitions().getPartition((Partitionable) world.getSelfPosition());
            if (myCurrentPartition == null) {
                System.err.println(world.getTime() + " " + world.getSelf().getID() + " my partition is null ");
                return;
            }
            if (myCurrentPartition.isNearRendezvousTime(world.getTime(), RendezvousConstants.rendezvousCheckPeriod)) {
                if (DEBUG_RENDEZVOUS_ACTION) {
                    System.out.println(world.getTime() + " " + agent.getID() + " >>>>>> time To Go Ren ");
                }
                int rendezvousToGoIndex = myCurrentPartition.getRendezvouseToGo(world.getTime(), RendezvousConstants.rendezvousCheckPeriod);
                List<Road> stayPlace = myCurrentPartition.getRendezvous().get(rendezvousToGoIndex).getRodes();
                for (Road road : stayPlace) {
                    if (world.getSelfPosition().getID().equals(road.getID())) {
                        agent.setMovingRendezvous(false);
                        agent.setIAmOnRendezvousPlace(true);
                        return;
                    }
                }
                if (DEBUG_RENDEZVOUS_ACTION) {
                    System.out.println(world.getTime() + " " + agent.getID() + " MOOOOOOVING TOOOOO " + stayPlace);
                }
                agent.setMovingRendezvous(true);
                agent.move((Area) stayPlace, IN_TARGET, false);
            }
            agent.setIAmOnRendezvousPlace(false);
        }
    }


    // -------------------------- check and report civilians ------------------------------

    public void stopNearCiviliansAndReport() throws CommandException {
        EntityID buildingToWaitOn = null;
        Set<EntityID> civilianBuildings;

        int numberOfPFAndFB = world.getPoliceForces().size() + world.getFireBrigades().size();
        civilianBuildings = findShouldCheckCivilianBuildings();

        if (civilianBuildings.size() <= numberOfPFAndFB / 2) {
            buildingToWaitOn = AssignProperBuildingToMe(civilianBuildings);
        } else {
            checkAndReportCivilians();
        }

        for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
            AmbulanceTeam aT = (AmbulanceTeam) standardEntity;
            if (world.getSelfPosition().equals(aT.getPosition(world))) {
                return;
            }
        }
        if (buildingToWaitOn != null) {
            if (!world.getSelfPosition().getID().equals(buildingToWaitOn)) {
                agent.move((Building) world.getEntity(buildingToWaitOn), IN_TARGET, true);
            } else {
                agent.sendRestAct(world.getTime());
            }
        }
    }

    private Set<EntityID> findShouldCheckCivilianBuildings() {

        Set<EntityID> civilianBuildings = new FastSet<EntityID>();

        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) standardEntity;
            if (civilian.isPositionDefined() && !(civilian.getPosition(world) instanceof Refuge)
                    && civilian.getHP() > 0 && civilian.getPosition(world) instanceof Building) {
                civilianBuildings.add(civilian.getPosition());
            }
        }
        return civilianBuildings;
    }

    protected void checkAndReportCivilians() throws CommandException {
        if (!world.getBurningBuildings().isEmpty()) {
            return;
        }

        findShouldCheckCivilians();
        if (!shouldCheckCiviliansQueue.isEmpty()) {
            Civilian civilian = shouldCheckCiviliansQueue.element();
            if (world.getSelfPosition().getID().equals(civilian.getPosition())) {
//                System.out.println("<<<<<<<<<<<<< me : " + me() + "   Arrived to :" + civilian.getPosition());
                shouldCheckCiviliansQueue.remove();
                List<Civilian> samePositionCivilians = new ArrayList<Civilian>();
                for (Civilian samePositionCivilian : shouldCheckCiviliansQueue) {
                    if (samePositionCivilian.getPosition().equals(civilian.getPosition())) {
                        samePositionCivilians.add(samePositionCivilian);
                    }
                }
                shouldCheckCiviliansQueue.removeAll(samePositionCivilians);
                shouldCheckCiviliansQueue.add(civilian);

                if (oneTimeCheckedCivilians.contains(civilian)) {
                    oneTimeCheckedCivilians.clear();
                    Collections.shuffle((List<?>) shouldCheckCiviliansQueue, random);
                } else {
                    oneTimeCheckedCivilians.add(civilian);
                }
            }

            civilian = shouldCheckCiviliansQueue.element();
            if (civilian != null && civilian.getPosition(world) instanceof Building) {
//                System.out.println("<-> me : " + me() + "   should go to :" + civilian.getPosition());
                agent.move((Building) world.getEntity(civilian.getPosition()), IN_TARGET, true);
            }
        }
    }

    private void findShouldCheckCivilians() {
        List<Civilian> civiliansToRemove = new ArrayList<Civilian>();
        for (Civilian civilian : shouldCheckCiviliansQueue) {
            if (!(civilian.isPositionDefined() && !(civilian.getPosition(world) instanceof Refuge)
                    && civilian.getHP() > 0 && civilian.getPosition(world) instanceof Building)) {
                civiliansToRemove.add(civilian);
            }
        }
        shouldCheckCiviliansQueue.removeAll(civiliansToRemove);

        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) standardEntity;
            if (civilian.isPositionDefined() && !(civilian.getPosition(world) instanceof Refuge)
                    && civilian.getHP() > 0 && civilian.getPosition(world) instanceof Building) {
                if (!shouldCheckCiviliansQueue.contains(civilian)) {
                    shouldCheckCiviliansQueue.add(civilian);
                }
            }
        }
    }

    private EntityID AssignProperBuildingToMe(Set<EntityID> civilianBuildings) {
        int i = 0, j = 0;

        for (EntityID entityID : civilianBuildings) {
            if (i < world.getPoliceForceList().size()) {
                if (world.getPoliceForceList().get(i).getID() == world.getSelf().getID()) {
                    return entityID;
                }
                i++;
            } else if (j < world.getPoliceForceList().size()) {
                if (world.getPoliceForceList().get(j).getID() == world.getSelf().getID()) {
                    return entityID;
                }
                j++;
            }
        }
        return null;
    }

}
