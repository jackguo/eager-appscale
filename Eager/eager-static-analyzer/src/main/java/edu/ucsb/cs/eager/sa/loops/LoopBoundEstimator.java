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

package edu.ucsb.cs.eager.sa.loops;

import edu.ucsb.cs.eager.sa.loops.ai.AbstractStateDomain;
import edu.ucsb.cs.eager.sa.loops.ai.IntervalDomain;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JGeExpr;
import soot.jimple.toolkits.annotation.logic.Loop;

import java.util.*;

public class LoopBoundEstimator {

    public static int estimate(Loop loop, Map<Value,AbstractStateDomain> variables) {
        IfStmt loopExit = findLoopExit(loop, variables);
        Expr condition = (Expr) loopExit.getCondition();

        Map<Value,AbstractStateDomain> tempVars = new HashMap<Value, AbstractStateDomain>();
        if (condition instanceof JGeExpr) {
            Value leftOp = ((JGeExpr) condition).getOp1();
            Value rightOp = ((JGeExpr) condition).getOp2();
            if (rightOp instanceof IntConstant) {
                AbstractStateDomain dom = variables.get(leftOp);
                if (dom != null) {
                    if (dom instanceof IntervalDomain) {
                        int low = ((IntervalDomain) dom).getLowerBound();
                        int high = ((IntConstant) rightOp).value - 1;
                        if (low <= high) {
                            tempVars.put(leftOp, new IntervalDomain(low, high));
                        } else {
                            return 0;
                        }
                    }
                }
            }
        }

        // TODO: Walk the loop code

        if (tempVars.size() > 0) {
            int bound = 1;
            for (AbstractStateDomain dom : tempVars.values()) {
                bound *= dom.getStates();
            }
            return bound;
        }
        return -1;
    }

    private static IfStmt findLoopExit(Loop loop, Map<Value,AbstractStateDomain> variables) {
        for (Stmt stmt : loop.getLoopStatements()) {
            if (stmt instanceof IfStmt) {
                return (IfStmt) stmt;
            } else if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                // TODO: update variables map
            }
        }
        throw new IllegalStateException("No loop exit found");
    }
}
