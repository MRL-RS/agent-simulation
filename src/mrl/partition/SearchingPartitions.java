package mrl.partition;


import javolution.util.FastMap;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
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

// this class is the same to Partitons  but it partitions map with considering all agent sizes instead of just
// considering policeForce Sizes
public class SearchingPartitions extends ArrayList<Partition> implements PartitionsI {

    private MrlWorld world;
    private Human self;
    private int numberOfPartitions;
    private Map<StandardEntity, Partition> humanPartitionMap;
    private ArrayList<Partition> mapPartitions;
    private Partition WORLD_PARTITION;
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

    public SearchingPartitions(MrlWorld world, Human self) {
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

    }


    private void makePartitions() {
        makeFrame();// todo consider map rotation!!!!
        assignProperAgentsToEachPartition();
        createRendezvous();
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


        for (int i = 0; i < numberOfPartitions; i++) {
            if (columnNums != 1) {
                if (i == columnNums) {
                    tempX -= eachPartitionWidth;
                    tempY += eachPartitionHeight;
                }

                Polygon polygon = new Polygon();
                polygon.addPoint(tempX, tempY);
                polygon.addPoint(tempX + eachPartitionWidth, tempY);
                polygon.addPoint(tempX + eachPartitionWidth, tempY + eachPartitionHeight);
                polygon.addPoint(tempX, tempY + eachPartitionHeight);

                Partition pD = new Partition(i, polygon, world);

                // for clock rounding
                if (i > columnNums - 1)
                    tempX -= eachPartitionWidth;
                else
                    tempX += eachPartitionWidth;
                mapPartitions.add(pD);
            } else {
                if (i == columnNums) {
                    tempY = eachPartitionHeight;
                }

                Polygon polygon = new Polygon();
                polygon.addPoint(tempX, tempY);
                polygon.addPoint(tempX + eachPartitionWidth, tempY);
                polygon.addPoint(tempX + eachPartitionWidth, tempY + eachPartitionHeight);
                polygon.addPoint(tempX, tempY + eachPartitionHeight);

                Partition pD = new Partition(i, polygon, world);


                tempY += eachPartitionHeight;
                mapPartitions.add(pD);
            }
        }

//       check unassigned path in partitions and assigned them to nearest partition
        assignUnAssigedPaths();
        assignUnAssigedBuildings();


        this.addAll(mapPartitions);

        Collections.sort(this, Partition.Partition_IDComparator);
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
     * @param partitions
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


        assignPartitionForUnAssignedOnes(lstAgents, partitions, numberOfRemainedPFA, numberOfRemainedFBA, numberOfRemainedATA);

    }

    private void assignPartitionForUnAssignedOnes(ArrayList<AgentDef> lstAgents, List<Partition> partitions, int numberOfRemainedPFA, int numberOfRemainedFBA, int numberOfRemainedATA) {
        int index = 0;
        int i = 0;
        while (index < lstAgents.size()) {
            //for (int i = 0; i < numberOfPartitions; i++) {
            Partition p = partitions.get(i);
            if (index == lstAgents.size())
                break;
            StandardEntity human = lstAgents.get(index).getStandardEntity();
            if (human instanceof PoliceForce)
                for (int j = 0; j < p.numberOfNeededPoliceForces; j++) {
                    if (numberOfRemainedPFA > 0) {
                        humanPartitionMap.put(human, p);
                        numberOfRemainedPFA--;
                        index++;
                        if (index == lstAgents.size())
                            break;
                        human = lstAgents.get(index).getStandardEntity();
                        if (!(human instanceof PoliceForce)) {
                            i = -1;
                        }
                    } else {
                        break;
                    }

                }
            if (human instanceof FireBrigade)
                for (int j = 0; j < p.numberOfNeededFireBrigades; j++) {

                    if (numberOfRemainedFBA > 0) {
                        humanPartitionMap.put(human, p);
                        numberOfRemainedFBA--;
                        index++;
                        if (index == lstAgents.size())
                            break;
                        human = lstAgents.get(index).getStandardEntity();
                        if (!(human instanceof FireBrigade)) {
                            i = -1;
                        }
                    } else {
                        break;
                    }
                }
            if (human instanceof AmbulanceTeam)
                for (int j = 0; j < p.numberOfNeededAmbulanceTeams; j++) {

                    if (numberOfRemainedATA > 0) {
                        humanPartitionMap.put(human, p);
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
        }

        while (index < lstAgents.size()) {
            humanPartitionMap.put(lstAgents.get(index).getStandardEntity(), partitions.get(0));
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
        for (Partition p : mapPartitions) {
            unAssignedPaths.removeAll(p.getPaths());
        }

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

        if (properNumberOfPartitions == -1)
            return "11Map";

        if (getMapName().equals("foligno") || getMapName().equals("small-kobe") || getMapName().equals("large-kobe")) {
            if (properNumberOfPartitions == 6)
                return "23Map";// means a map with 6 partitons such that 2rows and 3Columns
            else if (properNumberOfPartitions == 1 || properNumberOfPartitions == 2)
                return "1" + properNumberOfPartitions + "Map";
            else return "22Map";
        } else if (getMapName().equals("vc")) {
            if (properNumberOfPartitions == 6 || properNumberOfPartitions == 4)
                return "22Map";
            else return "1" + properNumberOfPartitions + "Map";
        } else
            return designProperFrame(properNumberOfPartitions);
    }


    private int findProperNumberOfPartitions() {
        // we make frames based on number of rescue agents, specialy policeForces
        // so that there remain one partition with more than one rescue agent
        int aSize = 0;
        if (world.getAgents().size() != 0) {
            aSize = world.getAgents().size() - world.getPoliceForces().size();
//            for(PoliceForce policeForce:world.policeForces)
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
            default:
                return 6;
        }

    }

    private String designProperFrame(int properNumberOfPartitions) {

        int relation = 1000;


//        relation = Math.round(world.getConnectedBuildings().size() / 300);
        if (world.getMapHeight() > world.getMapWidth()) {
            return Math.min(relation, properNumberOfPartitions) + "1Map";
        } else if (world.getMapHeight() < world.getMapWidth()) {
            return "1" + Math.min(relation, properNumberOfPartitions) + "Map";
        } else {


            return "1" + Math.min(relation, properNumberOfPartitions) + "Map";

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

//        RendezvousConstants constantsRendezvous = new RendezvousConstants(world);
//
//        if (getMapName().equals("foligno")) {
//            if (numberOfPartitions == 6)
//                setPartitionsRendezvous(constantsRendezvous.FOLIGNO_REND_6);
//            else if (numberOfPartitions == 4)
//                setPartitionsRendezvous(constantsRendezvous.FOLIGNO_REND_4);
//            else
//                setPartitionsRendezvous(constantsRendezvous.FOLIGNO_REND_2);
//
//        } else if (getMapName().equals("small-kobe")) {
//            if (numberOfPartitions == 6)
//                setPartitionsRendezvous(constantsRendezvous.KOBE_SMALL_REND_6);
//            else if (numberOfPartitions == 4)
//                setPartitionsRendezvous(constantsRendezvous.KOBE_SMALL_REND_4);
//            else
//                setPartitionsRendezvous(constantsRendezvous.KOBE_SMALL_REND_2);
//
//        } else if (getMapName().equals("large-kobe")) {
//            if (numberOfPartitions == 6)
//                setPartitionsRendezvous(constantsRendezvous.KOBE_LARGE_REND_6);
//            else if (numberOfPartitions == 4)
//                setPartitionsRendezvous(constantsRendezvous.KOBE_LARGE_REND_4);
//            else
//                setPartitionsRendezvous(constantsRendezvous.KOBE_LARGE_REND_2);
//
//        } else if (getMapName().equals("vc")) {
//            if (numberOfPartitions == 4)
//                setPartitionsRendezvous(constantsRendezvous.VC_REND_4);
//            else
//                setPartitionsRendezvous(constantsRendezvous.VC_REND_2);
//        } else // if map is unknown
//        {
        setPartitionsRendezvous(getPartitionRendezvousForUnknownMap());
//        }

      /*  for (Partition partition : this)
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