package mrl.assignment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * @author Siavash
 *         test class for HungarianAssignment class
 */
public class HungarianAlgorithmWrapperTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testComputeVectorAssignments() throws Exception {

        double[][] inputFirst = {{10, 19, 8, 15},
                {10, 18, 7, 17},
                {13, 16, 9, 14},
                {12, 19, 8, 18},
                {14, 17, 10, 19}};

        double[][] inputSecond = {{2, 5, 3, 8, 1, 2, 2},
                {4, 3, 6, 8, 10, 1, 2},
                {4, 7, 7, 3, 5, 4, 3},
                {4, 5, 6, 9, 3, 3, 10},
                {1, 6, 9, 8, 2, 10, 1},
                {9, 4, 5, 5, 6, 8, 4},
                {5, 3, 7, 1, 6, 6, 2},
                {9, 10, 1, 7, 6, 4, 1},
                {2, 2, 5, 3, 9, 10, 4}};

        double[][] inputThird = {
                {310475, 130299, 463963},
                {227738, 363665, 450895},
                {622516, 670718, 329081},
                {105042, 413418, 583836},
                {573936, 460690, 108141},
                {459601, 113495, 611510}};


        int[] expectedFirst = {0, 4, 1, 2};
        int[] expectedSecond = {4, 8, 7, 6, 0, 1, 2};
        int[] expectedThird = {3, 5, 4,};
        int[] result;


        boolean hasUniqueValues;
        IAssignment assignment = new HungarianAlgorithmWrapper();

        result = assignment.computeVectorAssignments(inputFirst);
        assertNotNull(result);
        System.out.println(Arrays.toString(result));
        assertTrue("these two should be equal", Arrays.equals(result, expectedFirst));

        result = assignment.computeVectorAssignments(inputSecond);
        assertNotNull(result);
        System.out.println(Arrays.toString(result));
        assertTrue("these two should be equal", Arrays.equals(result, expectedSecond));

        result = assignment.computeVectorAssignments(inputThird);
        assertNotNull(result);
        System.out.println(Arrays.toString(result));
        assertTrue("these two should be equal", Arrays.equals(result, expectedThird));

        for (int i = 0; i < 1000; i++) {
            inputFirst = createRandomInput();
            result = assignment.computeVectorAssignments(inputFirst);
            assertNotNull(result);
            hasUniqueValues = hasUniqueValues(result);
            if (!hasUniqueValues) {

                System.out.println("This has not unique values:" + Arrays.toString(result));
            }
            assertTrue("The result should have unique values", hasUniqueValues);
            assertEquals("Assignment result and number of columns must be equal", inputFirst[0].length, result.length);
        }
    }

    private double[][] createRandomInput() {
        double[][] result;
        Random random = new Random(System.currentTimeMillis());
        int randomInt1 = random.nextInt(100) + 1;
        int randomInt2 = random.nextInt(100) + 1;
        int rows, columns;

        if (randomInt1 >= randomInt2) {
            rows = randomInt1;
            columns = randomInt2;
        } else {
            rows = randomInt2;
            columns = randomInt1;
        }

        result = new double[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                result[i][j] = random.nextDouble();
            }
        }

        return result;

    }

    private boolean hasUniqueValues(int[] values) {
        Set<Integer> integers = new HashSet<Integer>();
        boolean result = false;
        for (int i : values) {
            if (!integers.contains(i)) {
                integers.add(i);
            }
        }

        if (values.length == integers.size()) {
            result = true;
        }

        return result;
    }
}
