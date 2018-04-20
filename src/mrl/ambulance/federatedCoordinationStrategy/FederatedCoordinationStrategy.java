package mrl.ambulance.federatedCoordinationStrategy;

import javolution.util.FastSet;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.ambulance.VictimClassifier;
import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.comparator.ConstantComparators;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.routing.path.Path;
import mrl.world.routing.pathPlanner.IPathPlanner;
import mrl.world.routing.pathPlanner.PathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

/**
 * @author Erfan Jazeb Nikoo
 */

public class FederatedCoordinationStrategy {
    private MrlWorld world;
    private MrlAmbulanceTeam me;
    private int numOfAmbulanceAgent;
    private int numOfPartitionPart;
    private boolean flag = true;
    private AmbulanceUtilities ambulanceUtilities;
    private AmbulanceConditionChecker ambulanceConditionChecker;
    private PathPlanner pathPlanner;
    private Set<StandardEntity> goodHumans = new FastSet<StandardEntity>();
    private List<Pair<EntityID, Integer>> ambulanceTeamList = new ArrayList<Pair<EntityID, Integer>>();
    private List<StandardEntity> centerOfPart = new ArrayList<StandardEntity>();
    private Map<Integer, ArrayList<Human>> halfOfAT = new HashMap<Integer, ArrayList<Human>>();
    private Map<Integer, ArrayList<Path>> zonePaths = new HashMap<Integer, ArrayList<Path>>();
    private Map<Integer, ArrayList<MrlZone>> listOfZones = new HashMap<Integer, ArrayList<MrlZone>>();
    private Map<Integer, ArrayList<MrlZone>> sortedZoneList = new HashMap<Integer, ArrayList<MrlZone>>();
    private Map<Integer, ArrayList<MrlZone>> zoneListOfPart = new HashMap<Integer, ArrayList<MrlZone>>();
    private Map<Integer, Integer> numOfZone = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> numOfAT = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> number = new HashMap<Integer, Integer>();
    private Map<Integer, Area> myCurrentTarget = new HashMap<Integer, Area>();
    private Map<Integer, Set<Area>> targets = new HashMap<Integer, Set<Area>>();
    private Map<Integer, Map<EntityID, Integer>> tempListOfTargets = new HashMap<Integer, Map<EntityID, Integer>>();
    private Map<Integer, Map<Integer, EntityID>> backupListOfTargets = new HashMap<Integer, Map<Integer, EntityID>>();
    private Map<Integer, Set<Building>> burningBuildingList = new HashMap<Integer, Set<Building>>();
    private Map<Integer, Set<Human>> victims = new HashMap<Integer, Set<Human>>();
    private Map<Integer, Set<Human>> deletedVictims = new HashMap<Integer, Set<Human>>();
    private Map<Integer, Set<Human>> humanVictims = new HashMap<Integer, Set<Human>>();
    private Map<Integer, Set<EntityID>> visitedBuilding = new HashMap<Integer, Set<EntityID>>();
    private Map<Integer, Set<Integer>> usedNumbers = new HashMap<Integer, Set<Integer>>();
    private Map<Integer, Set<Integer>> numOfPartitionsNeedHelp = new HashMap<Integer, Set<Integer>>();
    private Map<Integer, Boolean> isEmergencySituation = new HashMap<Integer, Boolean>();
    private Map<Integer, Map<EntityID, Integer>> secondTempListOfTargets = new HashMap<Integer, Map<EntityID, Integer>>();
    private Map<Integer, Map<Integer, EntityID>> secondBackupListOfTargets = new HashMap<Integer, Map<Integer, EntityID>>();
    private Map<Integer, Human> bestHuman = new HashMap<Integer, Human>();
    private Map<Integer, Double> bestHumanRate = new HashMap<Integer, Double>();
    private Map<Integer, Set<Area>> partitionArea = new HashMap<Integer, Set<Area>>();
    private Map<Integer, Set<MrlBuilding>> partitionBuilding = new HashMap<Integer, Set<MrlBuilding>>();
    private Map<Integer, Set<MrlBuilding>> backupPartitionBuilding = new HashMap<Integer, Set<MrlBuilding>>();
    private Map<Integer, Set<Area>> backupPartitionArea = new HashMap<Integer, Set<Area>>();
    private Map<Integer, Human> myTarget = new HashMap<Integer, Human>();
    private Map<Integer, Human> deletedTarget = new HashMap<Integer, Human>();
    private Map<Integer, ArrayList<EntityID>> halfOfAmbulanceTeam = new HashMap<Integer, ArrayList<EntityID>>();
    private VictimClassifier victimClassifier;

    public FederatedCoordinationStrategy(MrlWorld world, MrlAmbulanceTeam ambulanceTeam, AmbulanceUtilities ambulanceUtilities, AmbulanceConditionChecker ambulanceConditionChecker, IPathPlanner pathPlanner, VictimClassifier victimClassifier) {
        this.world = world;
        me = ambulanceTeam;
        this.pathPlanner = (PathPlanner) pathPlanner;
        this.ambulanceUtilities = ambulanceUtilities;
        this.ambulanceConditionChecker = ambulanceConditionChecker;
        numOfAmbulanceAgent = world.getAmbulanceTeams().size();
        this.victimClassifier = victimClassifier;
        initializeVariable();
    }

    /**
     * Make new every global map list and array list.
     */

    private void initializeVariable() {

        if (numOfAmbulanceAgent < 6) {
            numOfPartitionPart = 1;
        } else if (numOfAmbulanceAgent < 8 && numOfAmbulanceAgent > 5) {
            numOfPartitionPart = 2;
        } else {
            numOfPartitionPart = 4;
        }

        for (int i = 0; i < numOfPartitionPart; i++) {
            targets.put(i, new FastSet<Area>());
            sortedZoneList.put(i, new ArrayList<MrlZone>());
            halfOfAmbulanceTeam.put(i, new ArrayList<EntityID>());
            halfOfAT.put(i, new ArrayList<Human>());
            zonePaths.put(i, new ArrayList<Path>());
            listOfZones.put(i, new ArrayList<MrlZone>());
            zoneListOfPart.put(i, new ArrayList<MrlZone>());
            tempListOfTargets.put(i, new HashMap<EntityID, Integer>());
            backupListOfTargets.put(i, new HashMap<Integer, EntityID>());
            number.put(i, 0);
            myCurrentTarget.put(i, null);
            burningBuildingList.put(i, new FastSet<Building>());
            victims.put(i, new FastSet<Human>());
            humanVictims.put(i, new FastSet<Human>());
            visitedBuilding.put(i, new FastSet<EntityID>());
            usedNumbers.put(i, new FastSet<Integer>());
            numOfPartitionsNeedHelp.put(i, new FastSet<Integer>());
            isEmergencySituation.put(i, false);
            secondTempListOfTargets.put(i, new HashMap<EntityID, Integer>());
            secondBackupListOfTargets.put(i, new HashMap<Integer, EntityID>());
            deletedVictims.put(i, new FastSet<Human>());
            partitionArea.put(i, new FastSet<Area>());
            backupPartitionArea.put(i, new FastSet<Area>());
            partitionBuilding.put(i, new FastSet<MrlBuilding>());
            backupPartitionBuilding.put(i, new FastSet<MrlBuilding>());
            bestHumanRate.put(i, 0d);
        }
    }

    /**
     * @throws CommandException -> For move function
     */

    public void execute() throws CommandException {

        if (flag) {
            fillTargets();
            for (int i = 0; i < numOfPartitionPart; i++) {
                divideAmbulanceTeam(new Rectangle(divideMap(i)), i);
            }
            if (world.isCommunicationLess()) {
                System.out.println("Communication Less ... !");
            } else if (world.isCommunicationLow() || world.isCommunicationMedium()) {
                System.out.println("Communication Limited ... !");
            }
            flag = false;
        }

        goodHumans.clear();
        goodHumans.addAll(victimClassifier.getMyGoodHumans());

        for (int i = 0; i < numOfPartitionPart; i++) {
            fillVictims(i);

            if (myTarget.get(i) != null && !victims.get(i).isEmpty()) {
                if (!victims.get(i).contains(myTarget.get(i))) {
                    myTarget.put(i, null);
                    me.myTarget = null;
                    deletedTarget.put(i, null);
                    bestHuman.put(i, null);
                    bestHumanRate.put(i, 0d);
                } else {
                    if (ambulanceConditionChecker.isVisible(myTarget.get(i)) && ambulanceConditionChecker.isAlive(myTarget.get(i)) && ambulanceConditionChecker.needToBeHere(myTarget.get(i))) {
                        me.myTarget = myTarget.get(i);
                        return;
                    } else {
                        victims.get(i).remove(myTarget.get(i));
                        deletedTarget.put(i, myTarget.get(i));
                        myTarget.put(i, null);
                        me.myTarget = null;
                        bestHuman.put(i, null);
                        bestHumanRate.put(i, 0d);
                    }
                }
            }
        }

        for (int i = 0; i < numOfPartitionPart; i++) {
            for (int j = 0; j < numOfAT.get(i); j++) {
                if (me.getID().equals(halfOfAT.get(i).get(j).getID())) {
                    if (fillPartitionVisitedBuildings(i)) {
                        mergePartition(i);
                    } else {
                        emergencyMergePartition(i);
                    }
                }
            }

            for (int j = 0; j < numOfAT.get(i); j++) {
                if (me.getID().equals(halfOfAT.get(i).get(j).getID())) {
                    if (myCurrentTarget.get(i) != null && world.getSelfPosition().getID().equals(myCurrentTarget.get(i).getID())) {
                        if (saveCivilians((Area) world.getSelfPosition(), i, true)) {
                            return;
                        }
                    }
                }
            }

            chooseTarget(i);

            for (int j = 0; j < numOfAT.get(i); j++) {
                if (me.getID().equals(halfOfAT.get(i).get(j).getID())) {
                    if (world.getSelfPosition().getID().equals(myCurrentTarget.get(i).getID())) {
                        if (saveCivilians((Area) world.getSelfPosition(), i, true)) {
                            return;
                        }
                    }
                }
            }
            if (me.ambulanceConditionChecker.someoneOnBoard() == null) {
                for (int j = 0; j < numOfAT.get(i); j++) {
                    if (me.getID().equals(halfOfAT.get(i).get(j).getID())) {
                        me.move(myCurrentTarget.get(i), MRLConstants.IN_TARGET, false);
                    }
                }
            } else {
                return;
            }
        }
    }

    /**
     * @param num ->
     * @return ->
     */

    private void chooseTarget(int num) {
        int tryCount = 0;
        if (myCurrentTarget.get(num) != null) {
            rescueDecisionMaking(num);
            checkNumberEnd(num);
            updateBurningBuildings(num);
            while (isBurningBuilding(num, backupListOfTargets.get(num).get(number.get(num)))) {
                do {
                    if (tryCount == backupListOfTargets.get(num).size()) {
                        break;
                    }
                    usedNumbers.get(num).add(number.get(num));
                    number.put(num, number.get(num) + 1);
                    checkNumberEnd(num);
                    tryCount++;
                } while (usedNumbers.get(num).contains(number.get(num)));
            }
        }
        tryCount = 0;
        checkNumberEnd(num);
        myCurrentTarget.put(num, (Area) world.getEntity(backupListOfTargets.get(num).get(number.get(num))));

        if (bestHuman.get(num) != null) {
            myCurrentTarget.put(num, (Area) world.getEntity(bestHuman.get(num).getPosition()));
        }

        if (myCurrentTarget.get(num) != null) {
            if (!checkPassably(myCurrentTarget.get(num)) || world.getPlatoonAgent().isStuck() || usedNumbers.get(num).contains(number.get(num))) {
                do {
                    if (tryCount == backupListOfTargets.get(num).size()) {
                        break;
                    }
                    number.put(num, number.get(num) + 1);
                    checkNumberEnd(num);
                    tryCount++;
                }
                while (usedNumbers.get(num).contains(number.get(num)) ||
                        !checkPassably((Area) world.getEntity(backupListOfTargets.get(num).get(number.get(num)))));
                myCurrentTarget.put(num, (Area) world.getEntity(backupListOfTargets.get(num).get(number.get(num))));
            }
        }
    }

    /**
     * @param target ->
     * @return ->
     */

    private boolean checkPassably(Area target) {
        return !pathPlanner.planMove(target, MRLConstants.IN_TARGET, false).isEmpty();
    }

    /**
     * Fill every variable with functions.
     */

    private void fillTargets() {

        int counter;
        Human human;
        EntityID entranceID;

        if (numOfAmbulanceAgent < 6) {
            numOfPartitionPart = 1;
        } else if (numOfAmbulanceAgent < 8 && numOfAmbulanceAgent > 5) {
            numOfPartitionPart = 2;
        } else {
            numOfPartitionPart = 4;
        }

        for (int i = 0; i < numOfPartitionPart; i++) {
            centerOfPart.add(rendezvous(new Rectangle(divideMap(i))));
            listOfZones.get(i).addAll(partOfMapZones(new Rectangle(divideMap(i)), i));
            numOfZone.put(i, listOfZones.get(i).size());
            zoneSortCalc(i);
            counter = 0;
            for (int j = 0; j < sortedZoneList.get(i).size(); j++) {
                targets.get(i).addAll(getZoneEntrance(sortedZoneList.get(i).get(j), i));
            }
            partitionArea.get(i).addAll(targets.get(i));
            backupPartitionArea.get(i).addAll(targets.get(i));

            for (Area area : targets.get(i)) {
                tempListOfTargets.get(i).put(area.getID(), counter);
                backupListOfTargets.get(i).put(counter, area.getID());
                secondTempListOfTargets.get(i).put(area.getID(), counter);
                secondBackupListOfTargets.get(i).put(counter, area.getID());
                counter++;
            }

            for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
                human = (Human) standardEntity;
                if (world.getEntity(human.getPosition()) instanceof Building) {
                    entranceID = world.getMrlBuilding(world.getEntity(human.getPosition()).getID()).getEntrances().get(0).getNeighbour().getID();
                    if (backupListOfTargets.get(i).values().contains(entranceID)) {
                        humanVictims.get(i).add(human);
                    }
                    if (backupListOfTargets.get(i).values().contains(human.getPosition())) {
                        if (human.isBuriednessDefined() && human.isDamageDefined() && human.isPositionDefined()) {
                            victims.get(i).add(human);
                        }
                    }
                }
            }

            for (StandardEntity standardEntity : world.getPoliceForces()) {
                human = (Human) standardEntity;
                if (world.getEntity(human.getPosition()) instanceof Building) {
                    entranceID = world.getMrlBuilding(world.getEntity(human.getPosition()).getID()).getEntrances().get(0).getNeighbour().getID();
                    if (backupListOfTargets.get(i).values().contains(entranceID)) {
                        humanVictims.get(i).add(human);
                    }
                }
                if (backupListOfTargets.get(i).values().contains(human.getPosition())) {
                    if (human.isBuriednessDefined() && human.isDamageDefined() && human.isPositionDefined()) {
                        victims.get(i).add(human);
                    }
                }
            }

            for (StandardEntity standardEntity : world.getFireBrigades()) {
                human = (Human) standardEntity;
                if (world.getEntity(human.getPosition()) instanceof Building) {
                    entranceID = world.getMrlBuilding(world.getEntity(human.getPosition()).getID()).getEntrances().get(0).getNeighbour().getID();
                    if (backupListOfTargets.get(i).values().contains(entranceID)) {
                        humanVictims.get(i).add(human);
                    }
                }
                if (backupListOfTargets.get(i).values().contains(human.getPosition())) {
                    if (human.isBuriednessDefined() && human.isDamageDefined() && human.isPositionDefined()) {
                        victims.get(i).add(human);
                    }
                }
            }
        }

    }

    /**
     * @param num ->
     */

    private void rescueDecisionMaking(int num) {
        EntityID entranceID;
        for (int j = 0; j < numOfAT.get(num); j++) {
            if (me.getID().equals(halfOfAT.get(num).get(j).getID())) {
                bestHuman.put(num, bestVictimChosenWithRate(num));
                if (bestHuman.get(num) != null && world.getEntity(bestHuman.get(num).getPosition()) instanceof Building) {
                    entranceID = world.getMrlBuilding(world.getEntity(bestHuman.get(num).getPosition()).getID()).getEntrances().get(0).getNeighbour().getID();
                    if (tempListOfTargets.get(num).get(entranceID) != null) {
                        number.put(num, tempListOfTargets.get(num).get(entranceID));
                    } else if (tempListOfTargets.get(num).get(bestHuman.get(num).getPosition()) != null) {
                        number.put(num, tempListOfTargets.get(num).get(bestHuman.get(num).getPosition()));
                    } else {
                        search(num, halfOfAT.get(num).get(j).getPosition());
                        bestHuman.put(num, null);
                        bestHumanRate.put(num, 0d);
                    }
                } else {
                    search(num, halfOfAT.get(num).get(j).getPosition());
                    bestHuman.put(num, null);
                    bestHumanRate.put(num, 0d);
                }
            }
        }
    }

    /**
     * @param num ->
     * @return ->
     */

    private Human bestVictimChosenWithRate(int num) {
        int buried, damage, cycleToArrive, agent, density;
        double damageRate = 1;
        double cycleToArriveRate;
        double agentRate;
        double densityRate = 1.5;
        double totalRate;
        double bestRate;
        if (world.isMapHuge()) {
            cycleToArriveRate = 0.1;
        } else {
            cycleToArriveRate = 0.04;
        }
        if (!world.getRefuges().isEmpty()) {
            bestRate = Double.MIN_VALUE;
        } else {
            bestRate = Double.MAX_VALUE;
        }
        Map<Integer, Human> bestPerson = new HashMap<Integer, Human>();

        for (Human human : victims.get(num)) {
            if (!checkPassably((Area) world.getEntity(human.getPosition()))) {
                continue;
            }
            buried = human.getBuriedness();
            if (buried == 0) {
                buried = 1;
            }
            damage = human.getDamage();
            cycleToArrive = ambulanceUtilities.approximatingTTM(world.getSelfPosition(), human.getPosition(world));
            if (human instanceof Civilian) {
                agent = 1;
                agentRate = 1;
            } else {
                agent = 1;
                agentRate = (double) (400 - world.getTime()) / 400;
            }
            density = getBuildingDensity(num, human);

            totalRate = (((double) damage / buried) * damageRate) - (cycleToArrive * cycleToArriveRate) + (agent * agentRate) + (pow(density * densityRate, 2));

            if (world.getRefuges().isEmpty()) {
                totalRate = totalRate * -1;
            }

            if (totalRate >= bestRate) {
                bestRate = totalRate;
                bestPerson.put(num, human);
            }
        }

        if (bestRate > bestHumanRate.get(num) * 2 || bestHuman.get(num) != null && !ambulanceConditionChecker.isAlive(bestHuman.get(num)) && !ambulanceConditionChecker.isVisible(bestHuman.get(num))) {
            bestHumanRate.put(num, bestRate);
            return bestPerson.get(num);
        } else {
            return bestHuman.get(num);
        }
    }

    /**
     * @param num ->
     */

    private void fillVictims(int num) {
        EntityID entranceID;
        Human human;
        for (StandardEntity standardEntity : goodHumans) {
            human = (Human) standardEntity;
            if (world.getEntity(human.getPosition()) instanceof Building) {
                entranceID = world.getMrlBuilding(world.getEntity(human.getPosition()).getID()).getEntrances().get(0).getNeighbour().getID();
                if (backupListOfTargets.get(num).values().contains(entranceID)) {
                    if (human.isBuriednessDefined() && human.isDamageDefined() && human.isPositionDefined()) {
                        victims.get(num).add(human);
                    }
                }
                if (backupListOfTargets.get(num).values().contains(human.getPosition())) {
                    if (human.isBuriednessDefined() && human.isDamageDefined() && human.isPositionDefined()) {
                        victims.get(num).add(human);
                    }
                }
            }
        }

        for (Human human1 : humanVictims.get(num)) {
            if (human1.isPositionDefined()) {
                if (!human1.isBuriednessDefined()) {
                    human1.setBuriedness(1);
                }
                if (!human1.isDamageDefined()) {
                    human1.setDamage(1);
                }
                if (!human1.isHPDefined()) {
                    human1.setHP(10000);
                }
                victims.get(num).add(human1);
            }
        }

        if (world.isCommunicationLess()) {
            Civilian civilian;
            for (EntityID entityID : world.getFullBuildings()) {
                civilian = (Civilian) world.getEntity(entityID);
                if (world.getEntity(civilian.getPosition()) instanceof Building) {
                    entranceID = world.getMrlBuilding(world.getEntity(civilian.getPosition()).getID()).getEntrances().get(0).getNeighbour().getID();
                    if (backupListOfTargets.get(num).values().contains(entranceID)) {
                        if (civilian.isBuriednessDefined() && civilian.isDamageDefined() && civilian.isPositionDefined()) {
                            victims.get(num).add(civilian);
                        }
                    }
                    if (backupListOfTargets.get(num).values().contains(civilian.getPosition())) {
                        if (civilian.isBuriednessDefined() && civilian.isDamageDefined() && civilian.isPositionDefined()) {
                            victims.get(num).add(civilian);
                        }
                    }
                }
            }
        }

        removeVictims(num, victims.get(num));

    }

    /**
     * @param num    ->
     * @param humans ->
     * @return ->
     */

    private void removeVictims(int num, Set<Human> humans) {

        Road entranceRoad;
        Building building;
        Set<Human> remove = new FastSet<Human>();

        if (humans == null) {
            return;
        }

        for (Human human : humans) {
            if (human == null) {
                continue;
            }
            if (human.isPositionDefined() && world.getEntity(human.getPosition()) instanceof Building) {
                building = (Building) world.getEntity(human.getPosition());
                entranceRoad = world.getMrlBuilding(building.getID()).getEntrances().get(0).getNeighbour();
                if (backupListOfTargets.get(num).values().contains(human.getPosition())) {
                    remove.addAll(checkRemoveCases(num, human, human.getPosition()));
                } else if (backupListOfTargets.get(num).values().contains(entranceRoad.getID())) {
                    remove.addAll(checkRemoveCases(num, human, human.getPosition()));
                }
            } else {
                remove.add(human);
                deletedVictims.get(num).add(human);
            }

            if (deletedTarget.get(num) == human) {
                remove.add(human);
            }
            if (deletedVictims.get(num).contains(human)) {
                remove.add(human);
            }
        }
        victims.get(num).removeAll(remove);
    }

    /**
     * @param num    ->
     * @param human  ->
     * @param target ->
     * @return ->
     */

    private Set<Human> checkRemoveCases(int num, Human human, EntityID target) {

        Set<Human> remove = new FastSet<Human>();

        if (world.getSelfPosition().getID().equals(target)) {
            if (!saveCivilians((Area) world.getEntity(target), num, false)) {
                remove.add(human);
                deletedVictims.get(num).add(human);
            }
        }
        if ((!(human instanceof Civilian)) && human.isBuriednessDefined() && human.getBuriedness() == 0) {
            remove.add(human);
            deletedVictims.get(num).add(human);
        }
        if (!ambulanceUtilities.isAlivable(human, numOfAT.get(num))) {
            remove.add(human);
            deletedVictims.get(num).add(human);
        }
        if (human.isHPDefined() && human.getHP() < 500) {
            remove.add(human);
            deletedVictims.get(num).add(human);
        }
        if (world.getEntity(human.getPosition()) instanceof Refuge) {
            remove.add(human);
            deletedVictims.get(num).add(human);
        }
        return remove;
    }

    /**
     * @param num   ->
     * @param human ->
     * @return ->
     */

    private int getBuildingDensity(int num, Human human) {
        int counter = 0;
        for (Human human1 : victims.get(num)) {
            if (human.getPosition().equals(human1.getPosition())) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * @param num->
     * @param position ->
     */

    private void search(int num, EntityID position) {

        if (position.equals(myCurrentTarget.get(num).getID())) {
            do {
                number.put(num, number.get(num) + 1);
                checkNumberEnd(num);
            }
            while (usedNumbers.get(num).contains(number.get(num)));
        }
    }

    /**
     * @param num ->
     * @return ->
     */

    private Boolean fillPartitionVisitedBuildings(int num) {
        MrlBuilding mrlBuilding;
        Road entranceRoad;
        for (EntityID entityID : world.getVisitedBuildings()) {
            mrlBuilding = world.getMrlBuilding(entityID);
            entranceRoad = mrlBuilding.getEntrances().get(0).getNeighbour();
            if (backupListOfTargets.get(num).values().contains(entranceRoad.getID())) {
                visitedBuilding.get(num).add(mrlBuilding.getID());
            }
            if (backupListOfTargets.get(num).values().contains(entityID)) {
                visitedBuilding.get(num).add(mrlBuilding.getID());
            }
        }
        for (Human human : victims.get(num)) {
            visitedBuilding.get(num).remove(human.getPosition());
            mrlBuilding = world.getMrlBuilding(human.getPosition());
            entranceRoad = mrlBuilding.getEntrances().get(0).getNeighbour();
            if (tempListOfTargets.get(num).containsKey(human.getPosition())) {
                usedNumbers.get(num).remove(tempListOfTargets.get(num).get(human.getPosition()));
            }
            if (tempListOfTargets.get(num).containsKey(entranceRoad.getID())) {
                usedNumbers.get(num).remove(tempListOfTargets.get(num).get(entranceRoad.getID()));
            }
        }
        for (EntityID entityID : visitedBuilding.get(num)) {
            mrlBuilding = world.getMrlBuilding(entityID);
            entranceRoad = mrlBuilding.getEntrances().get(0).getNeighbour();
            if (tempListOfTargets.get(num).containsKey(entranceRoad.getID())) {
                usedNumbers.get(num).add(tempListOfTargets.get(num).get(entranceRoad.getID()));
            }
            if (tempListOfTargets.get(num).containsKey(entityID)) {
                usedNumbers.get(num).add(tempListOfTargets.get(num).get(entityID));
            }
        }
        return abs(usedNumbers.get(num).size() - backupListOfTargets.get(num).size()) < 25;
    }

    /**
     * @param num ->
     */

    private void mergePartition(int num) {
        Set<EntityID> entityIDs = new FastSet<EntityID>();
        int counter = 0;

        backupListOfTargets.get(num).clear();
        tempListOfTargets.get(num).clear();
        for (int i = 0; i < numOfPartitionPart; i++) {
            entityIDs.addAll(backupListOfTargets.get(i).values());
            entityIDs.removeAll(visitedBuilding.get(i));
            victims.get(num).addAll(victims.get(i));
            partitionArea.get(num).addAll(partitionArea.get(i));
            partitionBuilding.get(num).addAll(partitionBuilding.get(i));
            visitedBuilding.get(num).addAll(visitedBuilding.get(i));
            usedNumbers.get(num).addAll(usedNumbers.get(i));
        }

        for (EntityID entityID : entityIDs) {
            backupListOfTargets.get(num).put(counter, entityID);
            tempListOfTargets.get(num).put(entityID, counter);
            counter++;
        }

    }

    /**
     * @param num ->
     * @return ->
     */

    private boolean isEmergencySituation(int num) {

        int emergencyRate = 2;

        if (world.getTime() > 40) {
            if (victims.get(num).isEmpty()) {
                for (int i = 0; i < numOfPartitionPart; i++) {
                    if (i == num) {
                        continue;
                    }
                    if (victims.get(i).size() / numOfAT.get(i) >= emergencyRate) {
                        numOfPartitionsNeedHelp.get(num).add(i);
                    }
                }
            }
        }

        return !numOfPartitionsNeedHelp.get(num).isEmpty();

    }

    /**
     * @param num ->
     */

    private void emergencyMergePartition(int num) {

        Set<EntityID> entityIDs = new FastSet<EntityID>();
        int counter = 0;

        if (!isEmergencySituation.get(num)) {
            isEmergencySituation.put(num, isEmergencySituation(num));
        } else {
            entityIDs.addAll(backupListOfTargets.get(num).values());
            for (Integer integer : numOfPartitionsNeedHelp.get(num)) {
                entityIDs.addAll(backupListOfTargets.get(integer).values());
                victims.get(num).addAll(victims.get(integer));
                partitionArea.get(num).addAll(partitionArea.get(integer));
                partitionBuilding.get(num).addAll(partitionBuilding.get(integer));
            }
            for (EntityID entityID : entityIDs) {
                backupListOfTargets.get(num).put(counter, entityID);
                tempListOfTargets.get(num).put(entityID, counter);
                counter++;
            }
            if (victims.get(num).isEmpty()) {
                for (Integer integer : numOfPartitionsNeedHelp.get(num)) {
                    victims.get(num).removeAll(victims.get(integer));
                }
                backupListOfTargets.get(num).clear();
                tempListOfTargets.get(num).clear();
                partitionArea.get(num).clear();
                partitionBuilding.get(num).clear();
                partitionBuilding.get(num).addAll(backupPartitionBuilding.get(num));
                partitionArea.get(num).addAll(backupPartitionArea.get(num));
                backupListOfTargets.get(num).putAll(secondBackupListOfTargets.get(num));
                tempListOfTargets.get(num).putAll(secondTempListOfTargets.get(num));
                isEmergencySituation.put(num, false);
            }
        }

    }

    /**
     * @param num ->
     */

    private void checkNumberEnd(int num) {
        if (backupListOfTargets.get(num).size() <= number.get(num) || number.get(num) == null) {
            number.put(num, 0);
        }
    }

    /**
     * @param num    ->
     * @param areaID ->
     * @return ->
     */

    private boolean isBurningBuilding(int num, EntityID areaID) {

        MrlBuilding mrlBuilding;
        for (Building building : burningBuildingList.get(num)) {
            if (world.getEntity(areaID) instanceof Building) {
                if (building.getID().equals(areaID)) {
                    return true;
                }
            } else {
                mrlBuilding = world.getMrlBuilding(building.getID());
                for (Entrance entrance : mrlBuilding.getEntrances()) {
                    if (entrance.getNeighbour().getID().equals(areaID)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param num ->
     */

    private void updateBurningBuildings(int num) {
        Building building;
        for (MrlBuilding mrlBuilding : partitionBuilding.get(num)) {
            building = mrlBuilding.getSelfBuilding();
            if (building.isFierynessDefined() && building.isTemperatureDefined()) {
                if (building.getFieryness() > 0 || building.getTemperature() > 0) {
                    burningBuildingList.get(num).add(building);
                }
            }
        }
    }

    /**
     * Divide map to four same rectangle.
     *
     * @param num -> Index of part.
     * @return Rectangle -> Half of map.
     */

    private Rectangle divideMap(int num) {

        int mapHeight = (int) world.getBounds().getHeight();
        int mapWidth = (int) world.getBounds().getWidth();
        int minX = (int) world.getBounds().getMinX();
        int minY = (int) world.getBounds().getMinY();
        int maxX = (int) world.getBounds().getMaxX();
        int maxY = (int) world.getBounds().getMaxY();
        Map<Integer, Rectangle> part = new HashMap<Integer, Rectangle>();

        if (numOfAmbulanceAgent < 6) {
            part.put(0, new Rectangle(minX, minY, mapWidth, mapHeight));
        } else if (numOfAmbulanceAgent < 8 && numOfAmbulanceAgent > 5) {
            part.put(0, new Rectangle(minX, minY, mapWidth / 2, mapHeight));
            part.put(1, new Rectangle(maxX / 2, minY, mapWidth / 2, mapHeight));
        } else {
            part.put(0, new Rectangle(minX, minY, mapWidth / 2, mapHeight / 2));
            part.put(1, new Rectangle(minX, maxY / 2, mapWidth / 2, mapHeight / 2));
            part.put(2, new Rectangle(maxX / 2, minY, mapWidth / 2, mapHeight / 2));
            part.put(3, new Rectangle(maxX / 2, maxY / 2, mapWidth / 2, mapHeight / 2));
        }

        return part.get(num);
    }

    /**
     * Divide ambulance team to four same group. Sort of half of ambulance agent with distance to center of first Part and add to first list and another ambulance agents add to second list.
     *
     * @param partOfMap -> Part of map which one of ambulance team wants to do search and rescue.
     * @param num       -> Index of ambulance group.
     * @global numOfAmbulanceAgent -> Number of ambulance team agents.
     * @global ambulanceTeamList -> Temporary list of ambulance team for change and sort them.
     * @global halfOfAmbulanceTeam -> This is a map list of ambulance group.In this map list, key is ambulance group index and value is half of ambulance agents.
     */

    private void divideAmbulanceTeam(Rectangle partOfMap, int num) {

        Point center1;
        Human human;
        int counter;
        ArrayList<Pair<EntityID, Integer>> ambulanceTeamList1 = new ArrayList<Pair<EntityID, Integer>>();

        if (num == 0) {
            counter = 0;
            center1 = new Point((int) partOfMap.getCenterX(), (int) partOfMap.getCenterY());

            for (StandardEntity ambulanceAgent : world.getAmbulanceTeams()) {
                human = (Human) ambulanceAgent;
                ambulanceTeamList1.add(new Pair<EntityID, Integer>(human.getID(), (int) center1.distance(human.getX(), human.getY())));
                counter++;
            }

            Collections.sort(ambulanceTeamList1, ConstantComparators.DISTANCE_VALUE_COMPARATOR);

            ambulanceTeamList.clear();
            ambulanceTeamList.addAll(ambulanceTeamList1);

            for (int i = 0; i < (numOfAmbulanceAgent / numOfPartitionPart); i++) {
                ambulanceTeamList.remove(0);
            }

            halfOfAmbulanceTeam.get(0).clear();
            for (int i = 0; i < (numOfAmbulanceAgent / numOfPartitionPart); i++) {
                halfOfAmbulanceTeam.get(0).add(ambulanceTeamList1.get(i).first());
            }
            counter = 0;
            for (EntityID entityID : halfOfAmbulanceTeam.get(0)) {
                for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfAT.get(0).add(counter, human);
                        counter++;
                    }
                }
            }
            numOfAT.put(num, halfOfAT.get(num).size());

        } else if (num == 1) {
            halfOfAmbulanceTeam.get(1).clear();
            for (int i = 0; i < ambulanceTeamList.size() / 3; i++) {
                halfOfAmbulanceTeam.get(1).add(ambulanceTeamList.get(i).first());
            }

            for (int i = 0; i < halfOfAmbulanceTeam.get(num).size(); i++) {
                ambulanceTeamList.remove(0);
            }

            counter = 0;
            for (EntityID entityID : halfOfAmbulanceTeam.get(1)) {
                for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfAT.get(1).add(counter, human);
                        counter++;
                    }
                }
            }
            numOfAT.put(num, halfOfAT.get(num).size());
        } else if (num == 2) {
            halfOfAmbulanceTeam.get(2).clear();
            for (int i = 0; i < ambulanceTeamList.size() / 2; i++) {
                halfOfAmbulanceTeam.get(2).add(ambulanceTeamList.get(i).first());
            }

            for (int i = 0; i < halfOfAmbulanceTeam.get(num).size(); i++) {
                ambulanceTeamList.remove(0);
            }

            counter = 0;
            for (EntityID entityID : halfOfAmbulanceTeam.get(2)) {
                for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfAT.get(2).add(counter, human);
                        counter++;
                    }
                }
            }
            numOfAT.put(num, halfOfAT.get(num).size());
        } else if (num == 3) {
            halfOfAmbulanceTeam.get(3).clear();
            for (int i = 0; i < ambulanceTeamList.size(); i++) {
                halfOfAmbulanceTeam.get(3).add(ambulanceTeamList.get(i).first());
            }

            for (int i = 0; i < halfOfAmbulanceTeam.get(num).size(); i++) {
                ambulanceTeamList.remove(0);
            }

            counter = 0;
            for (EntityID entityID : halfOfAmbulanceTeam.get(3)) {
                for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
                    human = (Human) standardEntity;
                    if (human.getID().getValue() == entityID.getValue()) {
                        halfOfAT.get(3).add(counter, human);
                        counter++;
                    }
                }
            }
            numOfAT.put(num, halfOfAT.get(num).size());
        }
    }

    /**
     * Choose nearest road to center of part of map.
     *
     * @param partOfMap -> Part of map which one of ambulance team wants to do search and rescue.
     * @return rendezvous -> return nearest standard entity to center of part of map.
     */

    private StandardEntity rendezvous(Rectangle partOfMap) {

        Point center;
        Road road;
        double distance;
        double temp = Double.MAX_VALUE;
        StandardEntity nearestRoad = null;

        center = new Point((int) partOfMap.getCenterX(), (int) partOfMap.getCenterY());

        for (StandardEntity standardEntity : world.getRoads()) {
            road = (Road) standardEntity;
            distance = center.distance(road.getX(), road.getY());
            if (distance < temp) {
                temp = distance;
                nearestRoad = road;
            }
        }

        return nearestRoad;
    }

    /**
     * Divide zones and add all of them to two array list in one map list.
     *
     * @param partOfMap -> Part of map which one of ambulance team wants to do search and rescue.
     * @param num       -> Index of ambulance group.
     * @return ArrayList<MrlZone> -> List of half of zones in map.
     */

    private ArrayList<MrlZone> partOfMapZones(Rectangle partOfMap, int num) {

        for (MrlZone zone : world.getZones()) {
            if (partOfMap.contains(zone.getCenter())) {
                zoneListOfPart.get(num).add(zone);
            }
        }
        return zoneListOfPart.get(num);
    }

    /**
     * Choose nearest zone to center of part of map as first zone and calculate next zone with neighbour zone algorithm.
     *
     * @param num -> Index of ambulance group.
     */

    private void zoneSortCalc(int num) {

        Road centerOfPartRoad;
        centerOfPartRoad = new Road((Road) centerOfPart.get(num));
        Point point;
        point = new Point(centerOfPartRoad.getX(), centerOfPartRoad.getY());
        Map<Integer, MrlZone> bestZone = new HashMap<Integer, MrlZone>();
        double temp = Double.MAX_VALUE;

        for (int i = 0; i < numOfZone.get(num); i++) {
            for (MrlZone zone : listOfZones.get(num)) {
                if (point.distance(zone.getCenter()) < temp) {
                    temp = point.distance(zone.getCenter());
                    bestZone.put(num, zone);
                }
            }
            listOfZones.get(num).remove(bestZone.get(num));
            point = new Point(bestZone.get(num).getCenter());
            temp = Double.MAX_VALUE;
            sortedZoneList.get(num).add(bestZone.get(num));
        }
    }

    /**
     * This function take the path of zone and return a neighbour path in that zone.
     *
     * @param firstPath -> ‌Beginning path for calculate next path in same zone.
     * @param num       -> Index of ambulance group.
     * @return Path -> Neighbour path with first path in same zone.
     */

    private Path getZoneNeighbourPath(Path firstPath, int num) {

        ArrayList<Path> neighbours = new ArrayList<Path>();

        zonePaths.get(num).remove(firstPath);
        for (Path path : zonePaths.get(num)) {
            for (Path p : firstPath.getNeighbours()) {
                if (path.getId().getValue() == p.getId().getValue()) {
                    neighbours.add(path);
                }
            }
        }
        if (neighbours.isEmpty()) {
            neighbours.addAll(zonePaths.get(num));
        }
        return neighbours.get(0);

    }

    /**
     * This function take the zone of one part of map and return list of sorted entrance roads in that zone.
     *
     * @param zone -> Zone of part of map which we need that entrance roads.
     * @param num  -> Index of ambulance group.
     * @return ArrayList<Road> -> list of sorted entrance roads in zone.
     */

    private ArrayList<Area> getZoneEntrance(MrlZone zone, int num) {

        ArrayList<Path> neighbourPaths = new ArrayList<Path>();
        ArrayList<Area> areas = new ArrayList<Area>();
        ArrayList<Entrance> zoneEntrances = new ArrayList<Entrance>();
        Road road;
        Set<Area> shouldCheckInsideBuilding = new FastSet<Area>();
        Set<Road> badEntrance = new FastSet<Road>();

        Path tempPath = zone.getPaths().get(0);

        zonePaths.get(num).clear();
        zonePaths.get(num).addAll(zone.getPaths());
        neighbourPaths.add(0, zone.getPaths().get(0));
        for (int i = 0; i < (zone.getPaths().size() - 1); i++) {
            tempPath = getZoneNeighbourPath(tempPath, num);
            neighbourPaths.add(tempPath);
        }
        Area area;
        for (MrlBuilding building : zone) {
            zoneEntrances.addAll(building.getEntrances());
            partitionBuilding.get(num).add(building);
            backupPartitionBuilding.get(num).add(building);
            for (MrlBuilding mrlBuilding : world.getShouldCheckInsideBuildings()) {
                if (mrlBuilding.getID().equals(building.getID())) {
                    area = (Area) world.getEntity(building.getID());
                    shouldCheckInsideBuilding.add(area);
                    for (Entrance entrance : mrlBuilding.getEntrances()) {
                        badEntrance.add(entrance.getNeighbour());
                    }
                }
            }
        }
        for (Path nPath : neighbourPaths) {
            for (int j = 0; j < nPath.getEntrances().size(); j++) {
                if (zoneEntrances.contains(nPath.getEntrances().get(j))) {
                    road = new Road(nPath.getEntrances().get(j).getNeighbour());
                    areas.add(road);
                }
            }
        }
        areas.addAll(shouldCheckInsideBuilding);
        areas.removeAll(badEntrance);

        return areas;
    }

    /**
     * This function take current entrance road and see in neighbour building and check civilian in that building.
     *
     * @param myCurrentArea -> My current entrance road.
     * @param num           -> Index of ambulance group.
     * @param rescue        -> .
     * @return -> Is civilian in neighbour building of my current entrance road? If yes return true else retur false.
     */

    private Boolean saveCivilians(Area myCurrentArea, int num, boolean rescue) {

        for (Human human : victims.get(num)) {

            if (!rescue) {
                if (ambulanceConditionChecker.isVisible(human) && ambulanceConditionChecker.isAlive(human) && human.getPosition().equals(myCurrentArea.getID())) {
                    return true;
                }
//                for (EntityID entityID : myCurrentArea.getNeighboursByEdge()) {
//                    if (ambulanceConditionChecker.isVisible(human) && ambulanceConditionChecker.isAlive(human) && human.getPosition().equals(entityID)) {
//                        return true;
//                    }
//                }
            } else {
                if (ambulanceConditionChecker.isVisible(human) && ambulanceConditionChecker.isAlive(human) && human.getPosition().equals(myCurrentArea.getID())) {
                    if (rescue) {
                        myTarget.put(num, human);
                        me.myTarget = human;
                    }
                    return true;
                }
            }
        }
        return false;
    }
}

