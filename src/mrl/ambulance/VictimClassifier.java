package mrl.ambulance;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.common.MRLConstants;
import mrl.helper.HumanHelper;
import mrl.helper.RoadHelper;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 3/4/13
 *         Time: 6:49 PM
 */
public class VictimClassifier {

    private MrlAmbulanceTeamWorld world;
    private Set<StandardEntity> myGoodHumans = new FastSet<StandardEntity>();// should think about these.
    private ArrayList<StandardEntity> healthyHumans = new ArrayList<StandardEntity>();
    private ArrayList<Civilian> shouldCarryToRefugeCivilians = new ArrayList<Civilian>();
    private ArrayList<StandardEntity> selectedHumans = new ArrayList<StandardEntity>(); //todo should Consider Humans
    private ArrayList<StandardEntity> myBadHumans = new ArrayList<StandardEntity>();// shouldn't think about these.

    private ArrayList<StandardEntity> unReachableHumans = new ArrayList<StandardEntity>(); //todo should Consider Humans
    private Map<EntityID, Integer> unreachableHumanTime = new FastMap<EntityID, Integer>();

    private Random rnd = new Random(345);

    public VictimClassifier(MrlAmbulanceTeamWorld world) {
        this.world = world;
    }

    public void updateGoodHumanList(Collection<StandardEntity> civilians, Set<StandardEntity> unReachablePositions, boolean needToExecute) {

        myGoodHumans.clear();
        myGoodHumans.addAll(civilians);
        myGoodHumans.addAll(world.getAmbulanceTeams());
        myGoodHumans.addAll(world.getPoliceForces());
        myGoodHumans.addAll(world.getFireBrigades());

        myGoodHumans.remove(world.getSelfHuman());

//        healthyHumans.removeAll(rescuedCivilians);

        myGoodHumans.removeAll(selectedHumans);
        myGoodHumans.removeAll(myBadHumans); //todo >>> isn't it better not to do this
//        myGoodHumans.removeAll(world.getRescuedCivilians());
        myGoodHumans.removeAll(world.getTransportingCivilians());
        myGoodHumans.removeAll(healthyHumans);

        removeAllVictimsWithEmptyBuilding(myGoodHumans, world.getEmptyBuildings());

//        if (!needToExecute) {
//        myGoodHumans.removeAll(unReachableHumans);
//        } else {
//            unReachableHumans.clear();
        //the reachability to an agent will be postponed to next random time between a 3-6
        updateUnreachableHumanList(unReachableHumans);

        myGoodHumans.removeAll(unReachableHumans);
        StandardEntity entity;


        for (EntityID id : world.getRescuedCivilians()) {
            entity = world.getEntity(id);
            if (entity instanceof Civilian) {
                myGoodHumans.remove(entity);
            }
        }

        ArrayList<Human> lowInfoHumans = new ArrayList<Human>();
        for (StandardEntity standardEntity : myGoodHumans) {
            Human h = (Human) standardEntity;
            if (h.isPositionDefined() && h.isHPDefined() && h.isDamageDefined() && h.isBuriednessDefined()) {
                if (h.getHP() == 0 || (h instanceof Civilian && h.getPosition(world) instanceof Refuge) || !(h.getPosition(world) instanceof Area)) {
                    lowInfoHumans.add(h);
                } else if (h.getDamage() > 0 && h.getBuriedness() == 0) {/*&& !(world.getEntity(h.getPosition()) instanceof Refuge)*/
                    {
                        if (!(h instanceof Civilian) || (world.getRefuges().isEmpty() && !(h.getPosition(world) instanceof Building))) {
//                    {shouldCarryToRefuge.add(h);
                            lowInfoHumans.add(h);
                        }
                    }
                } else if ((h.getDamage() == 0 && h.getBuriedness() == 0)) {
                    if (h instanceof Civilian && world.getEntity(h.getPosition()) instanceof Refuge) {
                        world.getRescuedCivilians().add(h.getID());
                        lowInfoHumans.add(h);
                    } else if ((h instanceof Civilian) && h.getPosition(world) instanceof Road) {
                        lowInfoHumans.add(h);

                    } else if (!(h instanceof Civilian)) {
//                        healthyHumans.add(h);
                        lowInfoHumans.add(h);
                    } else if ((!(h.getPosition(world) instanceof Building) && h.getHP() == 10000)/* || ((h.getPosition(world) instanceof Building) && h.getHP() == 10000)*/) {//
                        lowInfoHumans.add(h);
                    }
                } else if (!(h.getPosition(world) instanceof Area)) {
                    lowInfoHumans.add(h);
                }
            } else {
                lowInfoHumans.add(h);
            }
        }

        myGoodHumans.removeAll(lowInfoHumans);

        //remove unreachable positions
        ArrayList<StandardEntity> toRemoveEntities = new ArrayList<StandardEntity>();
        Human human;
        for (StandardEntity standardEntity : myGoodHumans) {
            human = (Human) standardEntity;
            if (unReachablePositions.contains(human.getPosition(world))) {
                toRemoveEntities.add(standardEntity);
            }
        }
        myGoodHumans.removeAll(toRemoveEntities);


        updateGoodHumanListBasedOnFiryness(myGoodHumans);

        updateBuriedAgentList(myGoodHumans);

    }

    private void removeAllVictimsWithEmptyBuilding(Set<StandardEntity> myGoodHumans, Set<EntityID> emptyBuildings) {
        Set<StandardEntity> toRemoveEntities = new HashSet<StandardEntity>();
        for (StandardEntity entity : myGoodHumans) {
            Human h = (Human) entity;
            if (h instanceof Civilian && h.isPositionDefined() && emptyBuildings.contains(h.getPosition())) {
                toRemoveEntities.add(entity);
            }
        }
        myGoodHumans.removeAll(toRemoveEntities);
    }

    private void updateGoodHumanListBasedOnFiryness(Set<StandardEntity> myGoodHumans) {
        ArrayList<StandardEntity> toRemove = new ArrayList<StandardEntity>();
        int nOAR;
        Building building;
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        for (StandardEntity standardEntity : myGoodHumans) {
            Human human = (Human) standardEntity;
            StandardEntity pos = human.getPosition(world);
            if (pos instanceof Building) {
                building = (Building) pos;
                if (building.isFierynessDefined() && (building.getFieryness() != 0) && (building.getFieryness() != 4 && (building.getFieryness() != 5))) {
                    if (world.getRefuges().isEmpty()) {
                        toRemove.add(human);
                        continue;
                    } else {
                        nOAR = humanHelper.getNumberOfATsRescuing(human.getID());
                        if ((nOAR > 0 && human.getBuriedness() / nOAR > 3) || (nOAR == 0 && human.getBuriedness() > 2)) {
                            toRemove.add(standardEntity);
                        }

                    }
                }
            }
        }

        myGoodHumans.removeAll(toRemove);
    }

    public void postponeBlockVictimTasks(Set<StandardEntity> myGoodHumans) {


        Human human;
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);

        for (StandardEntity standardEntity : myGoodHumans) {

            human = (Human) standardEntity;
            if (human == null || !human.isPositionDefined()) {
                continue;
            }
//            roadHelper.getRoadInfo().isPassable()
            if ((human.getPosition(world) instanceof Road)) {
                if (roadHelper.isPassable(human.getPosition())) {
//                System.out.println(world.getTime()+" simple%%%%:  --- " + world.getSelf().getID() + " target:" + human.getPosition());
                    addToUnreachableHumans(getUnReachableHumans(), human);
                }
            } else if (!world.getMrlBuilding(human.getPosition()).isOneEntranceOpen(world)) {
//                System.out.println(world.getTime()+" simple%%%%:  --- " + world.getSelf().getID() + " target:" + human.getPosition());
                addToUnreachableHumans(getUnReachableHumans(), human);
            }
//            if (world.getMrlBuilding(human.getPosition()).isOneEntranceSurlyOpen(world)) {
//                System.out.println("sureLy>>>>:  --- " + world.getSelf().getID() + " target:" + human.getPosition());
//            }

        }
    }


    // put a value between 5-10 to postpone considering of this human

    public void addToUnreachableHumans(ArrayList<StandardEntity> unReachableHumans, StandardEntity human) {
        int postponeTime = rnd.nextInt(6) + 5;
        unreachableHumanTime.put(human.getID(), postponeTime);
        if (!unReachableHumans.contains(human)) {
            unReachableHumans.add(human);
        }
        if (MRLConstants.DEBUG_AMBULANCE_TEAM)
            System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + ">>>>> PostPoned >>>> " + human.getID() + " " + postponeTime);
    }

    /**
     * if there can be found any open raod to the unreachable target, then remove this target from unreachable ones
     *
     * @param unReachableHumans humans whom there are no ways to
     */
    private void updateUnreachableHumanList(ArrayList<StandardEntity> unReachableHumans) {
        ArrayList<StandardEntity> toRemove = new ArrayList<StandardEntity>();
        int postponeTime = 0;
        for (StandardEntity standardEntity : unReachableHumans) {

            postponeTime = unreachableHumanTime.get(standardEntity.getID()); //todo:::::: NULLLLLLLLLL
            postponeTime--;
            if (postponeTime <= 0) {
                toRemove.add(standardEntity);
                unreachableHumanTime.remove(standardEntity.getID());
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + ">>>>>Removed From PostPoned >>>> " + standardEntity.getID() + " " + postponeTime);
            } else {
                unreachableHumanTime.put(standardEntity.getID(), postponeTime);
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.out.println(world.getTime() + " " + world.getSelf().getID() + " " + ">>>>> PostPoned >>>> " + standardEntity.getID() + " " + postponeTime);
            }

        }
        unReachableHumans.removeAll(toRemove);


//        RoadHelper roadHelper = world.getHelper(RoadHelper.class);
//        BuildingHelper buildingHelper = world.getHelper(BuildingHelper.class);
//        ArrayList<Human> reachabletargets = new ArrayList<Human>();
//        for (StandardEntity standardEntity : unReachableHumans) {
//            Human human = (Human) standardEntity;
//            Area area = (Area) human.getPosition(world);
//            if (area instanceof Road) {
//                if (roadHelper.isPassable(area.getID())) {
//                    reachabletargets.add(human);
//                }
//            } else {
//                for (Entrance entrance : buildingHelper.getEntrances(area.getID())) {
//                    if (entrance.getNeighbour() instanceof Road) {
//                        Road road = (Road) entrance.getNeighbour();
//                        if (roadHelper.isPassable(road.getID())) {
//                            reachabletargets.add(human);
//                            break;
//                        }
//
//                    }
//
//                }
//            }
//        }
//
//        unReachableHumans.removeAll(reachabletargets);

    }

    /**
     * remove rescued agents from world.getBuriedAgents() list
     *
     * @param myGoodHumans list of humans that should be rescued
     */
    private void updateBuriedAgentList(Set<StandardEntity> myGoodHumans) {

        ArrayList<Human> shouldRemoveAgents = new ArrayList<Human>();

        for (EntityID id : world.getBuriedAgents()) {
            Human human = (Human) world.getEntity(id);
            if (!(myGoodHumans.contains(human))) {
                shouldRemoveAgents.add(human);
            }
        }

        world.getBuriedAgents().removeAll(shouldRemoveAgents);
    }


    public Set<StandardEntity> getMyGoodHumans() {
        return myGoodHumans;
    }

    public ArrayList<StandardEntity> getUnReachableHumans() {
        return unReachableHumans;
    }

    public ArrayList<StandardEntity> getMyBadHumans() {
        return myBadHumans;
    }

    public ArrayList<StandardEntity> getSelectedHumans() {
        return selectedHumans;
    }
}
