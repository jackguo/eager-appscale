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

import edu.ucsb.cs.eager.sa.loops.ai.IntegerInterval;
import soot.IntType;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.annotation.logic.Loop;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LoopBoundAnalysis {

    public static int estimateBound(Stmt loopHead, Collection<Loop> loops,
                             Map<Value,IntegerInterval> variables) {

        Loop loop = findLoop(loopHead, loops);
        Collection<Stmt> exits = loop.getLoopExits();
        Map<Value,IntegerInterval> tempVariables = new HashMap<Value, IntegerInterval>();
        Map<Value,IntegerInterval> loopRanges = new HashMap<Value, IntegerInterval>();
        for (Stmt exit : exits) {
            for (ValueBox useBox : exit.getUseBoxes()) {
                if (useBox.getValue() instanceof JimpleLocal) {
                    IntegerInterval current = variables.get(useBox.getValue());
                    if (current != null) {
                        tempVariables.put(useBox.getValue(), current.clone());
                    }
                }
            }

            Value condition = ((IfStmt) exit).getCondition();
            if (condition instanceof GeExpr) {
                Value op1 = ((GeExpr) condition).getOp1();
                Value op2 = ((GeExpr) condition).getOp2();
                if (op2 instanceof IntConstant) {
                    int value = ((IntConstant) op2).value;
                    IntegerInterval interval = tempVariables.get(op1);
                    if (interval != null) {
                        if (interval.getLowerBound() <= value) {
                            loopRanges.put(op1, new IntegerInterval(interval.getLowerBound(), value));
                        } else {
                            return 0;
                        }
                    } else {
                        loopRanges.put(op1, new IntegerInterval(0, value));
                    }
                }
            }
        }

        // TODO: Analyze each loop exit and figure out a range for each variable
        // TODO: Analyze loop code to see how fast the variables grow
        // TODO: Use the above values to make an estimation about the loop bound
        return -1;
    }

    private static Loop findLoop(Unit unit, Collection<Loop> loops) {
        for (Loop loop : loops) {
            if (loop.getHead().equals(unit)) {
                return loop;
            }
        }
        return null;
    }

}
