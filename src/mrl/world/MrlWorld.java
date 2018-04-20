package mrl.world;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.MrlPersonalData;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.MRLConstants;
import mrl.common.TimestampThreadLogger;
import mrl.common.Util;
import mrl.common.clustering.ClusterManager;
import mrl.communication2013.entities.PositionTypes;
import mrl.helper.*;
import mrl.partition.Partition;
import mrl.partition.Partitions;
import mrl.partition.PartitionsI;
import mrl.partition.PreRoutingPartitions;
import mrl.partitioning.IPartitionManager;
import mrl.platoon.MrlCentre;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.police.moa.Bid;
import mrl.world.object.*;
import mrl.world.object.mrlZoneEntity.MrlZoneFactory;
import mrl.world.object.mrlZoneEntity.MrlZones;
import mrl.world.routing.grid.AreaGrids;
import mrl.world.routing.path.Path;
import mrl.world.routing.path.Paths;
import rescuecore2.Constants;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: mrl
 * Date: Apr 28, 2010
 * Time: 10:11:22 PM
 */
public class MrlWorld extends StandardWorldModel {
    protected StandardAgent self;
    protected MrlPlatoonAgent platoonAgent;
    protected MrlCentre centre;
    protected Human selfHuman;
    protected Building selfBuilding;

    protected int time;
    private Set<EntityID> changes = new FastSet<EntityID>();

    protected boolean CommunicationLess = true;
    //    protected boolean CommunicationLimited = false;
    protected boolean isCommunicationLow = false;
    protected boolean isCommunicationMedium = false;
    protected boolean isCommunicationHigh = false;
    protected boolean isMapHuge = false;
    protected boolean isMapMedium = false;
    protected boolean isMapSmall = false;

    protected Area centerOfMap;
    protected int worldTotalArea;
    private int longestDistanceOfTheMap = 0; // when there is no blockade in the map

    //    protected IndexSort indexSort;
    protected List<IHelper> helpers = new ArrayList<IHelper>();
    protected List<PoliceForce> policeForceList = new ArrayList<PoliceForce>();
    protected List<FireBrigade> fireBrigadeList = new ArrayList<FireBrigade>();
    protected List<AmbulanceTeam> ambulanceTeamList = new ArrayList<AmbulanceTeam>();
    protected Set<EntityID> unvisitedBuildings = new HashSet<EntityID>();
    protected Set<EntityID> visitedBuildings = new FastSet<EntityID>();
    private Set<EntityID> thisCycleEmptyBuildings = new FastSet<EntityID>();
    //    protected Set<EntityID> sensedBuildings = new FastSet<EntityID>();
    protected List<MrlBuilding> shouldCheckInsideBuildings = new ArrayList<MrlBuilding>();
    protected MrlZones zones;
    protected List<MrlBuilding> mrlBuildings;
    protected Map<EntityID, MrlBuilding> tempBuildingsMap;
    protected List<MrlRoad> mrlRoads;
    protected Map<EntityID, MrlRoad> mrlRoadsMap;
    protected Set<EntityID> burningBuildings = new FastSet<EntityID>();
    protected Partitions partitions;
    protected Map<Human, Partition> humanPartitionMap;
    protected PreRoutingPartitions preRoutingPartitions;
    protected Paths paths;
    protected Map<EntityID, EntityID> entranceRoads = new FastMap<EntityID, EntityID>();

    protected int totalAreaOfAllBuildings = 0;

    protected Set<EntityID> shouldCheckBuildings = new FastSet<EntityID>();
    protected Set<EntityID> fullBuildings;
    protected Set<EntityID> emptyBuildings;

    /*---------------Sajjad-------------*/
    protected Set<EntityID> borderBuildings;
    public BorderEntities borderFinder;
    protected ClusterManager fireClusterManager;
    /*---------------------------------------*/

    /*---------------Mostafa-------------*/
    protected Set<MrlBuilding> estimatedBurningBuildings = new FastSet<MrlBuilding>();
    /*-----------------------------------*/

    //    protected Highways highways;
    protected double pole = 0;
    protected Long uniqueMapNumber;
    protected int minX, minY, maxX, maxY;
    protected double mapDiameter;

    public float rayRate = 0.0025f;
    private int kernel_TimeSteps = 1000;
    public int maxID = 0;

    Map<String, Building> buildingXYMap = new FastMap<String, Building>();
    Map<String, Road> roadXYMap = new FastMap<String, Road>();

    protected Set<Human> agentsSeen = new FastSet<Human>();
    protected Set<Civilian> civiliansSeen = new HashSet<Civilian>();
    protected Set<FireBrigade> fireBrigadesSeen = new FastSet<FireBrigade>();
    protected HashSet<Road> roadsSeen = new HashSet<Road>();
    protected Set<MrlBlockade> mrlBlockadesSeen = new HashSet<MrlBlockade>();
    protected Set<MrlRoad> mrlRoadsSeen = new HashSet<MrlRoad>();
    protected HashSet<Blockade> blockadeSeen = new HashSet<Blockade>();
    protected Set<Building> buildingSeen = new HashSet<Building>();

    //DELDAR..........
    protected AmbulanceUtilities ambulanceUtilities;
    private EntityID ambulanceLeaderID = null; // the leader who shoud process bids and allocate ambulances to victims and send their tasks
    private List<StandardEntity> firstTimeSeenVictims = new ArrayList<StandardEntity>();

    private Map<Integer, State> agentStateMap;

    private IPartitionManager partitionManager;

    private Map<EntityID, Map<EntityID, Bid>> targetBidsMap;
    private Map<EntityID, EntityID> civilianPositionMap;
    private Map<EntityID, EntityID> agentPositionMap;
    private Map<EntityID, EntityID> agentFirstPositionMap;
    private List<EntityID> heardCivilians;
    private int lastAfterShockTime = 0;
    private int aftershockCount = 0;

    protected ClusterManager policeTargetClusterManager;

    private long thinkStartTime_;
    private long thinkTime;
    private long thinkTimeThreshold;
    private boolean useSpeak;
    private int viewDistance;
    private int clearDistance;
    private int clearRadius;
    private int ignoreCommandTime;
    private int maxExtinguishDistance;
    private int maxPower;
    private int voiceRange;
    private Set<StandardEntity> availableHydrants = new HashSet<StandardEntity>();
    private int lastUpdateHydrants = -1;

    private Set<StandardEntity> buildings;
    private Set<StandardEntity> roads;
    private Set<StandardEntity> areas;
    private Set<StandardEntity> humans;
    private Set<StandardEntity> agents;
    private Set<StandardEntity> platoonAgents;

    public MrlWorld(StandardAgent self, Collection<? extends Entity> entities, Config config) {
        super();
        addEntities(entities);
        for (StandardEntity standardEntity : getEntitiesOfType(StandardEntityURN.POLICE_FORCE, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.AMBULANCE_TEAM)) {
            if (standardEntity instanceof FireBrigade) {
                fireBrigadeList.add((FireBrigade) standardEntity);
            } else if (standardEntity instanceof PoliceForce) {
                policeForceList.add((PoliceForce) standardEntity);
            } else if (standardEntity instanceof AmbulanceTeam) {
                ambulanceTeamList.add((AmbulanceTeam) standardEntity);
            }
            if (maxID < standardEntity.getID().getValue()) {
                maxID = standardEntity.getID().getValue();
            }
        }
        this.self = self;
        if (self instanceof MrlCentre) {
            selfBuilding = (Building) getEntity(self.getID());
            this.centre = (MrlCentre) self;
        } else {
            this.platoonAgent = (MrlPlatoonAgent) self;
            selfHuman = (Human) getEntity(self.getID());
        }

        retrieveConfigParameters(config);
        buildings = new HashSet<StandardEntity>(getBuildingsWithURN());
        roads = new HashSet<StandardEntity>(getRoadsWithURN());
        areas = new HashSet<StandardEntity>(getAreasWithURN());
        humans = new HashSet<StandardEntity>(getHumansWithURN());
        agents = new HashSet<StandardEntity>(getAgentsWithURN());
        platoonAgents = new HashSet<StandardEntity>(getPlatoonAgentsWithURN());

//        this.indexSort = new IndexSort();
        this.humanPartitionMap = new FastMap<Human, Partition>();
        this.civilianPositionMap = new FastMap<EntityID, EntityID>();
        this.agentPositionMap = new FastMap<EntityID, EntityID>();
        this.agentFirstPositionMap = new FastMap<EntityID, EntityID>();
        this.heardCivilians = new ArrayList<EntityID>();
        createUniqueMapNumber();
//        System.out.println("map name: " + getMapName());
//        System.out.println("unique Number: " + getUniqueMapNumber());

        initHelpers();
        for (StandardEntity s : getBuildings()) {
            Building b = (Building) s;
            String xy = b.getX() + "," + b.getY();
            buildingXYMap.put(xy, b);
        }

        createMrlBuildings();

        if (totalAreaOfAllBuildings == 0) {
            computeBuildingsTotalArea();
        }

//        createAreaGrids();

        for (StandardEntity s : getRoads()) {
            Road b = (Road) s;
            String xy = b.getX() + "," + b.getY();
            roadXYMap.put(xy, b);
        }
        calculateMapDimensions();
        this.paths = new Paths(this);
        createMrlRoads();
        getHelper(VisibilityHelper.class).setBuildingsVisitablePart();
        if(platoonAgent!=null){
            MrlPersonalData.VIEWER_DATA.setPathList(paths);
        }

//        MrlZoneFactory newMrlZoneFactory = new MrlZoneFactory(this);
// TODO @Pooya: Review it
//        zones = newMrlZoneFactory.createZones("data/" + getMapName() + ".zone");

//        MrlPersonalData.setZones(zones);

        ambulanceUtilities = new AmbulanceUtilities(this);
        targetBidsMap = new FastMap<EntityID, Map<EntityID, Bid>>();
        agentStateMap = new FastMap<Integer, State>();
        fullBuildings = new FastSet<EntityID>();
        emptyBuildings = new FastSet<EntityID>();

        verifyMap();

        borderBuildings = new FastSet<EntityID>();
        borderFinder = new BorderEntities(this);


        availableHydrants.addAll(getHydrants());
    }

    private void verifyMap() {

        double mapDimension = Math.hypot(getMapWidth(), getMapHeight());

        double rate = mapDimension / MRLConstants.MEAN_VELOCITY_OF_MOVING;

        if (rate > 60) {
            isMapHuge = true;
        } else if (rate > 30) {
            isMapMedium = true;
        } else {
            isMapSmall = true;
        }


    }


    private void computeBuildingsTotalArea() {
        Building building;
        for (StandardEntity buildingEntity : getBuildings()) {
            building = (Building) buildingEntity;
            totalAreaOfAllBuildings += building.getTotalArea();
        }
    }

    private void createAreaGrids() {
        Road road;
        int agentSize = 500;
        AreaGrids areaGrids;
        for (StandardEntity entity : getRoads()) {
            road = (Road) entity;
            areaGrids = new AreaGrids(road, agentSize);

        }
    }


    @Override
    public void merge(ChangeSet changeSet) {
        TimestampThreadLogger threadLogger = TimestampThreadLogger.getCurrentThreadLogger();
        threadLogger.log("merge(ChangeSet changeSet) started.");

        if (changeSet != null) {
            changes = changeSet.getChangedEntities();
        } else {
            System.out.println(" NULL changeSet  " + getTime() + " " + self);
            return;
        }
        PropertyHelper propertyHelper = getHelper(PropertyHelper.class);
        HumanHelper humanHelper = getHelper(HumanHelper.class);
        agentsSeen.clear();
        civiliansSeen.clear();
        fireBrigadesSeen.clear();
        roadsSeen.clear();
        blockadeSeen.clear();
        buildingSeen.clear();
        mrlBlockadesSeen.clear();
        mrlRoadsSeen.clear();

        for (EntityID entityID : changeSet.getChangedEntities()) {

            try {
                //<<<<<<<<<<<<<<<<<<<<<<<< CIVILIAN >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.CIVILIAN.toString())) {
                    Civilian civilian = (Civilian) getEntity(entityID);
                    if (civilian == null) {
                        civilian = new Civilian(entityID);
                        addNewCivilian(civilian);
                    }
                    civiliansSeen.add(civilian);

                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        civilian.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(civilian.getProperty(p.getURN()), time);
                    }
                    humanHelper.setFromSense(civilian.getID(), true);

                    //updating Civilian position map
                    EntityID prevPosition = civilianPositionMap.get(civilian.getID());
                    EntityID currentPosition = civilian.getPosition();

                    if (civilian.getHP() < 0.1 * civilian.getStamina()) {
                        //ignore dead civilians
                        civilianPositionMap.remove(civilian.getID());
                        if (prevPosition != null) {
                            MrlBuilding prevBuilding = getMrlBuilding(prevPosition);
                            if (prevBuilding != null) {
                                prevBuilding.getCivilians().remove(civilian);
                            }
                        }
                        MrlBuilding currentBuilding = getMrlBuilding(currentPosition);
                        if (currentBuilding != null) {
                            currentBuilding.getCivilians().remove(civilian);
                        }

                    } else {
                        if (prevPosition != null && getEntity(prevPosition) instanceof Building) {

                            if (!prevPosition.equals(currentPosition)) {
                                getMrlBuilding(prevPosition).getCivilians().remove(civilian);
                                if (getEntity(currentPosition) instanceof Building) {
                                    getMrlBuilding(currentPosition).getCivilians().add(civilian);
                                }
                            }
                        } else if (getEntity(currentPosition) instanceof Building) {
                            getMrlBuilding(currentPosition).getCivilians().add(civilian);
                        }
                        civilianPositionMap.put(civilian.getID(), civilian.getPosition());
                    }

                    setDamage(civilian);


//                humanHelper.setPreviousDamage(civilian.getID(),civilian.getDamage());
//                humanHelper.setCurrentDamage(civilian.getID(),civilian.getDamage());
//                humanHelper.setLastTimeDamageChanged(civilian.getID(),getTime());
//
//                humanHelper.setPreviousHP(civilian.getID(),civilian.getHP());
//                humanHelper.setCurrentHP(civilian.getID(),civilian.getHP());
//                humanHelper.setLastTimeHPChanged(civilian.getID(),getTime());

                    if (humanHelper.getNearestRefugeID(civilian.getID()) == null) {

                        firstTimeSeenVictims.add(civilian); //todo add it for other agents

                        humanHelper.setFirstHP(civilian.getID(), civilian.getHP());
                        humanHelper.setFirstDamage(civilian.getID(), civilian.getDamage());
                        humanHelper.setFirstBuriedness(civilian.getID(), civilian.getBuriedness());
                        if ((civilian.getDamage() != 0 || civilian.getBuriedness() != 0) && civilian.getHP() > 0 && civilian.getPosition(this) instanceof Area
                                && !(civilian.getPosition(this) instanceof Refuge)) {  //todo, should We let all agents do it or just ATs should do it?
                            Pair<Integer, EntityID> p = ambulanceUtilities.approximatingTTR(civilian);
                            humanHelper.setNearestRefuge(civilian.getID(), p.second());
                            humanHelper.setTimeToRefuge(civilian.getID(), p.first());
//                        System.out.println(getTime() + " " + self.getID() + " " + civilian.getID() + " >>> NearestRefuge SEEEEEEEEN >> " + p);
                        }
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< BLOCKADE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.BLOCKADE.toString())) {
                    Blockade blockade = (Blockade) getEntity(entityID);
                    if (blockade == null) {
                        blockade = new Blockade(entityID);
                    }
                    for (Property p : changeSet.getChangedProperties(entityID)) {

                        blockade.getProperty(p.getURN()).takeValue(p);
//                        propertyHelper.setPropertyTime(blockade.getProperty(p.getURN()), time);
                    }
                    if (getEntity(blockade.getPosition()) != null) {
                        Area area = (Area) getEntity(blockade.getPosition());
                        if (area.getBlockades() == null) {
                            area.setBlockades(new ArrayList<EntityID>());
                        }
                        if (!area.getBlockades().contains(blockade.getID())) {
                            ArrayList<EntityID> blockades = new ArrayList<EntityID>(area.getBlockades());
                            blockades.add(blockade.getID());
                            area.setBlockades(blockades);
                        }
                    }

                    if (getEntity(blockade.getID()) == null) {
                        addEntityImpl(blockade);
                        propertyHelper.addEntityProperty(blockade, time);
                    }


                    blockadeSeen.add(blockade);
                    //<<<<<<<<<<<<<<<<<<<<<<<< BUILDING >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.BUILDING.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.REFUGE.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.AMBULANCE_CENTRE.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.FIRE_STATION.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.POLICE_OFFICE.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.GAS_STATION.toString())) {
                    Building building = (Building) getEntity(entityID);

                    //Checking for AFTER SHOCK occurrence
                    Property brokennessProperty = building.getProperty(StandardPropertyURN.BROKENNESS.toString());
                    if (brokennessProperty.isDefined()) {
                        int newBrokennessValue = -1;
                        for (Property p : changeSet.getChangedProperties(entityID)) {
                            if (p.getURN().endsWith(brokennessProperty.getURN())) {
                                newBrokennessValue = (Integer) p.getValue();
                            }
                        }
                        if (building.getBrokenness() < newBrokennessValue) {
                            //after shock is occurred
                            if (propertyHelper.getPropertyTime(brokennessProperty) > getLastAfterShockTime()) {
                                setAftershockProperties(getTime(), getTime());
                            }
                        }
                    }

                    //Update seen building properties
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        building.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(building.getProperty(p.getURN()), time);
                    }

                    MrlBuilding mrlBuilding = getMrlBuilding(entityID);
                    if (building.isFierynessDefined() && building.isTemperatureDefined()) {
                        mrlBuilding.setEnergy(building.getTemperature() * mrlBuilding.getCapacity());
                        mrlBuilding.updateValues(building);
                    }
                    if (getEntity(building.getID()) == null) {
                        addEntityImpl(building);
                        propertyHelper.addEntityProperty(building, time);
                    }

                    //updating burning buildings set
                    if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                        burningBuildings.add(building.getID());
                    } else {
                        burningBuildings.remove(building.getID());
                    }

                    buildingSeen.add(building);
                    mrlBuilding.setSensed(getTime());
                    if (building.isOnFire()) {
                        mrlBuilding.setIgnitionTime(getTime());
                    }

                    //<<<<<<<<<<<<<<<<<<<<<<<< ROAD >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.ROAD.toString()) ||
                        changeSet.getEntityURN(entityID).equals(StandardEntityURN.HYDRANT.toString())) {
                    Road road = (Road) getEntity(entityID);
                    if (road == null) {
                        road = new Road(entityID);
                    }
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        road.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(road.getProperty(p.getURN()), time);
                    }
                    if (getEntity(road.getID()) == null) {
                        addEntityImpl(road);
                        propertyHelper.addEntityProperty(road, time);
                    }
                    roadsSeen.add(road);
                    MrlRoad mrlRoad = this.getMrlRoad(entityID);
                    mrlRoadsSeen.add(mrlRoad);
                    mrlBlockadesSeen.addAll(mrlRoad.getMrlBlockades());
                    //<<<<<<<<<<<<<<<<<<<<<<<< FIRE_BRIGADE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.FIRE_BRIGADE.toString())) {
                    FireBrigade fireBrigade = (FireBrigade) getEntity(entityID);
                    agentsSeen.add(fireBrigade);
                    fireBrigadesSeen.add(fireBrigade);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        fireBrigade.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(fireBrigade.getProperty(p.getURN()), time);
                    }

                    if (Util.isOnBlockade(this, fireBrigade)) {
                        getHelper(HumanHelper.class).setLockedByBlockade(fireBrigade.getID(), true);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< POLICE_FORCE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.POLICE_FORCE.toString())) {
                    PoliceForce policeForce = (PoliceForce) getEntity(entityID);
                    agentsSeen.add(policeForce);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        policeForce.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(policeForce.getProperty(p.getURN()), time);
                    }

                    if (Util.isOnBlockade(this, policeForce)) {
                        getHelper(HumanHelper.class).setLockedByBlockade(policeForce.getID(), true);
                    }

                    //<<<<<<<<<<<<<<<<<<<<<<<< AMBULANCE_TEAM >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.AMBULANCE_TEAM.toString())) {
                    AmbulanceTeam ambulanceTeam = (AmbulanceTeam) getEntity(entityID);
                    agentsSeen.add(ambulanceTeam);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        ambulanceTeam.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(ambulanceTeam.getProperty(p.getURN()), time);
                    }

                    if (Util.isOnBlockade(this, ambulanceTeam)) {
                        getHelper(HumanHelper.class).setLockedByBlockade(ambulanceTeam.getID(), true);
                    }

                    //<<<<<<<<<<<<<<<<<<<<<<<< FIRE_STATION >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.FIRE_STATION.toString())) {
                    FireStation fireStation = (FireStation) getEntity(entityID);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        fireStation.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(fireStation.getProperty(p.getURN()), time);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< POLICE_OFFICE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.POLICE_OFFICE.toString())) {
                    PoliceOffice policeOffice = (PoliceOffice) getEntity(entityID);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        policeOffice.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(policeOffice.getProperty(p.getURN()), time);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< AMBULANCE_CENTRE >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else if (changeSet.getEntityURN(entityID).equals(StandardEntityURN.AMBULANCE_CENTRE.toString())) {
                    AmbulanceCentre ambulanceCentre = (AmbulanceCentre) getEntity(entityID);
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        ambulanceCentre.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(ambulanceCentre.getProperty(p.getURN()), time);
                    }
                    //<<<<<<<<<<<<<<<<<<<<<<<< StandardEntity >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                } else {
                    StandardEntity standardEntity = getEntity(entityID);
                    if (!(standardEntity instanceof Refuge)) {
                        System.out.println("unknown standardEntity :" + standardEntity);
                    }
                    for (Property p : changeSet.getChangedProperties(entityID)) {
                        standardEntity.getProperty(p.getURN()).takeValue(p);
                        propertyHelper.setPropertyTime(standardEntity.getProperty(p.getURN()), time);
                    }
                }
            } catch (NullPointerException e) {

                e.printStackTrace();
            } catch (ClassCastException ex) {
                ex.printStackTrace();
            }
        }
        threadLogger.log("merge(ChangeSet changeSet) 2");
    }

    private void updatePropertyHelper(ChangeSet changeSet, StandardEntity entity, PropertyHelper propertyHelper, int time) {
        for (Property p : changeSet.getChangedProperties(entity.getID())) {
            entity.getProperty(p.getURN()).takeValue(p);
            propertyHelper.setPropertyTime(entity.getProperty(p.getURN()), time);
        }
    }

    public void updateBeforeSense() {

    }

    public void updateAfterSense() {

        if (time > 1) {
            for (MrlRoad mrlRoad : getMrlRoads()) {
                if (getRoadsSeen().contains(mrlRoad.getParent())) {
                    if (mrlRoad.isNeedUpdate()) {
                        mrlRoad.update();
                    }
                    getHelper(RoadHelper.class).updatePassably(mrlRoad);
                    mrlRoad.setLastSeenTime(getTime());
                    mrlRoad.setSeen(true);
                } else {
                    mrlRoad.resetOldPassably();
                }
            }
            for (MrlBuilding mrlBuilding : getMrlBuildings()) {
                if (getBuildingSeen().contains(mrlBuilding.getSelfBuilding())) {
                    boolean reachable = false;
                    if (mrlBuilding.isOneEntranceOpen(this)) {
                        reachable = true;
                    }
                    MrlRoad mrlRoad;
                    if (reachable) {
                        boolean tempReachable = false;
                        for (Road road : BuildingHelper.getEntranceRoads(this, mrlBuilding.getSelfBuilding())) {
                            mrlRoad = getMrlRoad(road.getID());
                            if (mrlRoad.isReachable()) {
                                tempReachable = true;
                                break;
                            }
                        }
                        if (!tempReachable) {
                            reachable = false;
                        }
                    }
                    mrlBuilding.setReachable(reachable);
                } else {
                    if (mrlBuilding.getSelfBuilding() instanceof Refuge) {
                        mrlBuilding.resetOldReachable(MRLConstants.REFUGE_PASSABLY_RESET_TIME);
                    } else {
                        mrlBuilding.resetOldReachable(MRLConstants.BUILDING_PASSABLY_RESET_TIME);
                    }
                }
                mrlBuilding.getCivilianPossibly().clear();

                //the following instruction remove Burnt buildings from visitedBuildings and add it into emptyBuildings list.
                if (isBuildingBurnt(mrlBuilding.getSelfBuilding())) {
                    setBuildingVisited(mrlBuilding.getID(), false);
                }
            }

            MrlPersonalData.VIEWER_DATA.updateThisCycleData(platoonAgent, this);
        }
        for (IHelper helper : helpers) {
            helper.update();
        }
    }

    protected void initHelpers() {
        //-- add helpers
        helpers.add(new PropertyHelper(this));
        helpers.add(new AreaHelper(this));
        helpers.add(new RoadHelper(this));
        helpers.add(new EdgeHelper());
        helpers.add(new HumanHelper(this));
        helpers.add(new CivilianHelper(this));
        helpers.add(new VisibilityHelper(this));

        for (IHelper helper : helpers) {
            helper.init();
        }
    }

    private void createUniqueMapNumber() {
        long sum = 0;
        for (StandardEntity building : getBuildings()) {
            Building b = (Building) building;
            int[] ap = b.getApexList();
            for (int anAp : ap) {
                if (Long.MAX_VALUE - sum <= anAp) {
                    sum = 0;
                }
                sum += anAp;
            }
        }
        uniqueMapNumber = sum;

//        System.out.println("Unique Map Number=" + uniqueMapNumber);
    }

    private void createMrlRoads() {
        mrlRoads = new ArrayList<MrlRoad>();
        mrlRoadsMap = new FastMap<EntityID, MrlRoad>();
        for (StandardEntity rEntity : getRoads()) {
            Road road = (Road) rEntity;
            MrlRoad mrlRoad = new MrlRoad(road, this);
            mrlRoads.add(mrlRoad);
            mrlRoadsMap.put(road.getID(), mrlRoad);
        }

        MrlPersonalData.VIEWER_DATA.setViewRoadsMap(self.getID(), mrlRoads);
    }

    private void createMrlBuildings() {

        tempBuildingsMap = new FastMap<EntityID, MrlBuilding>();
        mrlBuildings = new ArrayList<MrlBuilding>();
        MrlBuilding mrlBuilding;
        Building building;

        for (StandardEntity standardEntity : getBuildings()) {
            building = (Building) standardEntity;
            String xy = building.getX() + "," + building.getY();
            buildingXYMap.put(xy, building);

            mrlBuilding = new MrlBuilding(standardEntity, this);

            if ((standardEntity instanceof Refuge)
                    || (standardEntity instanceof FireStation)
                    || (standardEntity instanceof PoliceOffice)
                    || (standardEntity instanceof AmbulanceCentre)) {  //todo all of these buildings may be flammable..............
                mrlBuilding.setFlammable(false);
            }
            mrlBuildings.add(mrlBuilding);
            tempBuildingsMap.put(standardEntity.getID(), mrlBuilding);

            // ina bejaye building helper umade.
            unvisitedBuildings.add(standardEntity.getID());
            worldTotalArea += mrlBuilding.getSelfBuilding().getTotalArea();

        }
        shouldCheckInsideBuildings.clear();

        //related to FBLegacyStrategy and Zone operations
//        if (getSelfHuman() instanceof FireBrigade) {
//            for (MrlBuilding b : mrlBuildings) {
//                Collection<StandardEntity> neighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_SAMPLE_DISTANCE);
////            Collection<StandardEntity> fireNeighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_FIRE_DISTANCE);
//                List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
//                for (StandardEntity entity : neighbour) {
//                    if (entity instanceof Building) {
//                        neighbourBuildings.add(entity.getID());
//                        b.addMrlBuildingNeighbour(tempBuildingsMap.get(entity.getID()));
//                    }
//                }
//                b.setNeighbourIdBuildings(neighbourBuildings);
//            }
//        }


        for (MrlBuilding b : mrlBuildings) {
            //MTN
            if (b.getEntrances() != null) {
                building = b.getSelfBuilding();
                List<Road> rEntrances = BuildingHelper.getEntranceRoads(this, building);
                for (Road road : rEntrances) {
                    entranceRoads.put(road.getID(), b.getID());
                }


                boolean shouldCheck = true;
//                if (rEntrances != null) {
//                    if (rEntrances.size() == 0)
//                        shouldCheck = false;
                VisibilityHelper visibilityHelper = getHelper(VisibilityHelper.class);
                for (Road road : rEntrances) {
                    boolean shouldCheckTemp = !visibilityHelper.isInsideVisible(new Point(road.getX(), road.getY()), new Point(building.getX(), building.getY()), building.getEdgeTo(road.getID()), viewDistance);
                    if (!shouldCheckTemp) {
                        shouldCheck = false;
                        break;
//                    }
                    }
                }
                b.setShouldCheckInside(shouldCheck);
                if (shouldCheck) {
                    shouldCheckInsideBuildings.add(b);
                }


            }
//            b.setNeighbourFireBuildings(fireNeighbours);
            MrlPersonalData.VIEWER_DATA.setMrlBuildingsMap(b);

        }

        MrlPersonalData.VIEWER_DATA.setViewerBuildingsMap(self.getID(), mrlBuildings);
    }

    private void calculateMapDimensions() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        Pair<Integer, Integer> pos;
        List<StandardEntity> invalidEntities = new ArrayList<>();
        for (StandardEntity standardEntity : this.getAllEntities()) {
            pos = standardEntity.getLocation(this);
            if (pos.first() == Integer.MIN_VALUE || pos.first() == Integer.MAX_VALUE || pos.second() == Integer.MIN_VALUE || pos.second() == Integer.MAX_VALUE) {
                invalidEntities.add(standardEntity);
                continue;
            }
            if (pos.first() < this.minX)
                this.minX = pos.first();
            if (pos.second() < this.minY)
                this.minY = pos.second();
            if (pos.first() > this.maxX)
                this.maxX = pos.first();
            if (pos.second() > this.maxY)
                this.maxY = pos.second();
        }
        if (!invalidEntities.isEmpty()) {
            System.out.println("##### WARNING: There is some invalid entities ====> " + invalidEntities.size());
        }
    }

    private void calculateMapDiameter() {
        mapDiameter = Math.sqrt(Math.pow(getBounds().getHeight(), 2) + Math.pow(getBounds().getWidth(), 2)) / 2;
    }

    public void partitionMakingOperations(Human self) {
        if (!(self instanceof PoliceForce)) {
            return;
        }

        this.partitions = new Partitions(this, self);

    }

    public void preRoutingPartitions() {
        this.preRoutingPartitions = new PreRoutingPartitions(this);
        this.preRoutingPartitions.setColumnNums(2);
        this.preRoutingPartitions.setRowNums(2);
    }

    private List<EntityID> getNeighbours(StandardEntity building) {
        List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
        Collection<StandardEntity> entityCollection = getObjectsInRange(building, Wall.MAX_SAMPLE_DISTANCE);
        for (StandardEntity entity : entityCollection) {
            if (entity instanceof Building) {
                neighbourBuildings.add(entity.getID());
            }
        }
        return neighbourBuildings;
    }

    public List<MrlBuilding> getMrlBuildings() {
        return mrlBuildings;
    }

    public List<MrlRoad> getMrlRoads() {
        return mrlRoads;
    }

    public MrlRoad getMrlRoad(EntityID roadID) {
        return mrlRoadsMap.get(roadID);
    }

    public MrlBuilding getMrlBuilding(EntityID id) {
        return tempBuildingsMap.get(id);
    }

    private void setDamage(Human human) {
        if (human.getBuriedness() > 0 && human.getDamage() == 0) {
            human.setDamage(6);
        }
    }

    public MrlPlatoonAgent getPlatoonAgent() {
        return platoonAgent;
    }

    public MrlCentre getMrlCentre() {
        return centre;
    }

    public MrlCentre getCenterAgent() {
        return this.centre;
    }

    public boolean isCommunicationLess() {
        return CommunicationLess;
    }

    public void setCommunicationLess(boolean CL) {
        this.CommunicationLess = CL;
    }

//    public boolean isCommunicationLimited() {
//        return CommunicationLimited;
//    }
//
//    public void setCommunicationLimited(boolean cl) {
//        CommunicationLimited = cl;
//    }

    public MrlZones getZones() {
        return zones;
    }

    public List<EntityID> getBuriedAgents() {
        return getHelper(HumanHelper.class).getBuriedAgents();
    }

    public List<MrlBuilding> getShouldCheckInsideBuildings() {
        return shouldCheckInsideBuildings;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public void addNewCivilian(Civilian civilian) {
        this.addEntityImpl(civilian);
        getHelper(PropertyHelper.class).addEntityProperty(civilian, getTime());
        getHelper(HumanHelper.class).setInfoMap(civilian.getID());
        getHelper(CivilianHelper.class).setInfoMap(civilian.getID());
    }

    public <T extends IHelper> T getHelper(Class<T> c) {
        for (IHelper helper : helpers) {
            if (c.isInstance(helper)) {
                return c.cast(helper);
            }
        }
        throw new RuntimeException("Helper not available for:" + c);
    }

    public Area getCenterOfMap() {
        if (centerOfMap != null) {
            return centerOfMap;
        }

        double ret;
        int min_x = Integer.MAX_VALUE;
        int max_x = Integer.MIN_VALUE;
        int min_y = Integer.MAX_VALUE;
        int max_y = Integer.MIN_VALUE;

        Collection<StandardEntity> areas = getAreas();

        long x = 0, y = 0;
        Area result;

        for (StandardEntity entity : areas) {
            Area area1 = (Area) entity;
            x += area1.getX();
            y += area1.getY();
        }

        x /= areas.size();
        y /= areas.size();
        result = (Area) areas.iterator().next();
        for (StandardEntity entity : areas) {
            Area temp = (Area) entity;
            if (Util.distance((int) x, (int) y, result.getX(), result.getY()) > Util.distance((int) x, (int) y, temp.getX(), temp.getY())) {
                result = temp;
            }

            if (temp.getX() < min_x) {
                min_x = temp.getX();
            } else if (temp.getX() > max_x)
                max_x = temp.getX();

            if (temp.getY() < min_y) {
                min_y = temp.getY();
            } else if (temp.getY() > max_y)
                max_y = temp.getY();
        }
        ret = (Math.pow((min_x - max_x), 2) +
                Math.pow((min_y - max_y), 2));
        ret = Math.sqrt(ret);
        pole = ret;
        centerOfMap = result;

        return result;
    }

    public PartitionsI getPartitions() {
        //    if(isCommunicationLess())
        return partitions;
        //  else
        //      return searchingPartitions;
    }

    public PreRoutingPartitions getPreRoutingPartitions() {
        return preRoutingPartitions;
    }

    public StandardAgent getSelf() {
        return self;
    }

    public Human getSelfHuman() {
        return selfHuman;
    }

    public Building getSelfBuilding() {
        return selfBuilding;
    }

    public StandardEntity getSelfPosition() {
        if (self instanceof MrlCentre) {
            return selfBuilding;
        } else {
            return selfHuman.getPosition(this);
        }
    }

    public PositionTypes getSelfPositionType() {
        if (getSelfPosition() instanceof Road) {
            return PositionTypes.Road;
        } else {
            return PositionTypes.Building;
        }
    }

    public Pair<Integer, Integer> getSelfLocation() {
        if (self instanceof MrlPlatoonAgent) {
            return selfHuman.getLocation(this);
        } else {
            return selfBuilding.getLocation(this);
        }
    }

    public Paths getPaths() {
        return paths;
    }

    public Path getPath(EntityID id) {

        for (Path path : paths) {
            if (path.getId().equals(id)) {
                return path;
            }
        }
        return null;
    }

//    public Highways getHighways() {
//        return highways;
//    }

    public Config getConfig() {
        if (self instanceof MrlPlatoonAgent) {
            return ((MrlPlatoonAgent) self).getConfig();
        } else if (self instanceof MrlCentre) {
            return ((MrlCentre) self).getConfig();
        }
        return null;
    }

    public Set<EntityID> getVisitedBuildings() {
        return visitedBuildings;
    }

    public Set<EntityID> getUnvisitedBuildings() {
        return unvisitedBuildings;
    }

    public Set<EntityID> getBurningBuildings() {
        return burningBuildings;
    }

    public int getMinX() {
        return this.minX;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public int getMapWidth() {
        return maxX - minX;
    }

    public int getMapHeight() {
        return maxY - minY;
    }

    public Long getUniqueMapNumber() {
        return uniqueMapNumber;
    }

    public Set<FireBrigade> getFireBrigadesSeen() {
        return fireBrigadesSeen;
    }

    public Set<Human> getAgentsSeen() {
        return agentsSeen;
    }

    public Set<Civilian> getCiviliansSeen() {
        return civiliansSeen;
    }

    public HashSet<Road> getRoadsSeen() {
        return roadsSeen;
    }

    public Set<Blockade> getBlockadeSeen() {
        return blockadeSeen;
    }

    public Set<Building> getBuildingSeen() {
        return buildingSeen;
    }

    public List<EntityID> getInMyPartition(Collection<EntityID> buildings) {
        List<EntityID> inPartition = new ArrayList<EntityID>();
        for (EntityID entityID : buildings) {
            Building building = (Building) getEntity(entityID);
            if (getPartitions().getMyPartition().getBuildings().contains(getMrlBuilding(building.getID()))) {
                inPartition.add(entityID);
            }
        }
        return inPartition;
    }

    public <T extends StandardEntity> T getEntity(EntityID id, Class<T> c) {
        StandardEntity entity;

        entity = getEntity(id);
        if (c.isInstance(entity)) {
            T castedEntity;

            castedEntity = c.cast(entity);
            return castedEntity;
        } else {
            return null;
        }
    }

    public List<EntityID> getEntityIdsOfType(StandardEntityURN urn) {
        Collection<StandardEntity> entities = getEntitiesOfType(urn);
        List<EntityID> list = new ArrayList<EntityID>();
        for (StandardEntity entity : entities) {
            list.add(entity.getID());
        }
        return list;
    }

    public Collection<StandardEntity> getBuildingsWithURN() {
        return getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    public Collection<StandardEntity> getBuildings() {
        return buildings;
    }

    public Collection<StandardEntity> getHydrants() {
        return getEntitiesOfType(StandardEntityURN.HYDRANT);
    }

    public Set<StandardEntity> getAvailableHydrants() {
        if (lastUpdateHydrants < getTime() && selfHuman != null && selfHuman instanceof FireBrigade && !getHydrants().isEmpty()) {
            lastUpdateHydrants = getTime();
            availableHydrants.clear();
            availableHydrants.addAll(getHydrants());
            StandardEntity position;
            MrlRoad hydrantMrlRoad;
            PropertyHelper propertyHelper = getHelper(PropertyHelper.class);
            for (FireBrigade fireBrigade : getFireBrigadeList()) {
                if (fireBrigade.getID().equals(selfHuman.getID())) {
                    continue;
                }
                if (fireBrigade.isPositionDefined()) {
                    position = fireBrigade.getPosition(this);
                    if (position instanceof Hydrant) {
                        hydrantMrlRoad = getMrlRoad(position.getID());
                        int agentDataTime = propertyHelper.getEntityLastUpdateTime(fireBrigade);
                        int hydrantSeenTime = hydrantMrlRoad.getLastSeenTime();
                        if (getTime() - agentDataTime > 10 && getTime() - hydrantSeenTime > 10) {
//                                printData("my data from " + fireBrigade + " is out of date... my data time is : " + agentDataTime + " and hydrant seen time is: " + hydrantSeenTime);
                            continue;
                        }
                        availableHydrants.remove(position);
                    }
                }
            }
        }

        return availableHydrants;
    }

    public Collection<StandardEntity> getGasStations() {
        return getEntitiesOfType(StandardEntityURN.GAS_STATION);
    }


    public Set<EntityID> getBuildingIDs() {
        Set<EntityID> buildingIDs = new FastSet<EntityID>();
        Collection<StandardEntity> buildings = getBuildings();
        for (StandardEntity entity : buildings) {
            buildingIDs.add(entity.getID());
        }

        return buildingIDs;
    }

    public Collection<StandardEntity> getRefuges() {
        return getEntitiesOfType(StandardEntityURN.REFUGE);
    }

    public Collection<StandardEntity> getCentres() {
        return getEntitiesOfType(
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION);
    }

    public Collection<StandardEntity> getRoads() {
        return roads;
    }

    public Collection<StandardEntity> getRoadsWithURN() {
        return getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
    }

    public Collection<StandardEntity> getAreas() {
        return areas;
    }

    public Collection<StandardEntity> getAreasWithURN() {
        return getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.ROAD,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.HYDRANT,
                StandardEntityURN.GAS_STATION);
    }

    public Collection<StandardEntity> getHumans() {
        return humans;
    }

    public Collection<StandardEntity> getHumansWithURN() {
        return getEntitiesOfType(
                StandardEntityURN.CIVILIAN,
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    public Collection<StandardEntity> getAgents() {
        return agents;
    }

    public Collection<StandardEntity> getAgentsWithURN() {
        return getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.AMBULANCE_CENTRE);
    }

    public Collection<StandardEntity> getPlatoonAgents() {
        return platoonAgents;
    }

    public Collection<StandardEntity> getPlatoonAgentsWithURN() {
        return getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    public Collection<StandardEntity> getPoliceForces() {
        return getEntitiesOfType(StandardEntityURN.POLICE_FORCE);
    }

    public Collection<StandardEntity> getPoliceOffices() {
        return getEntitiesOfType(StandardEntityURN.POLICE_OFFICE);
    }

    public Collection<StandardEntity> getAmbulanceTeams() {
        return getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM);
    }

    public Collection<StandardEntity> getAmbulanceCentres() {
        return getEntitiesOfType(StandardEntityURN.AMBULANCE_CENTRE);
    }

    public Collection<StandardEntity> getFireBrigades() {
        return getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE);
    }

    public Collection<StandardEntity> getFireStations() {
        return getEntitiesOfType(StandardEntityURN.FIRE_STATION);
    }

    public Collection<StandardEntity> getCivilians() {
        return getEntitiesOfType(StandardEntityURN.CIVILIAN);
    }

    public List<PoliceForce> getPoliceForceList() {
        return policeForceList;
    }

    public List<FireBrigade> getFireBrigadeList() {
        return fireBrigadeList;
    }

    public List<AmbulanceTeam> getAmbulanceTeamList() {
        return ambulanceTeamList;
    }

    public Set<EntityID> getChanges() {
        return changes;
    }

    public boolean isVisible(StandardEntity entity) {
        return changes.contains(entity.getID()) && Util.distance(entity.getLocation(this), getSelfLocation()) <= viewDistance;
    }

    public boolean isVisible(EntityID entityID) {
        return changes.contains(entityID);
    }

    public int getWorldTotalArea() {
        return worldTotalArea;
    }

    public String getMapName() {
        return getUniqueMapNumber().toString();
    }

    public int getLongestDistanceOfTheMap() {
        return longestDistanceOfTheMap;
    }

    public void setLongestDistanceOfTheMap(int predictedDistance) {
        this.longestDistanceOfTheMap = predictedDistance;
    }

    public Building getBuildingInPoint(int x, int y) {
        String xy = x + "," + y;
        return buildingXYMap.get(xy);
    }

    public Road getRoadInPoint(Point point) {
        String xy = point.getX() + "," + point.getY();
        Road road = roadXYMap.get(xy);
        if (road == null) {
            for (StandardEntity entity : getRoads()) {
                Road r = (Road) entity;
                if (r.getShape().contains(point)) {
                    return r;
                }
            }
        }
        return road;
    }

    public void printData(String data) {
        MrlPersonalData.VIEWER_DATA.printData(platoonAgent, time, data);
    }

    public List<Path> getPathsOfThisArea(Area area) {
        int loop = 0;
        List<Path> paths = new ArrayList<Path>();
        List<Area> neighbours = new ArrayList<Area>();
        Road road = null;
        Area tempArea;
        neighbours.add(area);
        Area neighbour;
        EntityID pathId;
        RoadHelper roadHelper = getHelper(RoadHelper.class);
        while (road == null && !neighbours.isEmpty() && loop < 20) {
            loop++;
            tempArea = neighbours.get(0);
            neighbours.remove(0);

            for (EntityID entityID : tempArea.getNeighbours()) {
                neighbour = (Area) getEntity(entityID);
                if (getEntity(entityID) instanceof Road) {
                    road = (Road) getEntity(entityID);
                    pathId = roadHelper.getPathId(road.getID());
                    if (pathId != null) {
                        Path path = getPath(pathId);
                        if (!paths.contains(path)) {
                            paths.add(path);
                        }
                    }
                } else {
                    if (!neighbours.contains(neighbour)) {
                        neighbours.add(neighbour);
                    }
                }
            }
        }

        return paths;
    }

    public EntityID getAmbulanceLeaderID() {
        return ambulanceLeaderID;
    }

    public void setAmbulanceLeaderID(EntityID entityID) {
        this.ambulanceLeaderID = entityID;
    }

    public boolean amIAmbulanceLeader() {
        if (this.ambulanceLeaderID != null && this.ambulanceLeaderID.equals(self.getID())) {
            return true;
        } else {
            return false;
        }

    }

    public List<StandardEntity> getFirstTimeSeenVictims() {
        return firstTimeSeenVictims;
    }

//    public List<StandardEntity> getFreeAgents() {
//        HumanHelper humanHelper = getHelper(HumanHelper.class);
//        List<StandardEntity> freeAgents = new ArrayList<StandardEntity>();
//        freeAgents.addAll(getAgents());
//        freeAgents.removeAll(humanHelper.getBlockedAgents());
//        freeAgents.removeAll(humanHelper.getBuriedAgents());
//        return freeAgents;
//    }

    public int getKernel_TimeSteps() {
        return kernel_TimeSteps;
    }

    public void setKernelTimeSteps(int timeSteps) {
        this.kernel_TimeSteps = timeSteps;
    }

    public double getMapDiameter() {
        if (mapDiameter == 0) {
            calculateMapDiameter();
        }
        return mapDiameter;
    }


    /**
     * It is a data structure for keeping a Map of bidder and their bids for each specified target
     *
     * @return bids for each target
     */
    public Map<EntityID, Map<EntityID, Bid>> getTargetBidsMap() {
        return targetBidsMap;
    }


    /**
     * It keeps states of the agent in each time cycle
     *
     * @return a amp of agent states Cycle-State
     */
    public Map<Integer, State> getAgentStateMap() {
        return agentStateMap;
    }

    public IPartitionManager getPartitionManager() {
        return partitionManager;
    }

    public void setPartitionManager(IPartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public void getPathLength() {
        for (Path p : getPaths()) {
            getPlatoonAgent().getPathPlanner().planMove(p.getHeadOfPath(), p.getEndOfPath(), 0, false);
            p.setLenght(getPlatoonAgent().getPathPlanner().getPathCost());
        }
    }

    public Map<EntityID, EntityID> getCivilianPositionMap() {
        return civilianPositionMap;
    }

    public Map<EntityID, EntityID> getAgentPositionMap() {
        return agentPositionMap;
    }

    public Map<EntityID, EntityID> getAgentFirstPositionMap() {
        return agentFirstPositionMap;
    }

    public int getMyDistanceTo(StandardEntity entity) {
        return getDistance(getSelfPosition(), entity);
    }

    public int getMyDistanceTo(EntityID entityID) {
        return getDistance(getSelfPosition(), getEntity(entityID));
    }

    /**
     * gets buildings which contain one or more civilians
     *
     * @return set of full building ids
     */
    public Set<EntityID> getFullBuildings() {
        return fullBuildings;
    }

    /**
     * gets buildings which is empty  means contains no civilian
     *
     * @return set of empty buildings
     */
    public Set<EntityID> getEmptyBuildings() {
        return emptyBuildings;
    }

    public int getTotalAreaOfAllBuildings() {
        return totalAreaOfAllBuildings;
    }

    public Polygon getWorldPolygon() {
        Polygon worldPolygon;

        double[] point = new double[4];
        int xs[] = new int[4];
        int ys[] = new int[4];

        point[0] = this.getMinX() - 1;
        point[1] = this.getMinY() - 1;
        point[2] = this.getMaxX() + 1;
        point[3] = this.getMaxY() + 1;

        xs[0] = (int) point[0];
        ys[0] = (int) point[1];

        xs[1] = (int) point[2];
        ys[1] = (int) point[1];

        xs[2] = (int) point[2];
        ys[2] = (int) point[3];

        xs[3] = (int) point[0];
        ys[3] = (int) point[3];

        worldPolygon = new Polygon(xs, ys, 4);

        return worldPolygon;
    }

    public boolean isMapHuge() {
        return isMapHuge;
    }

    public boolean isMapMedium() {
        return isMapMedium;
    }

    public boolean isMapSmall() {
        return isMapSmall;
    }

    public boolean isEntrance(Road road) {
        return entranceRoads.containsKey(road.getID());

    }

    /**
     * this method remove input building from {@code visitedBuildings}, add it in the {@code unvisitedBuilding} and prepare
     * message that should be send.<br/><br/>
     * <font color="red"><b>Note: </b></font> this method is calling automatically in  agent {@code act} in {@link MrlPlatoonAgent}
     *
     * @param buildingID  {@code EntityID} of building that visited!
     * @param sendMessage {@code boolean} to sent visited building message
     */
    public void setBuildingVisited(EntityID buildingID, boolean sendMessage) {
        MrlBuilding mrlBuilding = getMrlBuilding(buildingID);
        if (platoonAgent == null) {
            return;
        }
        if (!mrlBuilding.isVisited()) {
            mrlBuilding.setVisited();
            visitedBuildings.add(buildingID);
            unvisitedBuildings.remove(buildingID);
        }
        updateEmptyBuildingState(mrlBuilding, sendMessage);
    }

    public void updateEmptyBuildingState(MrlBuilding mrlBuilding, boolean sendMessage) {
        if (!mrlBuilding.isVisited()) {
            return;
        }

        if (!emptyBuildings.contains(mrlBuilding.getID()) && mrlBuilding.getCivilians().isEmpty()) {
            if (sendMessage) {
                thisCycleEmptyBuildings.add(mrlBuilding.getID());
            }
            emptyBuildings.add(mrlBuilding.getID());
        }

        if (emptyBuildings.contains(mrlBuilding.getID()) && !mrlBuilding.getCivilians().isEmpty()) {
            emptyBuildings.remove(mrlBuilding.getID());
        }
    }

    /**
     * add civilian who speak of it was heard in current cycle!
     *
     * @param civID EntityID of civilian
     */
    public void addHeardCivilian(EntityID civID) {
        MrlPersonalData.VIEWER_DATA.setHeardPositions(civID, getSelfLocation());

        if (!heardCivilians.contains(civID)) {
            heardCivilians.add(civID);
        }
    }

    /**
     * Gets heard civilians at current cycle;<br/>
     * <br/>
     * <b>Note: </b> At each cycle the list will be cleared
     *
     * @return EntityIDs of heard civilians
     */
    public List<EntityID> getHeardCivilians() {
        return heardCivilians;
    }

    /**
     * Map of entrance RoadID to BuildingID
     *
     * @return
     */
    public Map<EntityID, EntityID> getEntranceRoads() {
        return entranceRoads;
    }

    public Set<EntityID> getShouldCheckBuildings() {
        return shouldCheckBuildings;
    }

    public void setShouldCheckBuildings(Set<EntityID> shouldCheckBuildings) {
        this.shouldCheckBuildings = shouldCheckBuildings;
    }

    public Set<EntityID> getBorderBuildings() {
        return borderBuildings;
    }

    public Set<EntityID> getThisCycleEmptyBuildings() {
        return thisCycleEmptyBuildings;
    }

    @Override
    public Collection<StandardEntity> getObjectsInRange(EntityID entity, int range) {
        return super.getObjectsInRange(getEntity(entity), range);
    }

    @Override
    public Collection<StandardEntity> getObjectsInRange(StandardEntity entity, int range) {
        return super.getObjectsInRange(entity, range);
    }

    @Override
    public Collection<StandardEntity> getObjectsInRange(int x, int y, int range) {
        int newRange = (int) (0.64 * range);
        return super.getObjectsInRange(x, y, newRange);
    }

    public ClusterManager getFireClusterManager() {
        return fireClusterManager;
    }

    public ClusterManager getPoliceTargetClusterManager() {
        return policeTargetClusterManager;
    }


    public boolean isBuried(Human human) {
        return human.isBuriednessDefined() && human.getBuriedness() > 0;
    }

    public void retrieveConfigParameters(Config config) {
        thinkTime = config.getIntValue(MRLConstants.THINK_TIME_KEY);
        thinkTimeThreshold = (long) (thinkTime * 0.9);
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(MRLConstants.SPEAK_COMMUNICATION_MODEL_KEY);
        viewDistance = config.getIntValue(MRLConstants.MAX_VIEW_DISTANCE_KEY);
        ignoreCommandTime = getConfig().getIntValue(MRLConstants.IGNORE_AGENT_COMMANDS_KEY);
        clearDistance = config.getIntValue(MRLConstants.MAX_CLEAR_DISTANCE_KEY);
        clearRadius = config.getIntValue(MRLConstants.CLEAR_RADIUS_KEY, 2000);//todo <==== clear radius key is not visible ... Kernel Bug
        maxExtinguishDistance = config.getIntValue(MRLConstants.MAX_EXTINGUISH_DISTANCE_KEY);
        maxPower = config.getIntValue(MRLConstants.MAX_EXTINGUISH_POWER_KEY);
        voiceRange = config.getIntValue(MRLConstants.VOICE_RANGE_KEY);

    }

    public int getIgnoreCommandTime() {
        return ignoreCommandTime;
    }

    public int getVoiceRange() {
        return voiceRange;
    }

    public int getClearDistance() {
        return clearDistance;
    }

    public int getClearRadius() {
        return clearRadius;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public int getMaxExtinguishDistance() {
        return maxExtinguishDistance;
    }

    public int getMaxPower() {
        return maxPower;
    }

    public boolean isUseSpeak() {
        return useSpeak;
    }

    public long getThinkStartTime_() {
        return thinkStartTime_;
    }

    public void setThinkStartTime_(long thinkStartTime_) {
        this.thinkStartTime_ = thinkStartTime_;
    }

    public long getThinkTime() {
        return thinkTime;
    }

    public long getThinkTimeThreshold() {
        return thinkTimeThreshold;
    }

    /**
     * This method finds nearest refuge based on euclidean distance
     *
     * @param positionId entityID of the position to find nearest refuge based on it
     * @return EntityID of the nearest refuge; if there is no refuges, returns null
     */
    public EntityID findNearestRefuge(EntityID positionId) {

        Collection<StandardEntity> refuges = getRefuges();
        EntityID nearestID = null;
        int nearestDistance = Integer.MAX_VALUE;
        int tempDistance;
        if (positionId != null && refuges != null && !refuges.isEmpty()) {

            for (StandardEntity refugeEntity : refuges) {
                tempDistance = getDistance(refugeEntity.getID(), positionId);
                if (tempDistance < nearestDistance) {
                    nearestDistance = tempDistance;
                    nearestID = refugeEntity.getID();
                }
            }

        }

        return nearestID;
    }

    public List<StandardEntity> getEntities(List<EntityID> entityIDs) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (EntityID next : entityIDs) {
            result.add(getEntity(next));
        }
        return result;
    }

    public List<StandardEntity> getAreasInShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getAreas()) {
            Area area = (Area) next;
            if (shape.contains(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
    }

    public List<StandardEntity> getBuildingsInShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getBuildings()) {
            Area area = (Area) next;
            if (shape.contains(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
    }

    public List<StandardEntity> getAreasIntersectWithShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getAreas()) {
            Area area = (Area) next;
            if (shape.intersects(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
    }

    public Set<MrlBuilding> getEstimatedBurningBuildings() {
        return estimatedBurningBuildings;
    }

    public void setEstimatedBurningBuildings(Set<MrlBuilding> estimatedBurningBuildings) {
        this.estimatedBurningBuildings = estimatedBurningBuildings;
    }

    public <T extends StandardEntity> List<T> getEntitiesOfType(Class<T> c, StandardEntityURN urn) {
        Collection<StandardEntity> entities = getEntitiesOfType(urn);
        List<T> list = new ArrayList<T>();
        for (StandardEntity entity : entities) {
            if (c.isInstance(entity)) {
                list.add(c.cast(entity));
            }
        }
        return list;
    }

    public <T extends Area> List<T> getEntitiesOfType(Class<T> c, Collection<StandardEntity> entities) {
        List<T> list = new ArrayList<T>();
        for (StandardEntity entity : entities) {
            if (c.isInstance(entity)) {
                list.add(c.cast(entity));
            }
        }
        return list;
    }

    public boolean isCommunicationLow() {
        return isCommunicationLow;
    }

    public void setCommunicationLow(boolean communicationLow) {
        isCommunicationLow = communicationLow;
    }

    public boolean isCommunicationMedium() {
        return isCommunicationMedium;
    }

    public void setCommunicationMedium(boolean communicationMedium) {
        isCommunicationMedium = communicationMedium;
    }

    public boolean isCommunicationHigh() {
        return isCommunicationHigh;
    }

    public void setCommunicationHigh(boolean communicationHigh) {
        isCommunicationHigh = communicationHigh;
    }

    public boolean isBuildingBurnt(Building building) {
        if (building == null || !building.isFierynessDefined()) {
            return false;
        }
        int fieriness = building.getFieryness();

        return fieriness != 0 && fieriness != 4 && fieriness != 5;
    }

    public int getLastAfterShockTime() {
        return lastAfterShockTime;
    }

    public void setAftershockProperties(int lastAfterShockTime, int aftershockCount) {
        this.aftershockCount = aftershockCount;
        if (this.aftershockCount < aftershockCount) {
            this.aftershockCount = aftershockCount;
            if (getSelf() instanceof MrlPlatoonAgent) {
                platoonAgent.postAftershockAction();
            }
        }
    }

    public int getAftershockCount() {
        return aftershockCount;
    }
}
