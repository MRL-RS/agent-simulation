package mrl.platoon.genericsearch;

import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Siavash
 */
public class StupidSearchDecisionMaker extends SearchDecisionMaker {

    private static Log logger = LogFactory.getLog(StupidSearchDecisionMaker.class);
    private List<Path> visitedPaths = new ArrayList<Path>();

    public StupidSearchDecisionMaker(MrlWorld world) {
        super(world);
    }

    @Override
    public void initialize() {

    }

    @Override
    public List<Area> evaluateTargets() {
        throw new UnsupportedOperationException("Not Supported yet.");
    }

    @Override
    public Path getNextPath() {
        Random random = new Random(System.currentTimeMillis());
        if (searchInPartition) {
            logger.debug("Fetching next random path from partition.");
            Partition partition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
            if (partition != null) {
                if (partition.getPaths().size() > 0) {
//                    int index = random.nextInt(partition.getPaths().size() - 1);
//                    for (Path p : partition.getPaths()) {
//                        if (index-- == 0) {
//                            return p;
//                        }
//                    }

                    int minDistance = Integer.MAX_VALUE;
                    int distance = 0;
                    Path selectedPath = null;

                    for (Path p : partition.getPaths()) {
                        if (visitedPaths.contains(p)) {
                            continue;
                        }

                        distance = world.getDistance(world.getSelf().getID(), p.getMiddleRoad().getID());
                        if (minDistance > distance) {
                            minDistance = distance;
                            selectedPath = p;
                            visitedPaths.add(selectedPath);
                        }
                    }
                    return selectedPath;
                }

            } else {
                logger.debug("Fetching next random path.");
                Path path;
                int counter = 0;
                do {
                    path = world.getPaths().get(random.nextInt(world.getPaths().size() - 1));
                } while (path.getBuildings().isEmpty() || ++counter < 100);

                return path;
            }

        } else {
            logger.debug("Fetching next random path.");
            Path path;
            int counter = 0;
            do {
                path = world.getPaths().get(random.nextInt(world.getPaths().size() - 1));

            } while (path.getBuildings().isEmpty() || ++counter < 100);

            return path;
        }

        return null;
    }

    @Override
    public Area getNextArea() {
        Random random = new Random(System.currentTimeMillis());
        Road target = null;
        if (searchInPartition) {
            logger.debug("Fetching next random path from partition.");
            Partition partition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
            int capSize = partition.getPaths().size();

            if (capSize < 2) {
                logger.debug("There is not much option, i'll have to stay where i am.");
                return null;
            }

            int index = random.nextInt(capSize);
            for (Path p : partition.getPaths()) {
                if (index-- == 0) {
                    target = p.getMiddleRoad();
                    break;
                }
            }

            if (world.getSelfPosition().getID().equals(target.getID())) {
                target = null;
            }

        } else {
            logger.debug("Fetching next random path.");
            Path path;
            int counter = 0;
            do {
                path = world.getPaths().get(random.nextInt(world.getPaths().size() - 1));
            } while (path.getBuildings().isEmpty() || ++counter < 100);
            target = path.getMiddleRoad();
        }
        return target;
    }
}
