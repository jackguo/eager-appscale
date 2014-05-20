/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package edu.ucsb.cs.eager.sa;

import soot.jimple.Stmt;

/**
 * Simulates the execution of a program, and compute an interesting parameter related to the
 * program (e.g. execution time).
 */
public interface InstructionSimulator {

    /**
     * Get the initial value of the parameter to be computed.
     *
     * @return a double value
     */
    public double getInitialValue();

    /**
     * Simulate the execution of a single instruction
     *
     * @param stmt Instruction Stmt to be simulated
     * @return a double value
     */
    public double simulateInstruction(Stmt stmt);

    /**
     * This method is called after the execution/simulation of each instruction to update the
     * value of the parameter that is being calculated by the simulation.
     *
     * @param current Latest value of the parameter
     * @param update Value obtained by simulating the last instruction
     * @return a double value that aggregates the current and update values
     */
    public double aggregateInstructionResult(double current, double update);

    /**
     * In the event the same simulation is being repeated multiple times (e.g. Monte Carlo method),
     * this method is used to aggregate the parameter values computed in each simulation run.
     *
     * @param results An array of parameter values computed in different iterations
     * @return a double value that aggregates all results computed
     */
    public double summarize(double[] results);

    public BranchSelector getBranchSelector();

}
