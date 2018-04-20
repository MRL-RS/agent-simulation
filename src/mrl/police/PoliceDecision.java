package mrl.police;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.HumanHelper;
import mrl.world.object.Entrance;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: roohola
 * Date: 6/15/11
 * Time: 3:42 PM
 */
public class PoliceDecision {
    protected MrlPoliceForceWorld world;
    protected Set<Path> importantPaths = new FastSet<Path>();

    protected List<Path> sortedImportantPaths = new ArrayList<Path>();

    protected List<Path> refugePath = new ArrayList<Path>();
    protected List<Path> blockAgentPath = new ArrayList<Path>();
    protected List<Path> buriedAgentPath = new ArrayList<Path>();
    protected List<Path> firePath = new ArrayList<Path>();
    protected List<Path> targetToGoPaths = new ArrayList<Path>();

    protected double refugePathCoef = 12.1;
    protected double blockAgentPathCoef = 10.3;
    protected double buriedAgentPathCoef = 6.4;
    protected double targetToGoPathFromATCoef = 5.0;
    //    protected double targetToGoPathFromFBCoef = 4.5;
    protected double firePathCoef = 3.6;


    Map<EntityID, Path> taskAssignment = new FastMap<EntityID, Path>();


    public PoliceDecision(MrlPoliceForceWorld world) {
        this.world = world;
        initialRefugePaths();
    }


    private void initialRefugePaths() {
        Map<Refuge, Integer> refuges = new FastMap<Refuge, Integer>();

        for (StandardEntity entity : world.getRefuges()) {
            Refuge refuge = (Refuge) entity;
            refuges.put(refuge, world.getDistance(refuge, world.getCenterOfMap()));
        }
        List<Refuge> sorted = Util.sortByValueInc(refuges);

        for (Refuge refuge : sorted) {
            for (Entrance entrance : world.getMrlBuilding(refuge.getID()).getEntrances()) {
                Path path = world.getPaths().getRoadPath(entrance.getID());
                refugePath.add(path);
                path.addImportantRoad(entrance.getNeighbour());
            }
        }
    }

    public Path assign() {
        StandardEntity nearestAgent;
        Collection<StandardEntity> freeAgents = new ArrayList<StandardEntity>(world.getPoliceForces());
        List<StandardEntity> toRemoveFreeAgents = new ArrayList<StandardEntity>();
        List<EntityID> toRemove = new ArrayList<EntityID>();
        for (EntityID id : taskAssignment.keySet()) {
            Path path = taskAssignment.get(id);
            if (path.isItOpened()) {
                toRemove.add(id);
            } else {
                toRemoveFreeAgents.add(world.getEntity(id));
            }
        }
        for (EntityID id : toRemove) {
            taskAssignment.remove(id);
        }
        for (StandardEntity entity : freeAgents) {
            Human human = (Human) entity;
            if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
                toRemoveFreeAgents.add(human);
            }
        }
        freeAgents.removeAll(toRemoveFreeAgents);

        for (Path path : sortedImportantPaths) {
            if (freeAgents.isEmpty()) {
                break;
            }
            path.updateAgentDistance(freeAgents);
            List<EntityID> agents = Util.sortByValueInc(path.getAgentDistanceMap());

            int needed = path.getNeededAgentsToSearch();

            for (int i = 0; i < needed && i < agents.size(); i++) {
                nearestAgent = world.getEntity(agents.get(i));
                freeAgents.remove(nearestAgent);
                taskAssignment.put(nearestAgent.getID(), path);
            }

        }
        return taskAssignment.get(world.getSelf().getID());

    }

    public Path getMyTask() {
        List<EntityID> toRemove = new ArrayList<EntityID>();
        for (EntityID id : taskAssignment.keySet()) {
            Path path = taskAssignment.get(id);
            if (path.isItOpened()) {
                toRemove.add(id);
            }
        }
        for (EntityID id : toRemove) {
            taskAssignment.remove(id);
        }
        List<Path> paths = getSortedImportantPaths();
        Integer healthyPoliceCount = null;

        while (true) {
            for (Path path : paths) {
                List<EntityID> agents = Util.sortByValueInc(path.getAgentDistanceMap());
                if (healthyPoliceCount == null) {
                    healthyPoliceCount = agents.size();
                }
                boolean pathAssignedOrNoMoreAgent = agents.isEmpty();
                int assignedAgentCount = 0;
                int neededAgentsToClearBlock = 1;
                while (!pathAssignedOrNoMoreAgent) {
                    for (EntityID id : agents) {
                        pathAssignedOrNoMoreAgent = assignedAgentCount >= neededAgentsToClearBlock || taskAssignment.size() >= healthyPoliceCount || taskAssignment.size() >= agents.size();
                        if (pathAssignedOrNoMoreAgent) {
                            break;
                        }
                        if (!taskAssignment.containsKey(id)) {
                            assignedAgentCount++;
                            taskAssignment.put(id, path);
                        }

                    }


                }
            }
            if (healthyPoliceCount == null || healthyPoliceCount <= taskAssignment.size() || isPoliceAssignLimitExceeded()) {
                break;
            }
        }

        return taskAssignment.get(world.getSelf().getID());

    }

    private boolean isPoliceAssignLimitExceeded() {
        Map<Path, Integer> map = new FastMap<Path, Integer>();
        for (Path path : taskAssignment.values()) {
            Integer integer = map.get(path);
            if (integer == null) {
                integer = 0;
            }
            integer++;
            map.put(path, integer);

        }
        for (Integer i : map.values()) {
            if (i > 3) {
                return true;
            }
        }
        return false;
    }

    public void update() {
        importantPaths.clear();

        // -------------- Refuge-------------
        updateRefugePath();
        // -------------- Block Agents -------------
        updateBlockAgentPaths();
        // -------------- Buried Agent -------------
        updateBuriedAgent();
        //-------------- Target To Go Paths-------------
//        updateTargetToGoPath();
        // -------------- Fire -------------
        updateFirePath();

        //-------------- Final Value --------------
        updatePathValues();

        sortedImportantPaths.clear();
        sortedImportantPaths.addAll(importantPaths);
        Collections.sort(sortedImportantPaths);

        if (MRLConstants.DEBUG_POLICE_FORCE) {
            world.printData(sortedImportantPaths.toString());
        }
    }

    private void updatePathValues() {
        for (Path path : importantPaths) {
            path.resetValue();
        }

        double additionalCoef = world.getRefuges().size();
        int numberOfRefugesToClear = Math.max((int) Math.ceil(refugePath.size() * 0.30), Math.min(3, refugePath.size()));

        if (additionalCoef <= 0) {
            additionalCoef = 1;
        }

        if (!refugePath.isEmpty()) {
            double openRefuge = world.getRefuges().size() - refugePath.size();
            if (openRefuge <= 0) {
                openRefuge = 1;
            }
            double d = world.getRefuges().size() / openRefuge;
            int importance = refugePath.size();

            for (Path path : refugePath) {
                double addition = refugePathCoef * d * importance;
                path.setValue(path.getValue() + addition);
                if (--numberOfRefugesToClear > 0) {
                    importance = 1;
                }
                if (importance > 1) {
                    importance--;
                }
            }

        }


        for (Path path : blockAgentPath) {
            double addition = blockAgentPathCoef * additionalCoef * Math.sqrt(additionalCoef);
            path.setValue(path.getValue() + addition);
        }

        for (Path path : buriedAgentPath) {
            double addition = buriedAgentPathCoef * additionalCoef * Math.sqrt(additionalCoef);
            path.setValue(path.getValue() + addition);
        }

        double newValue;
        for (Path path : targetToGoPaths) {
            double addition = targetToGoPathFromATCoef * additionalCoef * Math.sqrt(additionalCoef);

            newValue = path.getValue() + addition;
//            System.out.println(world.getTime()+" "+world.getSelf().getID()+ " pathMR_ID:"+path.getMiddleRoad().getID()+" BValue:"+path.getValue()+" addition:"+addition +" NewValue:"+newValue);
            path.setValue(path.getValue() + addition);
        }

        for (Path path : firePath) {
            double addition = firePathCoef * additionalCoef * Math.sqrt(additionalCoef);
            path.setValue(path.getValue() + addition);
        }

    }

    private void updateFirePath() {
        firePath.clear();
        int index = 0;
        for (MrlZone zone : world.getZones().getBurningZones()) {
            for (Path path : zone.getPaths()) {
                if (!path.isItOpened()) {
                    firePath.add(path);
                    importantPaths.add(path);
                }
            }
            if (++index > 4) {
                break;
            }
        }
    }

    private void updateTargetToGoPath() {
/*//        List<Path> toRemove = new ArrayList<Path>();
        targetToGoPaths.clear();

        for (Path path : world.getTargetToGoHelper().getPaths()) {
            if (!path.isItOpened()) {
                targetToGoPaths.add(path);
                importantPaths.add(path);
//            } else {
//                toRemove.add(path);
            }
        }
//        targetToGoPaths.removeAll(toRemove);*/
    }

    private void updateBuriedAgent() {
        buriedAgentPath.clear();
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        for (EntityID id : humanHelper.getBuriedAgents()) {
            Human human = (Human) world.getEntity(id);
            if (human.isPositionDefined()) {

                EntityID entityID = human.getPosition();
                StandardEntity standardEntity = world.getEntity(entityID);
                if (standardEntity instanceof Road) {
                    Path path = world.getPaths().getRoadPath(standardEntity.getID());
                    if (!path.isItOpened()) {
                        buriedAgentPath.add(path);
                        importantPaths.add(path);
                        path.addImportantRoad((Road) standardEntity);
                    }

                } else if (standardEntity instanceof Building) {
                    for (Entrance entrance : world.getMrlBuilding(standardEntity.getID()).getEntrances()) {
                        Path path = world.getPaths().getRoadPath(entrance.getID());
                        if (!path.isItOpened()) {
                            buriedAgentPath.add(path);
                            importantPaths.add(path);
                            path.addImportantRoad(entrance.getNeighbour());
                        }

                    }
                }
            }
        }
    }

    private void updateBlockAgentPaths() {
        blockAgentPath.clear();
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        for (Human human : humanHelper.getBlockedAgents()) {

            if ((human instanceof FireBrigade || human instanceof AmbulanceTeam) && human.isPositionDefined()) {

                EntityID entityID = human.getPosition();
                StandardEntity standardEntity = world.getEntity(entityID);
                if (standardEntity instanceof Road) {
                    Path path = world.getPaths().getRoadPath(standardEntity.getID());
                    if (!path.isItOpened()) {
                        blockAgentPath.add(path);
                        importantPaths.add(path);
                        path.addImportantRoad((Road) standardEntity);
                    }
                } else if (standardEntity instanceof Building) {
                    for (Entrance entrance : world.getMrlBuilding(standardEntity.getID()).getEntrances()) {
                        Path path = world.getPaths().getRoadPath(entrance.getID());
                        if (!path.isItOpened()) {
                            blockAgentPath.add(path);
                            importantPaths.add(path);
                            path.addImportantRoad(entrance.getNeighbour());
                        }
                    }
                }
            }
        }
    }


    private void updateRefugePath() {
        List<Path> toRemove = new ArrayList<Path>();

        for (Path path : refugePath) {
            if (path.isItOpened()) {
                toRemove.add(path);
            } else {
                importantPaths.add(path);
            }
        }
        refugePath.removeAll(toRemove);
    }

    private int repeatNumber(List<Path> list, Path path) {
        int counter = 0;
        for (Path p : list) {
            if (path.equals(p)) {
                counter++;
            }
        }
        return counter;
    }

    public List<Path> getSortedImportantPaths() {
        return sortedImportantPaths;
    }


    /*
      //  -----   TargetToGo Particles
      Road road = null;
      for (TargetToGoMessage targetToGo : world.getTargetToGoList()) {
          Human human = (Human) world.getEntity(new EntityID(targetToGo.getId()));
          Area area = (Area) world.getEntity(new EntityID(targetToGo.getMotionLessPosition()));
          if (area instanceof Road) {
              road = (Road) area;
          }
          if (area instanceof Building) {
              Building building = (Building) area;
              road = world.connectedRoad(building, true);
          }

          targetParticles.add(new TargetParticle(road, world));
      }
    */

    public Map<EntityID, Path> getTaskAssignment() {
        return taskAssignment;
    }
}
