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
public class GreedyAssignmentTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testComputeVectorAssignments() throws Exception {


        int[] result;
        double[][] input;

        boolean hasUniqueValues;
        IAssignment assignment = new GreedyAssignment();


        for (int i = 0; i < 1000; i++) {
            input = createRandomInput();
            result = assignment.computeVectorAssignments(input);
            assertNotNull(result);
            hasUniqueValues = hasUniqueValues(result);
            if (!hasUniqueValues) {

                System.out.println("This has not unique values:" + Arrays.toString(result));
            }
            assertTrue("The result should have unique values", hasUniqueValues);
            assertEquals("Assignment result and number of columns must be equal", input[0].length, result.length);
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
