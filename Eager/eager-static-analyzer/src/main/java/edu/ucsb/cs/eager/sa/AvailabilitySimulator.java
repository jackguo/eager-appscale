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

public class AvailabilitySimulator extends AbstractSimulator {

    @Override
    public double getInitialValue() {
        return 100.0;
    }

    @Override
    public double aggregateInstructionResult(double current, double update) {
        return Math.min(current, update);
    }

    @Override
    protected double simulateSpecialInvokeInstruction(InvokeExpr invocation, SootMethod method) {
        if (method.getName().equals("query1")) {
            return 99.99;
        }
        return 99.999;
    }

    @Override
    protected double simulateRegularInvokeInstruction(InvokeExpr invocation, SootMethod method) {
        return 100.0;
    }

    @Override
    protected double simulateNonInvokeInstruction(Stmt stmt) {
        return 100.0;
    }

    @Override
    public double summarize(double[] results) {
        double min = results[0];
        for (int i = 1; i < results.length; i++) {
            if (results[i] < min) {
                min = results[i];
            }
        }
        return min;
    }
}
