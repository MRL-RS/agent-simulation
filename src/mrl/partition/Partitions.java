package mrl.partition;


import javolution.util.FastMap;
import mrl.common.Util;
import mrl.helper.HumanHelper;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by  P.D.G.
 * User: root
 * Date: Dec 7, 2009
 * Time: 3:25:55 PM
 */
public class Partitions extends ArrayList<Partition> implements PartitionsI {

    private MrlWorld world;
    private Human self;
    private int myPartitionID = -1;

    private int numberOfPartitions;
    private Map<StandardEntity, Partition> humanPartitionMap;
    private ArrayList<Partition> mapPartitions;

    Partition WORLD_PARTITION;

    private int eachPartitionWidth;
    private int eachPartitionHeight;


    // Am I a leader for my Partition or not?; in our strategy police forces are leaders
    private Boolean leader = false;

    public Partition getMyPartition() {
        if (humanPartitionMap.get(self).getId() >= 0) {
            // this.get(myPartitionID).updateBuildingConditions();
            return humanPartitionMap.get(self);
        } else {
            return WORLD_PARTITION;
        }
    }

    public Boolean isLeader() {
        return leader;
    }

    public Partitions(MrlWorld world, Human self) {
        this.world = world;
        this.self = self;
        humanPartitionMap = new FastMap<StandardEntity, Partition>();
        mapPartitions = new ArrayList<Partition>();


        makePartitions();

        Polygon polygon = new Polygon();
        polygon.addPoint(world.getMinX(), world.getMinY());
        polygon.addPoint(world.getMaxX(), world.getMinY());
        polygon.addPoint(world.getMaxX(), world.getMaxY());
        polygon.addPoint(world.getMinX(), world.getMaxY());


        WORLD_PARTITION = new Partition(10, polygon, world);

//        MrlPartitionsLayer.PARTITIONS.addAll(this);

//        for (Partition partition : this) {
//            for (RendezvousI rendezvousI : partition.getRendezvous()) {
//                MrlRendezvousLayer.partitions.addAll(rendezvousI.getRodes());
//            }
//        }
    }


    private void makePartitions() {
        makeFrame();// todo consider map rotation!!!!
        assignProperAgentsToEachPartition();
//        createRendezvous();
    }

    //
    // Partition structure
    //
    //   [ 6   5   4
    //     1   2   3 ]
    //

    private void makeFrame() {


        // find the partition dimensions
        int rowNums = Integer.parseInt(getHowToPartitionTheMap().substring(0, 1));
        int columnNums = Integer.parseInt(getHowToPartitionTheMap().substring(1, 2));
        numberOfPartitions = columnNums * rowNums;

        // 1 is added for overlapping between partitions
        eachPartitionWidth = world.getMapWidth() / columnNums + 1;
        eachPartitionHeight = world.getMapHeight() / rowNums + 1;
        int tempX = world.getMinX();
        int tempY = world.getMinY();

        int sumOfBuildings = 0;

        for (int i = 0; i < numberOfPartitions; i++) {
            if (i % columnNums == 0 && i != 0) {
//                tempX = world.getMinX();
                tempX = world.getMinX();
                tempY += eachPartitionHeight;
            }

            Polygon polygon = new Polygon();
            polygon.addPoint(tempX, tempY);
            polygon.addPoint(tempX + eachPartitionWidth, tempY);
            polygon.addPoint(tempX + eachPartitionWidth, tempY + eachPartitionHeight);
            polygon.addPoint(tempX, tempY + eachPartitionHeight);

            Partition pD = new Partition(i, polygon, world);
            pD.setWidth(eachPartitionWidth);
            pD.setHeight(eachPartitionHeight);

            tempX += eachPartitionWidth;

//            sumOfBuildings+=pD.getBuildings().size();
            mapPartitions.add(pD);
//            idPartitionMap.put(pD.getId(), pD);
        }

//       check unassigned path in partitions and assigned them to nearest partition


        prunePartitions(mapPartitions);
        numberOfPartitions = mapPartitions.size();
        assignUnAssigedPaths();
        assignUnAssigedBuildings();

        this.addAll(mapPartitions);


        Collections.sort(this, Partition.Partition_IDComparator);
    }

    private void prunePartitions(ArrayList<Partition> mapPartitions) {
        int meanNumberOfBuildings = world.getBuildings().size() / mapPartitions.size();
        List<Partition> shouldRemovePartitions = new ArrayList<Partition>();
        for (Partition partition : mapPartitions) {
            if (!shouldRemovePartitions.contains(partition) && partition.getBuildings().size() < 0.65 * meanNumberOfBuildings) {
                shouldRemovePartitions.add(partition);
                assigneObjectsOfPrunedPartitionsToOthers(partition, shouldRemovePartitions);

            }
        }

        mapPartitions.removeAll(shouldRemovePartitions);
//        assigneObjectsOfPrunedPartitionsToOthers(shouldRemove);
        reNumberingPartitions();

    }

    private void reNumberingPartitions() {
        int i = 0;
        for (Partition partition : mapPartitions) {
            partition.setId(i);
            i++;
        }
    }

    private void assigneObjectsOfPrunedPartitionsToOthers(List<Partition> shouldRemove) {
        int minDistance = Integer.MAX_VALUE;
        int dist;
        Partition bestPartition = mapPartitions.get(0);
        for (Partition p : shouldRemove) {
            for (MrlBuilding building : p.getBuildings()) {
                for (Partition partition : mapPartitions) {
                    dist = Util.distance(building.getSelfBuilding().getLocation(world), partition.getCenterPosition());
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestPartition = partition;
                    }
                }
                bestPartition.getBuildings().add(building);
            }

        }
    }

    private void assigneObjectsOfPrunedPartitionsToOthers(Partition shouldRemove, List<Partition> shouldRemovePartitions) {
        int minDistance = Integer.MAX_VALUE;
        int dist;
        Partition bestPartition = mapPartitions.get(0);

        for (MrlZone zone : world.getZones()) {
            if (!shouldRemove.getBuildings().contains(zone.get(0))) {
                continue;
            }

//            if(zone.contains(world.getMrlBuilding(new EntityID(46294))))  {
//                System.out.println("");
//            }

            for (Partition partition : mapPartitions) {
                if (shouldRemovePartitions.contains(partition)) {
                    continue;
                }
                dist = Util.distance(zone.getCenter(), partition.getCenterPosition());
                if (dist < minDistance) {
                    minDistance = dist;
                    bestPartition = partition;
                }
            }
            bestPartition.getBuildings().addAll(zone);
            minDistance = Integer.MAX_VALUE;
            dist = 0;

        }

//        for (MrlBuilding building : shouldRemove.getBuildings()) {
//            if (building.getSelfBuilding().getID().getValue() == 29667) {
//                System.out.print("");
//            }
//            for (Partition partition : mapPartitions) {
//                if (shouldRemovePartitions.contains(partition)) {
//                    continue;
//                }
//                dist = Util.distance(building.getSelfBuilding().getLocation(world), partition.getCenterPosition());
//                if (dist < minDistance) {
//                    minDistance = dist;
//                    bestPartition = partition;
//                }
//            }
//            bestPartition.getBuildings().add(building);
//            minDistance = Integer.MAX_VALUE;
//            dist = 0;
//
//        }

    }

    /**
     * assign Proper number and type of Agents To PartitionNew basded on number of buildings in PartitionNew and PartitionNew type
     */
    private void assignProperAgentsToEachPartition() {
        int sumOfBuildings = world.getBuildings().size();
//        int i=0;
        int numberOfRescueAgents;
        double fltPercentOfNeededAgentType;
        numberOfRescueAgents = world.getPoliceForces().size() + world.getAmbulanceTeams().size() + world.getFireBrigades().size();


        for (int i = 0; i < size(); i++) {

            Partition pd = get(i);
            pd.numberOfNeededAgents = (int) Math.round(((double) pd.getBuildings().size() / (double) sumOfBuildings) * (double) numberOfRescueAgents);

            fltPercentOfNeededAgentType = ((double) pd.numberOfNeededAgents / (double) numberOfRescueAgents);
            pd.numberOfNeededPoliceForces = (int) Math.ceil(fltPercentOfNeededAgentType * (double) world.getPoliceForces().size());
            pd.numberOfNeededFireBrigades = (int) Math.ceil(fltPercentOfNeededAgentType * (double) world.getFireBrigades().size());
            pd.numberOfNeededAmbulanceTeams = (int) Math.ceil(fltPercentOfNeededAgentType * (double) world.getAmbulanceTeams().size());

            set(i, pd);
        }

        List<Partition> lstPd = new ArrayList<Partition>();
        lstPd.addAll(this);
        Collections.sort(lstPd, Partition.Partition_NumberOfAgentsComparator);

        // filling a hashSet by world Agents to assign them to proper partitions
        ArrayList<AgentDef> lstAgents = new ArrayList<AgentDef>();


        for (StandardEntity policeForce : world.getPoliceForces())
            lstAgents.add(new AgentDef(policeForce, 0));
        for (StandardEntity fireBrigade : world.getFireBrigades())
            lstAgents.add(new AgentDef(fireBrigade, 1));
        for (StandardEntity ambulanceTeam : world.getAmbulanceTeams())
            lstAgents.add(new AgentDef(ambulanceTeam, 2));

        if (self != null)
            assignAgentsToPartitions(lstAgents, lstPd);

    }

    /**
     * Assign Agents To Partitions Based on the number of agents of each type and number of needed agents in each
     * Partition
     *
     * @param lstAgents  Agent List
     * @param partitions p
     */
    private void assignAgentsToPartitions(ArrayList<AgentDef> lstAgents, List<Partition> partitions) {
        Collections.sort(lstAgents, AgentDef.AgentsIDComparator);

        int[] agentIndexes = new int[4];

        agentIndexes = findIndexesOfAgentTypes(lstAgents, agentIndexes);
        int fPF = agentIndexes[0];   //first Police Force Index
        int fFB = agentIndexes[1];     //first fire Brigade Index
        int fAT = agentIndexes[2];     //first Ambulance Team Index
//        int lPF = agentIndexes[3];   //last Police Force Index

        AgentDef objAgentDef;
        int numberOfRemainedPFA = world.getPoliceForces().size();
        int numberOfRemainedFBA = world.getFireBrigades().size();
        int numberOfRemainedATA = world.getAmbulanceTeams().size();
        ArrayList<AgentDef> agentsToRemove = new ArrayList<AgentDef>();

        int agentIndex = 0;

        if (numberOfRemainedPFA > 0) {
            StandardEntity human;
            for (int i = 0; i < numberOfPartitions; i++) {
                human = lstAgents.get(fPF).getStandardEntity();
                humanPartitionMap.put(human, this.get(i));
                if (world.getSelf().getID().equals(human.getID())) {
                    myPartitionID = i;
                }
                world.getHelper(HumanHelper.class).setLeader(human.getID(), true);
                agentsToRemove.add(lstAgents.get(fPF));
                fPF++;
                numberOfRemainedPFA--;

            }

            lstAgents.removeAll(agentsToRemove);
        }

        assignPartitionForUnAssignedOnes(lstAgents, partitions, numberOfRemainedPFA, numberOfRemainedFBA, numberOfRemainedATA);

    }

    private void assignPartitionForUnAssignedOnes(ArrayList<AgentDef> lstAgents, List<Partition> partitions, int numberOfRemainedPFA, int numberOfRemainedFBA, int numberOfRemainedATA) {
        int index = 0;
        boolean isTypeChanged = false;
        int i = 0;
        while (index < lstAgents.size()) {
            //for (int i = 0; i < numberOfPartitions; i++) {
            Partition p = partitions.get(i);
            if (index == lstAgents.size())
                break;
            StandardEntity human = lstAgents.get(index).getStandardEntity();
            if (human instanceof PoliceForce) {
//                for (int j = 0; j < p.numberOfNeededPoliceForces; j++) {

                if (numberOfRemainedPFA > 0) {
                    humanPartitionMap.put(human, p);
                    myPartitionID = p.getId();
                    numberOfRemainedPFA--;
                    index++;
                    if (index == lstAgents.size())
                        break;
                    human = lstAgents.get(index).getStandardEntity();
                    if (!(human instanceof PoliceForce)) {
                        i = -1;
                    }

                }
//                else {
//                    break;
//                }
            }
//                }
            if (human instanceof FireBrigade)
//                for (int j = 0; j < p.numberOfNeededFireBrigades; j++) {

                if (numberOfRemainedFBA > 0) {
                    humanPartitionMap.put(human, p);
                    myPartitionID = p.getId();
                    numberOfRemainedFBA--;
                    index++;
                    if (index == lstAgents.size())
                        break;
                    human = lstAgents.get(index).getStandardEntity();
                    if (!(human instanceof FireBrigade)) {
                        isTypeChanged = true;
                        i = -1;
                    }

//                    } else {
//                        break;
//                    }

                }

            if (human instanceof AmbulanceTeam)
                for (int j = 0; j < p.numberOfNeededAmbulanceTeams; j++) {

                    if (numberOfRemainedATA > 0) {
                        humanPartitionMap.put(human, p);
                        myPartitionID = p.getId();
                        numberOfRemainedATA--;
                        index++;
                        if (index == lstAgents.size())
                            break;
                        human = lstAgents.get(index).getStandardEntity();
                        if (!(human instanceof AmbulanceTeam)) {
                            i = -1;
                        }

                    } else {
                        break;
                    }

                }
            i++;
            if (i == partitions.size()) {
                i = 0;
            }

        }


        while (index < lstAgents.size()) {
            humanPartitionMap.put(lstAgents.get(index).getStandardEntity(), partitions.get(0));
            myPartitionID = 0;
            index++;
        }

    }

    // this method is used for finding the start and end index of each agent type in the sorted lstAgents list

    private int[] findIndexesOfAgentTypes(ArrayList<AgentDef> lstAgents, int[] indxOfAgents) {
        int fPF = -1;
        int lPF = -1;
        int fFB = -1;
        int lFB = -1;
        int fAT = -1;
        int lAT = -1;
        AgentDef objAgentDef;


        for (int i = 0; i < lstAgents.size(); i++) {

            objAgentDef = lstAgents.get(i);
            if (objAgentDef.getType() == 0) {
                if (lPF == -1) {
                    fPF = i;
                    lPF = i;
                }
                lPF++;
            } else if (objAgentDef.getType() == 1) {
                if (lFB == -1) {
                    fFB = i;
                    lFB = i;
                }
                lFB++;
            } else   // if (lstAgents.get(i) instanceof AmbulanceTeam)
            {
                if (lAT == -1) {
                    fAT = i;
                    lAT = i;
                }
                lAT++;
            }

        }
        indxOfAgents[0] = fPF;
        indxOfAgents[1] = fFB;
        indxOfAgents[2] = fAT;
        indxOfAgents[3] = lPF - 1;

        return indxOfAgents;

    }


    private void assignUnAssigedPaths() {
        List<Path> unAssignedPaths = new ArrayList<Path>();
        unAssignedPaths.addAll(world.getPaths());
//        for (Partition p : mapPartitions) {
//            unAssignedPaths.removeAll(p.getPaths());
//        }

        int minDistanceToPartitionCenter = Integer.MAX_VALUE;
        int tempDistance;
        Partition tempPartition = null;

        for (Path path : unAssignedPaths) {
//                System.out.println("nooooooooooooooooo     path " + path.getID() + " is not assigned.");
            for (Partition partition : mapPartitions) {
//                EntityID partitionCenterArea= path.getMiddleArea();
                tempDistance = partition.getDistanceToCenter(path.getMiddleAreaLocation().first(), path.getMiddleAreaLocation().second());
                if (tempDistance <= minDistanceToPartitionCenter) {
                    minDistanceToPartitionCenter = tempDistance;
                    tempPartition = partition;
                }
            }
            minDistanceToPartitionCenter = Integer.MAX_VALUE;
            if (tempPartition != null) {
                tempPartition.getPaths().add(path);
            }

        }
    }

    private void assignUnAssigedBuildings() {
        List<MrlBuilding> unAssignedBuildings = new ArrayList<MrlBuilding>();

        unAssignedBuildings.addAll(world.getMrlBuildings());
        for (Partition p : mapPartitions) {
            unAssignedBuildings.removeAll(p.getBuildings());
        }

        int minDistanceToPartitionCenter = Integer.MAX_VALUE;
        int tempDistance;
        Partition tempPartition = null;

        for (MrlBuilding building : unAssignedBuildings) {
            for (Partition partition : mapPartitions) {
                tempDistance = partition.getDistanceToCenter(building.getSelfBuilding());
                if (tempDistance <= minDistanceToPartitionCenter) {
                    minDistanceToPartitionCenter = tempDistance;
                    tempPartition = partition;
                }
            }
            if (tempPartition != null) {
                tempPartition.getBuildings().add(building);
            }

        }

    }


    private String getHowToPartitionTheMap() {
        int properNumberOfPartitions = findProperNumberOfPartitions();

        if (properNumberOfPartitions == -1 || world.getMapName() == null)
            return "11Map";

/*
        if (world.getMapName().equals("Kobe") || world.getMapName().equals("Berlin1") || world.getMapName().equals("Berlin") || world.getMapName().equals("Paris")) {
            if (properNumberOfPartitions == 6)
                return "23Map";// means a map with 6 partitons such that 2rows and 3Columns
            else if (properNumberOfPartitions == 1 || properNumberOfPartitions == 2)
                return "1" + properNumberOfPartitions + "Map";
            else return "22Map";
        } else if (world.getMapName().equals("VC")) {
            if (properNumberOfPartitions == 6 || properNumberOfPartitions == 4)
                return "22Map";
            else return "1" + properNumberOfPartitions + "Map";
        } else
*/
        return designProperFrame(properNumberOfPartitions);
    }


    private int findProperNumberOfPartitions() {
        // we make frames based on number of rescue agents, specialy policeForces
        // so that there remain one partition with more than one rescue agent
        int aSize;
        if (world.getPoliceForces().size() != 0) {
            aSize = world.getPoliceForces().size();
//            for(PoliceForce policeForce:world.policeForces)


        } else if (world.getFireBrigades().size() != 0) {
            aSize = world.getFireBrigades().size();
        } else {
            aSize = world.getAmbulanceTeams().size();
        }


        switch (aSize) {
            case 0:
                return -1;
            case 1:
            case 2:
                return 1;
            case 3:
            case 4:
                return 2;
            case 5:
            case 6:
                return 4;
            case 7:
            case 8:
                return 6;
            case 9:
            case 10:
                return 8;
            case 11:
            case 12:
                return 9;
            default:
                return 12;
        }

    }

    private String designProperFrame(int properNumberOfPartitions) {

        /*  int relation = 1000;


//        relation = Math.round(world.getConnectedBuildings().size() / 300);
        if (world.getMapHeight() > world.getMapWidth()) {
            return Math.min(relation, properNumberOfPartitions) + "1Map";
        } else if (world.getMapHeight() < world.getMapWidth()) {
            return "1" + Math.min(relation, properNumberOfPartitions) + "Map";
        } else {


            return "1" + Math.min(relation, properNumberOfPartitions) + "Map";

        }*/

        switch (properNumberOfPartitions) {
            case 1:
                return "11Map";
            case 2:
                return "21Map";
            case 4:
                return "22Map";
            case 6:
                return "23Map";
            case 8:
                return "24Map";
            case 9:
                return "33Map";
            case 12:
                return "43Map";
            default:
                return "11Map";
        }


    }

    public Partition getPartition(Pair<Integer, Integer> pairPosition) {
        for (Partition partition : this) {
            if (partition.contains(pairPosition))
                return partition;
        }
        return null;
    }

    public Partition getPartition(Partitionable object) {
        for (Partition partition : this) {
            if (partition.contains(object))
                return partition;
        }
        return null;
    }

    public Partition getPartition(Path path) {
        for (Partition partition : this) {
            if (partition.contains(path))
                return partition;
        }
        return null;
    }

    public Partition findPartitionID(int x, int y) {
        for (Partition pd : this) {
            if (pd.isIn(x, y))
                return pd;
        }
        return null;
    }


    private void createRendezvous() {
        //todo we should design a more dynamic code for this function//

        if (numberOfPartitions == 1)
            return;

        RendezvousConstants constantsRendezvous = new RendezvousConstants(world);
//
        if (world.getMapName().equals("Kobe")) {
            if (numberOfPartitions == 6)
                setPartitionsRendezvous(constantsRendezvous.KOBE_REND_6);
            else if (numberOfPartitions == 4)
                setPartitionsRendezvous(constantsRendezvous.KOBE_REND_4);
            else
                setPartitionsRendezvous(constantsRendezvous.KOBE_REND_2);

        } else if (world.getMapName().equals("VC")) {
            if (numberOfPartitions == 4)
                setPartitionsRendezvous(constantsRendezvous.VC_REND_4);
            else
                setPartitionsRendezvous(constantsRendezvous.VC_REND_2);
        } else // if map is unknown
        {
//        setPartitionsRendezvous(getPartitionRendezvousForUnknownMap());
        }
/*

        for (Partition partition : this)
            Collections.sort(partition.getRendezvous(), DefaultRendezvous.RendezvousPriorityComparator);
*/

    }

    private ArrayList<DefaultRendezvous> getPartitionRendezvousForUnknownMap() {

        ArrayList<DefaultRendezvous> UnknownMapRendezvouses = new ArrayList<DefaultRendezvous>();
        Point point;

        int width;
        int height;

        width = eachPartitionWidth;
        height = eachPartitionHeight;
        int sign = -1;
        int priority = 1;

        for (int i = 0; i < numberOfPartitions - 1; i++) {
            if (world.getMapHeight() > world.getMapWidth()) {

                priority = priority + sign;
                point = new Point(width / 2, height);
                UnknownMapRendezvouses.add(new DefaultRendezvous(world, point, priority, false, i, i + 1));
                sign *= -1;
                height += eachPartitionHeight;

            } else if (world.getMapHeight() <= world.getMapWidth()) {

                priority = priority + sign;
                point = new Point(width, height / 2);
                UnknownMapRendezvouses.add(new DefaultRendezvous(world, point, priority, false, i, i + 1));
                sign *= -1;
                width += eachPartitionWidth;
            }
        }


        return UnknownMapRendezvouses;
    }

    private void createRendezvousOfUnknownMap(ArrayList<DefaultRendezvous> unKnownMapRendezvous, int width, int height) {
        if (numberOfPartitions == 6) {
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(width, height / 2), 0, false, 0, 1));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(2 * width, height / 2), 1, false, 1, 2));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(2 * width + width / 2, height), 0, false, 2, 3));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(2 * width, height + height / 2), 1, false, 3, 4));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(width, height + height / 2), 0, false, 4, 5));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(width / 2, height), 1, false, 5, 0));
        } else if (numberOfPartitions == 4) {
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(width, height / 2), 0, false, 0, 1));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(2 * width, height), 1, false, 1, 2));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(width, 2 * height), 0, false, 2, 3));
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(width / 2, height), 1, false, 3, 0));
        } else if (numberOfPartitions == 2) {
            unKnownMapRendezvous.add(new DefaultRendezvous(world, new Point(width / 2, height / 2), 0, false, 0, 1));
        }
    }

    private void setPartitionsRendezvous(ArrayList<DefaultRendezvous> defaultRendezvousList) {
/*
        for (DefaultRendezvous defaultRendezvous : defaultRendezvousList)
            for (Integer partitionID : defaultRendezvous.getPartitionsId())
                this.get(partitionID).getRendezvous().add(defaultRendezvous);
*/
    }

    public Map<StandardEntity, Partition> getHumanPartitionMap() {
        return humanPartitionMap;
    }

    public String getMapName() {

//        Long hashMapValue = world.hash();

//        if (hashMapValue == 24940533)
//            return "foligno";
//        else if (hashMapValue == 14895169)
//            return "small-kobe";
//        else if (hashMapValue == 51690288)
//            return "large-kobe";
//        else if (hkashMapValue == 37880333)
//            return "vc";
//        else
        return "unknown";

    }


}