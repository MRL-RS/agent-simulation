package mrl.police;

import javolution.util.FastMap;
import mrl.helper.IHelper;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: roohola
 * Date: 4/1/11
 * Time: 7:57 PM
 */
public class TargetToGoHelper implements IHelper {


    MrlWorld world;
    // map target to Agents.
    Map<EntityID, TargetToGoOBJ> targetsMap = new FastMap<EntityID, TargetToGoOBJ>();

    public TargetToGoHelper(MrlWorld world) {
        this.world = world;
    }

    @Override
    public void init() {
    }

    @Override
    public void update() {

//        RoadHelper roadHelper = world.getHelper(RoadHelper.class);
//        List<EntityID> toDelete = new ArrayList<EntityID>();
//
//        for (EntityID id : targetToGo) {
//            StandardEntity entity = world.getEntity(id);
//            if (entity instanceof Building) {
//                List<Entrance> entrances = world.getMrlBuilding(entity.getID()).getEntrances();
//                for (Entrance entrance : entrances) {
//                    if (roadHelper.isSeenAndBlocked(entrance.getNeighbour())) {
//                        toDelete.add(id);
//                        break;
//                    }
//                }
//            } else if (entity instanceof Road) {
//                if (roadHelper.isPassable(entity.getID()))
//                    toDelete.add(id);
//            }
//        }
//        getTargetToGo().removeAll(toDelete);

    }

    public List<Path> getPaths() {
        List<Path> paths = new ArrayList<Path>();
        StandardEntity entity;

        for (TargetToGoOBJ target : targetsMap.values()) {

            for (EntityID id : target.reporters) {
                entity = world.getEntity(id);
                if (entity instanceof FireBrigade) {

                } else {
                    paths.add(target.path);
                }
            }
        }
        return paths;
    }

    public void addTargetToGo(EntityID target, EntityID sender) {
        StandardEntity senderEntity = world.getEntity(sender);
        TargetToGoOBJ targetToGoOBJ = targetsMap.get(target);
        int targetToGoPriority = 1;

        if (senderEntity instanceof AmbulanceTeam) {
            targetToGoPriority = 3;
        }
        if (targetToGoOBJ == null) {
            targetToGoOBJ = new TargetToGoOBJ(sender, world.getTime(), world.getPathsOfThisArea((Area) world.getEntity(target)).get(0));
        }
        targetToGoOBJ.addReporter(sender, targetToGoPriority);

        targetsMap.put(target, targetToGoOBJ);
    }

}
