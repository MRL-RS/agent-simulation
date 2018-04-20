/*
 * AssignmentAlgorithm.java
 *
 * Created on 10. December 2007, 20:34
 *
 * This file is part of the TimeFinder project.
 * Visit http://www.timefinder.de for more information.
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
 * Time: 7:21 PM
 */
public interface IAssignment {

    /**
     * Compute assignment based on given cost matrix
     * <b>Note:</b>There is no guarantee that the costMatrix will be cloned.
     *
     * @param costMatrix A matrix that contains costs for each agent to reach each target
     * @return A vector of
     */
    public int[] computeVectorAssignments(double[][] costMatrix);


}
