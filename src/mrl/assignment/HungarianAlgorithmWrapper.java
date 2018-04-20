package mrl.assignment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Siavash
 */
public class HungarianAlgorithmWrapper implements IAssignment {
    /**
     * Compute assignment based on given cost matrix
     * <b>Note:</b>There is no guarantee that the costMatrix will be cloned.
     *
     * @param costMatrix A matrix that contains costs for each agent to reach each target
     * @return A vector of
     */
    @Override
    public int[] computeVectorAssignments(double[][] costMatrix) {
        HungarianAssignment hungarianAssignment = new HungarianAssignment(costMatrix);
        int[] temp = hungarianAssignment.execute();
        int[] result = null;
        List<Integer> tempList = new ArrayList<Integer>();

        if (temp != null) {
            for (int i : temp) {
                if (i != -1) {
                    tempList.add(i);
                }
            }

            result = new int[tempList.size()];

            for (int i = 0; i < temp.length; i++) {
                if (temp[i] != -1) {
                    result[temp[i]] = i;
                }
            }

        }

        return result;

    }
}
