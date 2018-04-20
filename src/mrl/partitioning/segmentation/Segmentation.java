package mrl.partitioning.segmentation;

import mrl.world.MrlWorld;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/16/12
 * Time: 12:21 PM
 */


/**
 * This class determines operations needed for a segmentation method
 *
 * @param <T>
 */
public abstract class Segmentation<T> {

    protected MrlWorld world;

    public Segmentation(MrlWorld world) {
        this.world = world;
    }

    /**
     * Splits T object to n different segments.
     * <br><br></br> </br>
     * <b>Note:</b> T can be an <Link>EntityCluster</Link> or a Polygon;
     *
     * @param t the object to split
     * @param n number of needed segments
     * @return list of segmented objects as SegmentationResult
     * @see EntityCluster
     * @see java.awt.Polygon
     * @see SegmentationResult
     */
    public abstract SegmentationResult split(T t, int n);


}
