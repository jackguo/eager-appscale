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

import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class PerformanceSimulator extends AbstractSimulator {

    public PerformanceSimulator(BranchSelector selector) {
        super(selector);
    }

    @Override
    public double getInitialValue() {
        return 0.0;
    }

    @Override
    public double aggregateInstructionResult(double current, double update) {
        return current + update;
    }

    @Override
    protected double simulateSpecialInvokeInstruction(InvokeExpr invocation, SootMethod method) {
        return 1.0;
    }

    public double simulateRegularInvokeInstruction(InvokeExpr invocation, SootMethod method) {
        return 0.0;
    }

    public double simulateNonInvokeInstruction(Stmt stmt) {
        return 0.0;
    }

    @Override
    public double summarize(double[] results) {
        double sum = 0.0;
        for (double r : results) {
            sum += r;
        }
        return sum / results.length;
    }

}
