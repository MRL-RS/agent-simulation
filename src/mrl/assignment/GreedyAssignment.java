/*
 * This file is part of the TimeFinder project.
 * Visit http://www.timefinder.de for more information.
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mrl.assignment;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/13/12
 * Time: 7:26 PM
 */
public class GreedyAssignment implements IAssignment {


    @Override
    public int[] computeVectorAssignments(double[][] costMatrix) {
        //matrix[y][x] => element x,y = matrix[row][column]
        //matrix[0] => first rows
        int ROWS = costMatrix.length;
        int COLUMNS = costMatrix[0].length;

        assert ROWS > 0 : "Matrix should have at least one entry";
        assert COLUMNS > 0 : "Matrix should have at least one entry";

        boolean coveredRows[] = new boolean[ROWS];
        boolean coveredCols[] = new boolean[COLUMNS];
        double minValInCol;

//        // find the min value in each row
//        double minValInRow;
//        for (int row = 0; row < ROWS; row++) {
//            minValInRow = Double.MAX_VALUE;
//            for (int col = 0; col < COLUMNS; col++) {
//                if (minValInRow > costMatrix[row][col]) {
//                    minValInRow = costMatrix[row][col];
//                }
//            }
//
//            // subtract it from all values in the row
//            if (minValInRow < Double.MAX_VALUE) {
//                for (int col = 0; col < COLUMNS; col++) {
//                    if (costMatrix[row][col] < Double.MAX_VALUE) {
//                        costMatrix[row][col] -= minValInRow;
//                    }
//                }
//            } else {
//                coveredRows[row] = true;
//            }
//        }
//
//        //do the same for the columns
//        double minValInCol = Double.MAX_VALUE;
//        for (int col = 0; col < COLUMNS; col++) {
//            minValInCol = Double.MAX_VALUE;
//            for (int row = 0; row < ROWS; row++) {
//                if (minValInCol > costMatrix[row][col]) {
//                    minValInCol = costMatrix[row][col];
//                }
//            }
//
//            if (minValInCol < Double.MAX_VALUE) {
//                for (int row = 0; row < ROWS; row++) {
//                    if (costMatrix[row][col] < Double.MAX_VALUE) {
//                        costMatrix[row][col] -= minValInCol;
//                    }
//                }
//            } else {
//                coveredCols[col] = true;
//            }
//        }

        // now pick the zeros with a simple approach: 'first zeros win'
        int result[] = new int[COLUMNS];
        for (int col = 0; col < COLUMNS; col++) {

            if (!coveredCols[col]) {
                // find again the better minimum
                minValInCol = Double.MAX_VALUE;
                int goodRow = 0;
                for (int row = 0; row < ROWS; row++) {
                    if (!coveredRows[row] && minValInCol > costMatrix[row][col]) {
                        minValInCol = costMatrix[row][col];
                        goodRow = row;
                    }
                }
                if (minValInCol < Double.MAX_VALUE) {
                    coveredRows[goodRow] = true;
                    coveredCols[col] = true;
                    result[col] = goodRow;
                }
            }
        }
        return result;
    }
}
