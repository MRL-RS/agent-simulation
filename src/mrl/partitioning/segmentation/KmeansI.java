package mrl.partitioning.segmentation;

import java.util.ArrayList;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/9/12
 *         Time: 5:43 AM
 */
public interface KmeansI {
    Double getTotalVar();

    Double[] getClusterVars();

    ArrayList[] getClusters();

    void calculateClusters();

    void setEpsilon(Double epsilon);

    Double[][] getClusterCenters();
}
