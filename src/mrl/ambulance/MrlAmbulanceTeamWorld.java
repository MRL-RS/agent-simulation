package mrl.ambulance;

import javolution.util.FastMap;
import mrl.ambulance.marketLearnerStrategy.AmbulanceTeamBid;
import mrl.ambulance.marketLearnerStrategy.Task;
import mrl.ambulance.structures.RescuedCivilian;
import mrl.ambulance.structures.StartRescuingCivilian;
import mrl.world.MrlWorld;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: Pooyad
 * Date: Feb 21, 2011
 * Time: 7:49:20 PM
 */
public class MrlAmbulanceTeamWorld extends MrlWorld {

    //P.D.G. Added  for AmbulanceTeam Tasks
    //
    // <simulationTime, list of recieved best State Value from all ATs with myself>
    protected HashMap<Integer, ArrayList<Integer>> valueFunctions;
    // map of <bidderID,its Bids>
    protected Map<EntityID, ArrayList<AmbulanceTeamBid>> bids;
    protected List<EntityID> badeHumans;

    // map of <agentID,victimID>
    protected Map<EntityID, EntityID> tasks = new FastMap<EntityID, EntityID>();

    protected ArrayList<Pair<EntityID, EntityID>> loaders; //Pair<LoaderID,CivilianID>
    protected ArrayList<RescuedCivilian> currentlyRescuedCivilians;
    protected HashSet<EntityID> rescuedCivilians = new HashSet<EntityID>();
    protected ArrayList<StartRescuingCivilian> startRescuingCivilians;  // should use chanel 0 as saying
    protected HashSet<Civilian> transportingCivilians;
    //    protected Map<EntityID,Map<Integer,List<AmbulanceTeamBid>>> victimAMbulanceMap;
    protected Map<EntityID, ArrayList<Pair<EntityID, Integer>>> victimBidsMap;

    // shows which civilian is the current rescuing one of the ATs <<ATid,CivilianID>,<startTime,currentTime>>
    protected Map<Pair<EntityID, EntityID>, Pair<Integer, Integer>> ambulanceCivilianMap;

    // map of <AmbulanceId,VictimID>
    protected Map<EntityID, EntityID> taskAssignment;

    // map of <VictimID,AmbulanceId>
    protected Map<EntityID, List<EntityID>> agentAssignment;


    protected Map<EntityID, Map<EntityID, Task>> taskLists;

    //Ambulance Leader Bids
    protected List<EntityID> leaderBids;


    public MrlAmbulanceTeamWorld(StandardAgent self, Collection<? extends Entity> entities, Config config) {
        super(self, entities, config);

        this.bids = new FastMap<EntityID, ArrayList<AmbulanceTeamBid>>();
        this.badeHumans = new ArrayList<EntityID>();
        this.loaders = new ArrayList<Pair<EntityID, EntityID>>();
        this.currentlyRescuedCivilians = new ArrayList<RescuedCivilian>();
        this.valueFunctions = new HashMap<Integer, ArrayList<Integer>>();
        this.ambulanceCivilianMap = new HashMap<Pair<EntityID, EntityID>, Pair<Integer, Integer>>();
        this.startRescuingCivilians = new ArrayList<StartRescuingCivilian>();
        this.transportingCivilians = new HashSet<Civilian>();
//        this.victimAMbulanceMap=new FastMap<EntityID,Map<Integer,List<AmbulanceTeamBid>>>();
        this.victimBidsMap = new FastMap<EntityID, ArrayList<Pair<EntityID, Integer>>>();
        this.taskAssignment = new FastMap<EntityID, EntityID>();
        this.agentAssignment = new FastMap<EntityID, List<EntityID>>();
        this.taskLists = new FastMap<EntityID, Map<EntityID, Task>>();
        this.leaderBids = new ArrayList<EntityID>();

    }

    @Override
    public void updateAfterSense() {
        super.updateAfterSense();

    }


    public Map<Pair<EntityID, EntityID>, Pair<Integer, Integer>> getAmbulanceCivilianMap() {
        return ambulanceCivilianMap;
    }

    public Map<EntityID, ArrayList<AmbulanceTeamBid>> getBids() {
        return bids;
    }

    public ArrayList<RescuedCivilian> getCurrentlyRescuedCivilians() {
        return currentlyRescuedCivilians;
    }

    public ArrayList<StartRescuingCivilian> getStartRescuingCivilianPackets() {
        return startRescuingCivilians;
    }

    public HashSet<Civilian> getTransportingCivilians() {
        return transportingCivilians;
    }

    public HashMap<Integer, ArrayList<Integer>> getValueFunctions() {
        return valueFunctions;
    }


    public ArrayList<Pair<EntityID, EntityID>> getLoaders() {
        return loaders;
    }

    public List<EntityID> getBadeHumans() {
        return badeHumans;
    }

    public Map<EntityID, ArrayList<Pair<EntityID, Integer>>> getVictimBidsMap() {
        return victimBidsMap;
    }

    /**
     * map of AmbulanceId to VictimID
     *
     * @return
     */
    public Map<EntityID, EntityID> getTaskAssignment() {
        return taskAssignment;
    }

    public Map<EntityID, List<EntityID>> getAgentAssignment() {
        return agentAssignment;
    }

    /**
     * map of AmbulanceID to map(VictimID,task) as its tasks
     *
     * @return
     */
    public Map<EntityID, Map<EntityID, Task>> getTaskLists() {
        return taskLists;
    }

    public List<EntityID> getLeaderBids() {
        return leaderBids;
    }

    public HashSet<EntityID> getRescuedCivilians() {
        return rescuedCivilians;
    }

    /**
     * Am I loader for this victim?
     *
     * @param id the victim to check wether I should load it or not
     * @return if I am loader return true otherwise false
     */
    public boolean isloader(EntityID id) {
        return this.loaders.contains(new Pair<EntityID, EntityID>(self.getID(), id));
    }
}
