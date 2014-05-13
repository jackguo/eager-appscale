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

package edu.ucsb.cs.soot.graph;

import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.UnitGraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GraphBuilder {

    public PerformanceGraph build(UnitGraph graph) {
        GraphBuildContext context = new GraphBuildContext(graph);
        visit((Stmt) graph.getHeads().get(0), null, context);
        return context.result;
    }

    private void visit(Stmt stmt, PerformanceInfo currentHead, GraphBuildContext context) {
        if (context.visited.contains(stmt)) {
            PerformanceInfo vertex = context.result.findVertex(stmt);
            context.result.createEdge(currentHead, vertex);
            return;
        }
        context.visited.add(stmt);

        PerformanceInfo vertex = createVertex(stmt, context);
        context.result.addVertex(vertex);
        if (currentHead != null) {
            context.result.createEdge(currentHead, vertex);
        }
        currentHead = vertex;

        for (Unit child : context.graph.getSuccsOf(stmt)) {
            visit((Stmt) child, currentHead, context);
        }
    }

    private PerformanceInfo createVertex(Stmt stmt,GraphBuildContext context) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invocation = stmt.getInvokeExpr();
            if (invocation.getMethod().getDeclaringClass().getName().equals("DataStore")) {
                return new SpecialAPICallInstruction(stmt);
            } else {
                // TODO: Handle regular function call
            }
        }

        Loop loop = findLoop(stmt, context);
        if (loop != null) {
            // TODO: Handle loop
        }

        return new LocalInstruction(stmt);
    }

    private Loop findLoop(Stmt stmt, GraphBuildContext context) {
        if (!stmt.branches()) {
            return null;
        }
        for (Loop loop : context.loops) {
            if (loop.getHead().equals(stmt)) {
                return loop;
            }
        }
        return null;
    }

    private static class GraphBuildContext {
        PerformanceGraph result;
        Set<Stmt> visited;
        UnitGraph graph;
        Collection<Loop> loops;

        GraphBuildContext(UnitGraph graph) {
            this.graph = graph;
            this.result = new PerformanceGraph();
            this.visited = new HashSet<Stmt>();
            LoopFinder loopFinder = new LoopFinder();
            loopFinder.transform(graph.getBody());
            this.loops = loopFinder.loops();
        }
    }
}
