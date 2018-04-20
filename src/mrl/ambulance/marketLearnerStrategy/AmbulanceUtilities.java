package mrl.ambulance.marketLearnerStrategy;

import javolution.util.FastSet;
import mrl.ambulance.structures.Bid;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.HumanHelper;
import mrl.helper.PropertyHelper;
import mrl.partition.PairSerialized;
import mrl.partition.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.IPathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 28, 2010
 * Time: 7:03:52 PM
 */
public class AmbulanceUtilities {

    private MrlWorld world;
    private int lastTraveledDistance = 0; // it will set to zero when it is rescuing a civilian
    private int moveTimeInPartition = 2;
//    private HashMap<Pair<EntityID, EntityID>, Integer> partitionRefugeTimeMap = new HashMap<Pair<EntityID, EntityID>, Integer>();
//    private FastMap<EntityID, ArrayList<Civilian>> similarCivilians = new FastMap<EntityID, ArrayList<Civilian>>();

    private Set<StandardEntity> readyAmbulances;
    private HumanHelper humanHelper;


    //// getter && setter

    public int getLastTraveledDistance() {
        return lastTraveledDistance;
    }

    public void setLastTraveledDistance(int lastTraveledDistance) {
        this.lastTraveledDistance = lastTraveledDistance;
    }


    //// Public Methods

    public AmbulanceUtilities(MrlWorld world) {
        this.world = world;
        this.readyAmbulances = new FastSet<StandardEntity>();
        this.humanHelper = world.getHelper(HumanHelper.class);
    }

    public void computeLastTraveledDistance() {


        int[] history = world.getSelfHuman().getPositionHistory();
        int tempDistance = 0;

        if (history == null)
            return;
        if (history.length < 4) {
            return;
        }


        int x = history[0];
        int y = history[1];
        for (int i = 2; i < history.length; i += 2) {
            int x2 = history[i];
            int y2 = history[i + 1];
            tempDistance = Util.distance(x, y, x2, y2);

            lastTraveledDistance += tempDistance;

            x = x2;
            y = y2;
        }


    }

    public int approximatingTTA(Human human) {

        //todo compute it   ==> TEST IS OK

        Partition targetPartition = null;
        Partition myPartition = null;
        PairSerialized<List<Integer>, Integer> pathAndMoveTime = new PairSerialized<List<Integer>, Integer>(null, null);


        //if we are in the same building
        if (human.getPosition().equals(world.getSelfPosition().getID())) {
            return 1;
        }


        targetPartition = world.getPreRoutingPartitions().getPartition(human.getPosition(world).getLocation(world));
        if (targetPartition == null) {
            System.out.println("PPPPPPPPPPPcivilianPartitioncivilianPartition  human.getLocation(world)" + human.getLocation(world));
            return 1000;
        }

        myPartition = world.getPreRoutingPartitions().getPartition(world.getSelfHuman().getLocation(world));
        //it is like <path from A to B,move Time on this path>
        if (myPartition != null) {
            // if there is a refuge in my partitions
            if (myPartition.getId().equals(targetPartition.getId()))
                return this.moveTimeInPartition;

            pathAndMoveTime = myPartition.getPathsToOthers().get(new PairSerialized<Integer, Integer>(myPartition.getId(), targetPartition.getId()));
        }

        if (pathAndMoveTime == null) {
            System.out.println("pathAndMoveTimepathAndMoveTimepathAndMoveTimepathAndMoveTime");
            return computeSimpleMoveTime(world.getSelfPosition().getID(), human.getPosition());

        }
        if (pathAndMoveTime.second() == 0) {
            return 2;
        }

        return pathAndMoveTime.second() + 1;
    }

    public int approximatingTTM(StandardEntity firstPosition, StandardEntity secondPosition) {

        //todo compute it   ==> TEST IS OK

        Partition firstPartition = null;
        Partition secondPartition = null;
        PairSerialized<List<Integer>, Integer> pathAndMoveTime = new PairSerialized<List<Integer>, Integer>(null, null);

        if (firstPosition instanceof AmbulanceTeam) {
            firstPosition = ((AmbulanceTeam) firstPosition).getPosition(world);
        }
        if (secondPosition instanceof AmbulanceTeam) {
            secondPosition = ((AmbulanceTeam) secondPosition).getPosition(world);
        }

        //if we are in the same building
        if (firstPosition.equals(secondPosition)) {
            return 1;
        }

        if (firstPosition.getLocation(world) == null) {
            System.out.println("OHOHOH be man begid hatman");
        }


        firstPartition = world.getPreRoutingPartitions().getPartition(firstPosition.getLocation(world));
        if (firstPartition == null) {
            System.out.println("PPPPPPPPPPPcivilianPartitioncivilianPartition  civilian.getLocation(world)" + firstPosition.getLocation(world));
            return 1000;
        }

        secondPartition = world.getPreRoutingPartitions().getPartition(secondPosition.getLocation(world));
        //it is like <path from A to B,move Time on this path>
        if (secondPartition != null) {
            // if there is a refuge in my partitions
            if (secondPartition.getId().equals(firstPartition.getId()))
                return this.moveTimeInPartition;

            pathAndMoveTime = secondPartition.getPathsToOthers().get(new PairSerialized<Integer, Integer>(secondPartition.getId(), firstPartition.getId()));
        }

        if (pathAndMoveTime == null) {
            System.out.println("pathAndMoveTimepathAndMoveTimepathAndMoveTimepathAndMoveTime");
            return computeSimpleMoveTime(firstPosition.getID(), secondPosition.getID());

        }
        if (pathAndMoveTime.second() == 0) {
            return 2;
        }

        return pathAndMoveTime.second() + 1;
    }

    private int computeSimpleMoveTime(EntityID position1, EntityID position2) {
        return (int) (world.getDistance(position1, position2) / MRLConstants.MEAN_VELOCITY_OF_MOVING) + 1;
    }

    /**
     * approximating Time To Refuge for this civilan
     *
     * @param civilian civilian to compute TTR for
     * @return returns  a Pair<Integer,Refuge> that first is TTA to second, nearest refuge
     */
    public Pair<Integer, EntityID> approximatingTTR(Civilian civilian) {

        //todo compute it  ==> TEST IS OK

        int minTime = 255;
        EntityID nearestRefuge = null;


        if (!civilian.isDamageDefined())
            return null;

        int minDistance;
        List<EntityID> refugePath;

        if (world.getSelf() instanceof MrlPlatoonAgent) {
            refugePath = world.getPlatoonAgent().getPathPlanner().getRefugePath((Area) world.getEntity(civilian.getPosition()), true);
            minDistance = world.getPlatoonAgent().getPathPlanner().getNearestAreaPathCost();
            if (MRLConstants.DEBUG_AMBULANCE_TEAM && refugePath.size() == 0) {
//                refugePath = world.getPlatoonAgent().getPathPlanner().getRefugePath((Area) world.getEntity(civilian.getPosition()), true);
//                System.err.println(civilian.getPosition() + " refugePath.size()==0 ");
            }


        } else {
            refugePath = world.getCenterAgent().getPathPlanner().getRefugePath((Area) world.getEntity(civilian.getPosition()), true);
            minDistance = world.getCenterAgent().getPathPlanner().getNearestAreaPathCost();

        }
        if (refugePath.size() > 0) {
            nearestRefuge = refugePath.get(refugePath.size() - 1);
            minTime = (int) (minDistance / MRLConstants.MEAN_VELOCITY_OF_MOVING);
            if (minTime == 0)
                minTime = 1;
            if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
                System.err.println("OOOO TTR=0");
            }
        }

        minTime++;
        return new Pair<Integer, EntityID>(minTime, nearestRefuge);


//        ArrayList <Pair<Integer,EntityID>> refugePairs=new ArrayList <Pair<Integer,EntityID>>();
//        for(Refuge refuge:world.getRefuges())
//        {
//            refugePairs.add(new Pair<Integer,EntityID> (world.getDistance(civilian.getPosition(),refuge.getID()),refuge.getID()));
//        }
//
//        Collections.sort(refugePairs,RefugeDistanceComparator);
//
//        for (int i=0;i<refugePairs.size()/2;i++) {
////            civilianPartition = world.getPreRoutingPartitions().getPartition(civilian.getLocation(world));
//            tempTime = partitionRefugeTimeMap.get(new Pair<EntityID, EntityID>(civilian.getPosition(), refugePairs.get(i).second()));
//            if (tempTime == null) {
//                tempTime = computeMoveTimeBetweenTwoPoints(civilian.getPosition(), refugePairs.get(i).second());
//                partitionRefugeTimeMap.put(new Pair<EntityID, EntityID>(civilian.getPosition(), refugePairs.get(i).second()), tempTime);
//            }
//            if (tempTime < minTime) {
//                minTime = tempTime;
//                nearestRefuge = refugePairs.get(i).second();
//            }
//
//        }

//        System.out.println(" <<<<<< CivilID "+civilian.getID()+" civPart: "+civilianPartition.getId() +" RRRRRRRR movReeeffff "+ moveTimeAndReguges);

    }

    public int computeTimeToNearestAvailableRefuge(StandardEntity victimPosition) {

        //Time To Refuge
        int ttr = 1000;
        int temp = 0;
        boolean isOpen = false;
        Building refuge;
        for (StandardEntity refugeStandardEntity : world.getRefuges()) {

            refuge = (Building) refugeStandardEntity;
            isOpen = false;

            if (world.getMrlBuilding(refuge.getID()).isOneEntranceOpen(world)) {
                isOpen = true;
            }

            if (isOpen) {

                temp = this.approximatingTTM(victimPosition, refugeStandardEntity);
                if (temp < ttr) {
                    ttr = temp;
                }
            }
        }

        return ttr;
    }


    public Comparator RefugeDistanceComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            Pair<Integer, EntityID> p1 = (Pair<Integer, EntityID>) (o1);
            Pair<Integer, EntityID> p2 = (Pair<Integer, EntityID>) (o2);

            if (p1.first() > p2.first())
                return 1;
            if (p1.first() == p2.first())
                return 0;

            return -1;
        }
    };

//    public Civilian approximateAndSetTTR(Civilian civilian) {
//        Pair<Integer, Refuge> p = approximatingTTR(civilian);
//
//
//        civilian.setTimeToRefuge(p.first());
//        civilian.setNearestRefuge(p.second());
//
//        return civilian;
//
//    }

    public int computeNumberOfATsThatSelect(ArrayList<Bid> bids, Civilian civilian) {
        int num = 0;
        for (Bid bid : bids) {
            if (bid.getCivilian(world).equals(civilian)) {
                num++;
            }
        }

        return num;
    }

    public void updateEveryCycleInfos(Set<StandardEntity> humans) {
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        for (StandardEntity standardEntity : humans) {
            Human human = (Human) standardEntity;
            buriednessOperations(human, humanHelper);
        }

    }

    public void updateCiviliansHP(Set<StandardEntity> humans, int hpPrecision, int damagePrecision) {

        int directHP = 0;
        int directDamage = 0;
        int tempHP = 0;
        int tempDMG = 0;
        int magicHP = (int) Math.ceil((float) hpPrecision / 10);
        int magicDMG = (int) Math.ceil((float) damagePrecision / 10);
        int halfOfHPprecision = hpPrecision / 2;
        int halfOfDMGprecision = damagePrecision / 2;


        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        for (StandardEntity standardEntity : humans) {
            Human human = (Human) standardEntity;

//            ///////////// predicting current HP and current Damage
//            int deltaT=world.getTime()-humanHelper.getLastTimeHPChanged(human.getID());

            // because we get right values from the platoon agent itself
            if (!(human instanceof Civilian)) {
                humanHelper.setCurrentHP(human.getID(), human.getHP());
                humanHelper.setCurrentDamage(human.getID(), human.getDamage());
                continue;
            }

            int firstDMG = humanHelper.getFirstDamage(human.getID());
            if (firstDMG == 6) {
                humanHelper.setFirstDamage(human.getID(), damagePrecision / 4);
            }

            world.getHelper(PropertyHelper.class).getEntityLastUpdateTime(human);
            // first prediction
//            if (damagePrecision == 25) {// stable condition
//                 switch (firstDMG ) {
//                    case 0:
//                        humanHelper.setCurrentDamage(human.getID(), 0);
//                        humanHelper.setCurrentHP(human.getID(), 10000);
//                        break;
//                    case 6:
//                        tempHP = computeNewHP(6, 0.2, world.getTime());
//                        tempDMG = computeNewDamage(6, 0.2, world.getTime());
//                        break;
//                    case 25:
//                        tempHP = computeNewHP(19, 0.2, world.getTime());
//                        tempDMG = computeNewDamage(19, 0.2, world.getTime());
//                        break;
//                    case 50:
//                        tempHP = computeNewHP(50, 0.3, world.getTime());
//                        tempDMG = computeNewDamage(50, 0.3, world.getTime());
//                        break;
//                    case 75:
//                        tempHP = computeNewHP(75, 0.4, world.getTime());
//                        tempDMG = computeNewDamage(75, 0.4, world.getTime());
//                        break;
//                    default:
//                        tempHP = computeNewHP(100, 0.5, world.getTime());
//                        tempDMG = computeNewDamage(100, 0.5, world.getTime());
//
//                }
//
//            } else {
            if (firstDMG == 0) {
                humanHelper.setCurrentDamage(human.getID(), 0);
                humanHelper.setCurrentHP(human.getID(), 10000);
            } else if (firstDMG == damagePrecision / 4) {
                tempHP = computeNewHP(firstDMG, 0.2, world.getTime());
                tempDMG = computeNewDamage(firstDMG, 0.2, world.getTime());
            } else if (firstDMG < 37) {
                int a = firstDMG - damagePrecision / 4;
                tempHP = computeNewHP(a, 0.2, world.getTime());
                tempDMG = computeNewDamage(a, 0.2, world.getTime());
            } else if (firstDMG <= 62) {
                int a = firstDMG - damagePrecision / 4;
                tempHP = computeNewHP(a, 0.3, world.getTime());
                tempDMG = computeNewDamage(a, 0.3, world.getTime());
            } else if (firstDMG <= 87) {
                int a = firstDMG - damagePrecision / 4;
                tempHP = computeNewHP(a, 0.4, world.getTime());
                tempDMG = computeNewDamage(a, 0.4, world.getTime());
            } else {
                int a = firstDMG - damagePrecision / 4;
                tempHP = computeNewHP(a, 0.5, world.getTime());
                tempDMG = computeNewDamage(a, 0.5, world.getTime());
            }


//            }
            tempHP = 10000 - tempHP;

            directHP = tempHP;
            directDamage = tempDMG;

            tempHP = tempHP - humanHelper.getDeltaHP(human.getID());
            tempDMG = tempDMG - humanHelper.getDeltaDamage(human.getID());

            // investigation of first prediction by sense information and repair it
            if (world.getHelper(PropertyHelper.class).getPropertyTime(human.getHPProperty()) == world.getTime()) {

//                System.out.println(world.getTime() + " " + world.getSelf().getID() + " AFTER SENCE====>>> " + human.getID());

                humanHelper.setLastTimeHPChanged(human.getID(), world.getTime());
                humanHelper.setLastTimeDamageChanged(human.getID(), world.getTime());

                int deltaHP = human.getHP() - tempHP;
                int deltaHP_ABS = Math.abs(deltaHP);
                int deltaDamage = human.getDamage() - tempDMG;
                int deltaDamage_ABS = Math.abs(deltaDamage);
                int sign = 1;
                if (deltaHP < 0)
                    sign = -1;


//                if (deltaHP > 250) {
//                    if (deltaHP <= 250 + world.getTime()) {
//                        tempDMG += 1;
//                        tempHP -= world.getTime();
//                    } else {
//                        tempHP = (((tempHP + 250) % 250) + 1) * 250 - Math.abs(deltaHP % 250);
//                        tempDMG = approximateNewDamage(humanHelper.getFirstDamage(human.getID()), human.getDamage(), tempDMG, tempHP, world.getTime());
//                    }
//                } else if (deltaHP < 0) {
//                    if (deltaHP + world.getTime() >= 0) {
//                        tempDMG -= 1;
//                        tempHP += world.getTime();
//                    } else {
//                        tempHP = (((tempHP - 250) % 250) + 1) * 250 - Math.abs(deltaHP % 250);
//                        tempDMG = approximateNewDamage(humanHelper.getFirstDamage(human.getID()), human.getDamage(), tempDMG, tempHP, world.getTime());
//                    }
//                }


                // HP reconstruction
                if (deltaHP_ABS > halfOfHPprecision) {
                    if (deltaHP_ABS - magicHP > halfOfHPprecision) {
                        tempHP = human.getHP() - (deltaHP) % halfOfHPprecision;
                    } else {
                        tempHP += magicHP * sign;
                    }
                    if (deltaDamage_ABS > halfOfDMGprecision) {
                        if (deltaDamage_ABS - magicDMG > halfOfDMGprecision) {
//                            tempDMG = approximateNewDamage(humanHelper.getFirstDamage(human.getID()), human.getDamage(), tempDMG, tempHP, world.getTime());
                            tempDMG = human.getDamage() - (deltaDamage) % halfOfDMGprecision;
                            if (MRLConstants.DEBUG_AMBULANCE_TEAM && tempDMG <= 0) {
                                System.out.println("OOO");
                            }

                        } else {
                            tempDMG -= sign * magicDMG;
                            if (MRLConstants.DEBUG_AMBULANCE_TEAM && tempDMG <= 0) {
                                System.out.println("OOO");
                            }

                        }
                    } else {

                        if (Math.abs(human.getDamage() - (tempDMG - sign)) <= halfOfDMGprecision) // if change dose not break the range
                        {
                            tempDMG -= sign;
                            if (MRLConstants.DEBUG_AMBULANCE_TEAM && tempDMG <= 0) {
                                System.out.println("OOO");
                            }

                        }
                    }

                } else {
                    if (deltaDamage_ABS > halfOfDMGprecision) {
                        if (deltaDamage_ABS - magicDMG > halfOfDMGprecision) {
                            //todo ???????
                            tempDMG = approximateNewDamage(humanHelper.getFirstDamage(human.getID()), human.getDamage(), tempDMG, tempHP, world.getTime());
                            if (MRLConstants.DEBUG_AMBULANCE_TEAM && tempDMG <= 0) {
                                System.out.println("OOO");
                            }

                        } else {
                            tempDMG -= sign * magicDMG;
                            if (MRLConstants.DEBUG_AMBULANCE_TEAM && tempDMG <= 0) {
                                System.out.println("OOO");
                            }

                        }
                    }
                }

            }
            humanHelper.setDeltaHP(human.getID(), (directHP - tempHP));
            humanHelper.setDeltaDamage(human.getID(), (directDamage - tempDMG));


            if (MRLConstants.DEBUG_AMBULANCE_TEAM && tempDMG <= 0) {
                System.out.println("OOO");
            }

            // set predicted values
            humanHelper.setCurrentHP(human.getID(), tempHP);
            if (MRLConstants.DEBUG_AMBULANCE_TEAM && tempDMG >= 100)
                System.out.println("tempDMG >>>>>>> 100");
            humanHelper.setCurrentDamage(human.getID(), tempDMG);

//                updateSimilarCiviliansConditions(humans, human.getDamage(), human.getHP());

        }
    }

    private void buriednessOperations(Human human, HumanHelper humanHelper) {

        int previousBuriedness = humanHelper.getPreviousBuriedness(human.getID());
        int buriednessDifference = previousBuriedness - human.getBuriedness();
        if (buriednessDifference != 0) {
            if (buriednessDifference > 0) {
                humanHelper.setNumberOfATsRescuing(human.getID(), buriednessDifference);
            }
        } else {
            humanHelper.setNumberOfATsRescuing(human.getID(), 0);
        }

        //update buriedness
        if (!humanHelper.isFromSense(human.getID())) {
            humanHelper.setCurrentBuriedness(human.getID(), human.getBuriedness() - humanHelper.getNumberOfATsRescuing(human.getID()));
        } else {
            humanHelper.setCurrentBuriedness(human.getID(), human.getBuriedness());
        }

        humanHelper.setPreviousBuriedness(human.getID(), human.getBuriedness());

    }

    private int approximateNewDamage(int firstDamage, int damage, int tempDMG, int tempHP, int time) {

        int newDMG = 0;

        newDMG = ((10000 - tempHP) / time - firstDamage) * 2 + firstDamage;

        int delta1 = Math.abs(damage - tempDMG);
        int delta2 = Math.abs(newDMG - damage);

        if (delta1 <= 13) {
            if (delta2 <= 13) {
                return (tempDMG + newDMG) / 2;
            } else {
                return tempDMG;
            }
        } else {
            if (delta2 <= 13) {
                return newDMG;
            } else {
                return (damage + newDMG) / 2;
            }
        }

//        if (delta1 < 13 && delta2 < 13) {
//            return (tempDMG + newDMG) / 2;
//        } else if (delta1 < 13 && delta2 > 13) {
//            return tempDMG;
//        } else if (delta1 > 13 && delta2 < 13) {
//            return newDMG;
//        } else {
//            return (damage + newDMG) / 2;
//        }


    }

//    private void updateSimilarCiviliansConditions
//            (ArrayList<Civilian> civilians, int damage,
//             int hp) {
//        for (Civilian civilian : civilians) {
//            CivilianHelper civilianHelper = world.getHelper(CivilianHelper.class);
//            // if is similar
//            if (civilian.getHP() == hp && civilian.getDamage() == damage) {
//                civilianHelper.setCurrentHP(civilian.getID(), civilian.getHP() - 125);
//                civilianHelper.setCurrentDamage(civilian.getID(), civilian.getDamage());
//
//            }
//        }
//    }

    private int computeNewDamage(int alpha, double beta, int time) {
        return (int) (alpha + time * beta);
    }

    private int computeNewHP(int alpha, double beta, int time) {

        return (int) ((alpha * time) + (beta * (time * (time - 1) / 2)));

    }

//    public void updateCiviliansHealthProperties(ArrayList<Civilian> civilians) {
//        int deltaT = 0;
//
//        for (Civilian civ : civilians) {
//            CivilianHelper civilianHelper = world.getHelper(CivilianHelper.class);
//            int currentHP = civilianHelper.getCurrentHP(civ.getID());
//            int currentDamage = civilianHelper.getCurrentDamage(civ.getID());
//            int previousHP = civilianHelper.getPreviousHP(civ.getID());
//            buriednessOperations(civ, civilianHelper);
//
//
//            if (civilianHelper.getLastTimeHPChanged(civ.getID()) == 0) {
//                //DMG
//                civilianHelper.setCurrentDamage(civ.getID(), civ.getDamage());
//                civilianHelper.setPreviousDamage(civ.getID(), civ.getDamage());
//                //HP
//                civilianHelper.setCurrentHP(civ.getID(), civ.getHP());
//                civilianHelper.setPreviousHP(civ.getID(), civ.getHP());
//                //Time
//                civilianHelper.setLastTimeHPChanged(civ.getID(), world.getTime());
//
//                continue;
//            }
//
//            deltaT = world.getTime() - civilianHelper.getLastTimeHPChanged(civ.getID());
//            if (/*civ.getHP() != civilianHelper.getPreviousHP(civ.getID()) &&*/ deltaT != 0) {
//                int tempHp = (previousHP - deltaT * civ.getDamage());
//
//                if (Math.abs(tempHp - civ.getHP()) < 25) {
//                    civilianHelper.setCurrentHP(civ.getID(), tempHp);
//                } else {
//                    civilianHelper.setCurrentHP(civ.getID(), civ.getHP());
//                }
//
//
//                civilianHelper.setCurrentDamage(civ.getID(), (civilianHelper.getPreviousHP(civ.getID()) - civ.getHP()) / deltaT);
//                civilianHelper.setPreviousHP(civ.getID(), civ.getHP());
//                civilianHelper.setLastTimeHPChanged(civ.getID(), world.getTime());
//            }
//            deltaT = world.getTime() - civilianHelper.getLastTimeDamageChanged(civ.getID());
//            if (civ.getDamage() != civilianHelper.getPreviousDamage(civ.getID()) && deltaT != 0) {
//                civilianHelper.setDeltaDamage(civ.getID(), Math.abs(civilianHelper.getPreviousDamage(civ.getID()) - civ.getDamage()) / deltaT);
//                civilianHelper.setCurrentDamage(civ.getID(), civ.getDamage());
//                civilianHelper.setPreviousDamage(civ.getID(), civ.getDamage());
//                civilianHelper.setLastTimeDamageChanged(civ.getID(), world.getTime());
//            }
//            civilianHelper.setCurrentDamage(civ.getID(), civilianHelper.getCurrentDamage(civ.getID()) + civilianHelper.getDeltaDamage(civ.getID()));
//            civilianHelper.setCurrentHP(civ.getID(), civilianHelper.getCurrentHP(civ.getID()) - (civilianHelper.getCurrentDamage(civ.getID()) + civilianHelper.getDeltaDamage(civ.getID())));
//            if (civ.getHP() <= 0) {
//                civilianHelper.setCurrentHP(civ.getID(), 0);
//                civilianHelper.setCurrentDamage(civ.getID(), 0);
//                //System.out.println("The civilian ["+civ.getID()+"] is already DEAD");
//            }
//            if (civilianHelper.getCurrentHP(civ.getID()) <= 0) {
//                if (civ.getHP() - (civ.getDamage() * (world.getTime() - civilianHelper.getLastTimeHPChanged(civ.getID()))) <= 0) {
//                    civilianHelper.setCurrentHP(civ.getID(), 0);
//                    civilianHelper.setCurrentDamage(civ.getID(), 0);
//                    civ.setHP(0);
//                } else {
//                    civilianHelper.setCurrentHP(civ.getID(), civ.getHP() - (civ.getDamage() * (world.getTime() - civilianHelper.getLastTimeHPChanged(civ.getID()))));
//                    civilianHelper.setCurrentDamage(civ.getID(), civ.getDamage() + civilianHelper.getDeltaDamage(civ.getID()));
//                }
//            }
//        }
//    }

    /**
     * to find time to free range
     * TTFRange=1 if TTF=0
     * TTFRange=2 if  ( 1<= TTF <=5 )
     * TTFRange=3 if  ( 6<= TTF <=10 )
     * TTFRange=4 if  ( 11<= TTF <=15 )
     * TTFRange=5 if  ( TTF > 15 )
     *
     * @param timeToFree timeToFreeForVictim
     * @return TTF range number
     */
    public int computeTTFRange(int timeToFree) {

        if (timeToFree == 0)
            return 1;
        if (timeToFree >= 1 && timeToFree <= 5)
            return 2;
        if (timeToFree >= 6 && timeToFree <= 10)
            return 3;
        if (timeToFree >= 11 && timeToFree <= 15)
            return 4;
        if (timeToFree > 15)
            return 5;

        return -1;


    }

    public int timeToFreeForVictim(Human human) {


        if (human != null && human.getBuriednessProperty().isDefined()) {

//            int num = computeNumberOfATsRescuingThis(civilian);

            int num = world.getHelper(HumanHelper.class).getNumberOfATsRescuing(human.getID());
            if (num == 0) {

//                System.out.println("vaaaaaaaaaaaaaaaaaaaaayyyyy");
                return 1000;
            } else
                return (int) Math.ceil((float) human.getBuriedness() / num);

        }

        // means not rescuing any civilian and is free
        return 0;
    }

    public int timeToFreeAT(Human human) {


        if (human != null && human.getBuriednessProperty().isDefined()) {

//            int num = computeNumberOfATsRescuingThis(civilian);

            int num = world.getHelper(HumanHelper.class).getNumberOfATsRescuing(human.getID());
            if (num == 0) {

//                System.out.println("vaaaaaaaaaaaaaaaaaaaaayyyyy");
                return 0;
            } else
                return (int) Math.ceil((float) human.getBuriedness() / num);

        }

        // means not rescuing any civilian and is free
        return 0;
    }

    /**
     * to find number Of Available Ambulance teams range
     * AATRange=1 if  AAT is Low or <=2
     * AATRange=2 if  AAT is Medium or >=3 And <=5
     * AATRange=3 if  AAT is high or >=6
     *
     * @param firstCycles no bids came yet
     * @return AAT range number
     */
    public int computeAvailableATsRange(boolean firstCycles) {

        int num = computeNumberOfHealthyATs();


        if (num <= 2)
            return 1;
        else if (num >= 3 && num <= 5)
            return 2;
        else
            return 3;

    }

    public int computeNumberOfHealthyATs() {
        int num = 0;
        for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
            AmbulanceTeam ambulanceTeam = (AmbulanceTeam) standardEntity;
            if (ambulanceTeam.isBuriednessDefined() && ambulanceTeam.getBuriedness() == 0)
                num++;
        }
        return num;
    }

    public int computeNumberOfATsRescuingThis(Human human) {

        //todo it is about previouse cycle, is there any better method
        Pair<Integer, Integer> startCurrentTimePair;
        int num = 0;
//        for (AmbulanceTeam at : world.getAmbulanceTeams()) {
//            startCurrentTimePair = world.getAmbulanceCivilianMap().get(new Pair<EntityID, EntityID>(at.getID(), human.getID()));
//            if (startCurrentTimePair != null && startCurrentTimePair.second() == world.getTime() - 1)
//                num++;
//        }

        return num;

    }


//    //// Private Methods
//    private int computeMoveTimeBetweenTwoPoints(EntityID first, EntityID second) {
//
//
//        List<EntityID> path = new ArrayList<EntityID>();
//        IPathPlanner pathPlanner = world.getPlatoonAgent().getPathPlanner();
//
//
//        //todo compute it  ==> TEST IS OK
//
//        Area firstArea = (Area) world.getEntity(first);
//        Area secondArea = (Area) world.getEntity(second);
//        path = pathPlanner.planMove(firstArea, secondArea, MRLConstants.IN_TARGET, false);
//        int predictedDistance = pathPlanner.getPathCost();
//
//        if (path.isEmpty())
//            return 1000; // means a long Move Time
//
////        int predictedDistance = Util.distance(world.getEntity(first).getLocation(world),world.getEntity(second).getLocation(world));
//
//        //todo ==> compute moveTime for a path   =>>> TEST IS OK    sometimes bigger than real
//
//        return (int) (predictedDistance / MRLConstants.meanVelocityOfMoving);
//    }


    public int computeNumberOfVisibleATs() {
        int num = 0;
        for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
            AmbulanceTeam at = (AmbulanceTeam) standardEntity;
            if (world.isVisible(at) && at.getPosition().equals(world.getSelfPosition().getID())) {
                num += 1;
            }
        }
        return num;

    }

    public void chooseLeader() {

        if (world.getTime() <= 6 || world.getAmbulanceLeaderID() != null) {
            return;
        }
        int leaderID = world.getSelf().getID().getValue();
        for (AmbulanceTeam at : world.getAmbulanceTeamList()) {
            if (at.isBuriednessDefined()) {
                if (at.getBuriedness() == 0 && at.getID().getValue() < leaderID) {
                    leaderID = at.getID().getValue();
                }
            }
        }

//        System.out.println("...... " + world.getTime() + " " + world.getSelf().getID() + " LEADER IS ... ............... " + leaderID);
        world.setAmbulanceLeaderID(new EntityID(leaderID));

    }

    public Set<StandardEntity> getReadyAmbulances() {
        return readyAmbulances;
    }


    public void updateReadyAmbulances() {
//        readyAmbulances.clear();
        for (AmbulanceTeam ambulanceTeam : world.getAmbulanceTeamList()) {
            if (ambulanceTeam.getID().equals(world.getSelf().getID())) {      // This "ambulanceTeam" is actually "me"
                if (world.getSelfPosition() instanceof Building) {
                    if (world.getSelfHuman().getBuriedness() == 0            // Means that I'm not buried!
                            && world.getTime() >= 2) {                          // exactly at the second cycle, ambulance Team agents might get buried.
                        readyAmbulances.add(ambulanceTeam);
                    } else {
                        // Too soon to decide if I will be buried (at time:2)
                    }
                } else { // I am on the road!
                    readyAmbulances.add(ambulanceTeam);
                }
            } else { // This "ambulanceTeam" is someone else (not me)
                if (humanHelper.getAgentState(ambulanceTeam.getID()) == null) { // I have no idea in what state this ambulanceTeam is.
                    //TODO @BrainX Is there someway better to access others' states?
                    if (ambulanceTeam.isBuriednessDefined() && ambulanceTeam.getBuriedness() == 0) { // I have information about this ambulanceTeam's buriedness and it's not buried.
                        readyAmbulances.add(ambulanceTeam);
                    } else if (ambulanceTeam.getPosition(world) instanceof Road) { // If I don't know if this ambulanceTeam is buried, I consider it healthy if it's on the road.
                        readyAmbulances.add(ambulanceTeam);
                    }
                } else { // I know this ambulanceTeam's state
                    if (humanHelper.isAgentStateHealthy(ambulanceTeam.getID())) {
                        readyAmbulances.add(ambulanceTeam);
                    }
                }
            }
        }


    }

    /**
     * Computes time to death of the selected victim, it should be considered that if the victim is not seen during
     * a long time, this estimation might not be currect
     *
     * @param victim The selected victim to compute its Time To Death
     * @return pair(ttd, lastAliveCycle) The estimaded time to death of the selected victim
     */
    public Pair<Integer, Integer> computeTTD(StandardEntity victim) {

        float currentDMG;
        int currentHP;

        if (victim instanceof Civilian) {
            HumanHelper humanHelper = world.getHelper(HumanHelper.class);
            currentDMG = humanHelper.getCurrentDamage(victim.getID());
            currentHP = humanHelper.getCurrentHP(victim.getID());
        } else {
            Human agent = (Human) victim;
            if (!agent.isHPDefined()) {
                return new Pair<Integer, Integer>(0, 0);
            }
            currentDMG = agent.getDamage();
            currentHP = agent.getHP();
        }

//        System.out.println(world.getTime() + " me:" + world.getSelf().getID() + " target:" + victim.getID() + " currentHP:" + currentHP);

        int ttd = -1;
        int lastLiveTime = -1;

        for (int i = world.getTime() + 1; i <= world.getKernel_TimeSteps(); i++) {

            if (currentDMG <= 37) {
                currentDMG += 0.2;
            } else if (currentDMG <= 63) {
                currentDMG += 0.3;
            } else if (currentDMG <= 88) {
                currentDMG += 0.4;
            } else {
                currentDMG += 0.5;
            }

            currentHP -= Math.round(currentDMG);
            if (currentHP <= 0) {
                ttd = i - world.getTime();
                lastLiveTime = i;
                break;
            }

        }

        return new Pair<Integer, Integer>(ttd, lastLiveTime);

    }


    public int computeDistanceToFire(EntityID id) {

        Building building;
        int dist, minDist = Integer.MAX_VALUE;
        Building nearestFieryBuilding;
        for (StandardEntity b : world.getBuildings()) {
            building = (Building) b;
            if (building.isFierynessDefined() && building.getFieryness() != 0 && building.getFieryness() != 4) {
                dist = world.getDistance(id, building.getID());
                if (dist < minDist) {
                    minDist = dist;
                    nearestFieryBuilding = building;
                }

            }

        }

        return minDist;
    }


    public boolean isAlivable(Human human) {

        if (world.getTime() - world.getHelper(HumanHelper.class).getLastTimeHPChanged(human.getID()) > 40) {
            return true;
        }

        int ttd = computeTTD(world.getEntity(human.getID())).first();
        int minTimeToAvailableRefuge = computeTimeToNearestAvailableRefuge(human.getPosition(world));
        int numberOfRescuing = world.getHelper(HumanHelper.class).getNumberOfATsRescuing(human.getID());
        int deltaBuriedness;

        if (!human.isBuriednessDefined()) {
            return true;
        }

        if (numberOfRescuing > 0)
            deltaBuriedness = (int) Math.ceil((float) human.getBuriedness() / numberOfRescuing);
        else {
            deltaBuriedness = human.getBuriedness();
        }
        return ttd > (minTimeToAvailableRefuge + deltaBuriedness);

    }

    public boolean isAlivable(Human human, int numOfAgent) {

        if (world.getTime() - world.getHelper(HumanHelper.class).getLastTimeHPChanged(human.getID()) > 40) {
            return true;
        }

        int ttd = computeTTD(world.getEntity(human.getID())).first();
        int minTimeToAvailableRefuge = computeTimeToNearestAvailableRefuge(human.getPosition(world));
        int deltaBuriedness;

        if (!human.isBuriednessDefined()) {
            return true;
        }

        deltaBuriedness = (int) Math.ceil((float) human.getBuriedness() / numOfAgent);

        return ttd > (minTimeToAvailableRefuge + deltaBuriedness);

    }

    public Set<EntityID> findShouldCheckBuildings(Set<StandardEntity> myGoodHumans) {

        Set<EntityID> positionsToCheck = new FastSet<EntityID>();

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        Human human;
        for (StandardEntity humanEntity : myGoodHumans) {
            human = (Human) humanEntity;
            if (human.getPosition() != null && world.getEntity(human.getPosition()) instanceof Building && humanHelper.getCurrentHP(human.getID()) < 1000) {
                positionsToCheck.add(human.getPosition());
            }
        }
        return positionsToCheck;

    }

    /**
     * Computes distance between to entities
     *
     * @return an integer which shows the distance of two entities
     */
    public int computeDistance(EntityID fistPositionID, EntityID secondPositionID) {
        IPathPlanner pathPlanner = world.getPlatoonAgent().getPathPlanner();
        Area firstArea = world.getEntity(fistPositionID, Area.class);
        Area secondArea = world.getEntity(secondPositionID, Area.class);
        pathPlanner.planMove(firstArea, secondArea, 0, true);
        return pathPlanner.getPathCost();
//        return Util.distance(agent.getLocation(world), partition.getCenter());
    }


    /**
     * minimum needed AmbulanceTeam to rescue the human
     *
     * @param human the human to calculate its need
     * @return number of needed agents
     */
    public int computeMinimumNeededAgent(Human human) {

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        int minimumNeed = 0;
        //time to death
        int ttd;

        //Optimistic
//        ttd = humanHelper.getCurrentHP(human.getID()) / (humanHelper.getCurrentDamage(human.getID()) + 1);

        //realistic  without fire consideration
        ttd = computeTTD(human).first();

        //realistic  without fire consideration
//        ttd= TODO:-------------------------


        int ttr;
        //Time To Refuge
        if (human.isPositionDefined()) {
            ttr = computeTimeToNearestAvailableRefuge(human.getPosition(world));
        } else {
            ttr = 1000;
        }
        if (ttd > ttr) {
            minimumNeed = (int) Math.ceil((float) human.getBuriedness() / (ttd - ttr));
            if (minimumNeed == 0) {
                minimumNeed = 1;
            }
        } else {
            minimumNeed = 0;
        }
        return minimumNeed;
    }


}