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

package edu.ucsb.cs.soot;

import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.UnitGraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PerformanceGraphBuilder {

    private Set<Stmt> visited;
    private PerformanceGraph result;
    private Collection<Loop> loops;
    private Loop currentLoop;

    public PerformanceGraph build(UnitGraph graph) {
        return build(graph, (Stmt) graph.getHeads().get(0));
    }

    public PerformanceGraph build(UnitGraph graph, Stmt head) {
        visited = new HashSet<Stmt>();
        result = new PerformanceGraph();

        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(graph.getBody());
        loops = loopFinder.loops();

        visit(head, graph, null);
        return result;
    }

    private PerformanceGraph buildLoop(UnitGraph graph, Loop loop, Collection<Loop> loops) {
        visited = new HashSet<Stmt>();
        result = new PerformanceGraph();
        this.loops = loops;
        this.currentLoop = loop;

        visit(loop.getLoopStatements().get(1), graph, null);
        return result;
    }

    private void visit(Stmt stmt, UnitGraph graph, PerformanceGraph.PerformanceInfo currentHead) {
        if (currentLoop != null && stmt.equals(currentLoop.getHead())) {
            return;
        }
        if (visited.contains(stmt)) {
            if (currentHead.loop) {
                return;
            }
            PerformanceGraph.PerformanceInfo vertex = result.findVertex(stmt);
            result.createEdge(currentHead, vertex);
            return;
        }
        visited.add(stmt);

        if (currentHead == null || isInteresting(currentHead.stmt) || isInteresting(stmt)) {
            PerformanceGraph.PerformanceInfo vertex = new PerformanceGraph.PerformanceInfo(stmt);
            result.addVertex(vertex);
            if (currentHead != null) {
                result.createEdge(currentHead, vertex);
            }

            Loop loop = findLoop(stmt);
            if (loop != null) {
                vertex.loop = true;
                for (Stmt loopStmt : loop.getLoopStatements()) {
                    visited.add(loopStmt);
                }

                PerformanceGraphBuilder tempBuilder = new PerformanceGraphBuilder();
                vertex.loopContent = tempBuilder.buildLoop(graph, loop, loops);
            }

            currentHead = vertex;
        }

        for (Unit child : graph.getSuccsOf(stmt)) {
            visit((Stmt) child, graph, currentHead);
        }
    }

    private boolean isInteresting(Stmt stmt) {
        if (stmt.branches() && stmt.fallsThrough()) {
            return true;
        } else if (stmt.containsInvokeExpr()) {
            InvokeExpr invoke = stmt.getInvokeExpr();
            if (invoke.getMethod().getDeclaringClass().getName().equals("DataStore")) {
                return true;
            }
        }
        return false;
    }

    private Loop findLoop(Stmt stmt) {
        if (!stmt.branches()) {
            return null;
        }
        for (Loop loop : loops) {
            if (loop.getHead().equals(stmt)) {
                return loop;
            }
        }
        return null;
    }
}
