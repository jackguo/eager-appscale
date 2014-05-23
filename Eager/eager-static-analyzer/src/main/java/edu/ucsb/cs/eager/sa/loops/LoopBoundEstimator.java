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

import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;

import java.util.Map;

public class LoopBoundEstimator {

    public static int estimate(Loop loop, Map<Value,Value> variables) {
        return -1;
    }

    private static IfStmt findLoopExit(Loop loop, Map<Value,Value> variables) {
        for (Stmt stmt : loop.getLoopStatements()) {
            if (stmt instanceof IfStmt) {
                return (IfStmt) stmt;
            } else if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;
                variables.put(assignStmt.getLeftOp(), assignStmt.getRightOp());
            }
        }
        throw new IllegalStateException("No loop exit found");
    }
}
