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
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class CFGAnalyzer {

    private Collection<Loop> loops;
    private Map<Loop,Integer> loopedApiCalls = new HashMap<Loop, Integer>();

    private static final String[] GAE_PACKAGES = new String[] {
        "javax.persistence",
        "edu.ucsb.cs.eager.gae",
    };

    public void analyze(UnitGraph graph) {
        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(graph.getBody());
        loops = loopFinder.loops();

        Stmt stmt = (Stmt) graph.getHeads().get(0);
        visit(stmt, graph, 0);
    }

    private void analyzeLoop(Loop loop, int nestingLevel) {
        if (loopedApiCalls.containsKey(loop)) {
            return;
        }

        Set<Loop> nestedLoops = new HashSet<Loop>();
        for (Stmt stmt : loop.getLoopStatements()) {
            Loop nestedLoop = findLoop(stmt);
            if (nestedLoop != null && !nestedLoop.equals(loop)) {
                nestedLoops.add(nestedLoop);
            }
        }

        int apiCallCount = 0;
        for (Stmt stmt : loop.getLoopStatements()) {
            Loop nestedLoop = findLoop(stmt);
            if (nestedLoop != null && !nestedLoop.equals(loop)) {
                analyzeLoop(nestedLoop, nestingLevel + 1);
            }

            if (isStmtInNestedLoopBody(stmt, nestedLoops)) {
                continue;
            }

            if (stmt.containsInvokeExpr()) {
                InvokeExpr invocation = stmt.getInvokeExpr();
                if (isApiCall(invocation)) {
                    apiCallCount++;
                }
            }
        }
        loopedApiCalls.put(loop, apiCallCount);
        System.out.println("API calls in loop [ " + loop.getHead() + "]: " + apiCallCount +
                " [Nesting level = " + nestingLevel + "]");
    }

    public void visit(Stmt stmt, UnitGraph graph, int apiCallCount) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invocation = stmt.getInvokeExpr();
            if (isApiCall(invocation)) {
                apiCallCount++;
            }
        }

        List<Unit> children = graph.getSuccsOf(stmt);

        Loop loop = findLoop(stmt);
        if (loop != null) {
            analyzeLoop(loop, 1);
            IfStmt head = getLoopHeadBranchStmt(loop);
            Stmt target = head.getTarget();
            children = new ArrayList<Unit>();
            children.add(target);
        }

        for (Unit child : children) {
            visit((Stmt) child, graph, apiCallCount);
        }
        if (children.isEmpty()) {
            System.out.println("API calls in path: " + apiCallCount);
        }
    }

    private boolean isApiCall(InvokeExpr invocation) {
        String pkg = invocation.getMethod().getDeclaringClass().getPackageName();
        for (String gaePackage : GAE_PACKAGES) {
            if (gaePackage.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    private Loop findLoop(Stmt stmt) {
        for (Loop loop : loops) {
            if (loop.getHead().equals(stmt)) {
                return loop;
            }
        }
        return null;
    }

    private IfStmt getLoopHeadBranchStmt(Loop loop) {
        for (Stmt stmt : loop.getLoopStatements()) {
            if (stmt instanceof IfStmt) {
                return (IfStmt) stmt;
            }
        }
        throw new IllegalStateException("Failed to find loop head branch");
    }

    private boolean isStmtInNestedLoopBody(Stmt stmt, Set<Loop> nestedLoops) {
        for (Loop nestedLoop : nestedLoops) {
            if (nestedLoop.getLoopStatements().contains(stmt)) {
                return true;
            }
        }
        return false;
    }
}
