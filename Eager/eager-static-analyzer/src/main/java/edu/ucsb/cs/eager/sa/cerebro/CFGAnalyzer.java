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

package edu.ucsb.cs.eager.sa.cerebro;

import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.UnitGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CFGAnalyzer {

    private Set<Stmt> visited = new HashSet<Stmt>();

    public void analyze(UnitGraph graph) {
        visited.clear();
        Stmt stmt = (Stmt) graph.getHeads().get(0);
        visit(stmt, graph);
    }

    public void visit(Stmt stmt, UnitGraph graph) {
        if (visited.contains(stmt)) {
            return;
        }
        visited.add(stmt);
        // Todo: process stmt
        System.out.println(stmt);

        List<Unit> children = graph.getSuccsOf(stmt);
        for (Unit child : children) {
            visit((Stmt) child, graph);
        }
    }
}
