package mrl.partitioning;

import mrl.world.MrlWorld;

import java.awt.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mrl
 * Date: 6/3/12
 * Time: 10:20 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IPartitionMaker {

    public List<Partition> makePartitions(MrlWorld world, List<Polygon> polygons);
}
