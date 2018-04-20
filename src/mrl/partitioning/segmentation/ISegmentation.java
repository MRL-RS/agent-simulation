package mrl.partitioning.segmentation;


import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/12/12
 * Time: 5:15 PM
 */
public interface ISegmentation {

    /**
     * This method segments a polygon to some smaller polygons based on imported points
     *
     * @param polygon polygon to be segmented
     * @param points  points to do segmentation based on them
     * @return segmented polygons
     */
    List<Polygon> doSegmentation(Polygon polygon, List<Point2D> points);


}
