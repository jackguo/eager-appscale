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

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JCaughtExceptionRef;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class CFGAnalyzer {

    private Collection<Loop> loops;
    private Map<Loop,Integer> loopedApiCalls = new HashMap<Loop, Integer>();
    private List<Integer> pathApiCalls = new ArrayList<Integer>();
    private Set<SootMethod> userMethodCalls = new LinkedHashSet<SootMethod>();

    private final UnitGraph graph;
    private final SootMethod method;

    private static final String[] GAE_PACKAGES = new String[] {
        "javax.persistence",
        "edu.ucsb.cs.eager.gae",
    };

    public CFGAnalyzer(SootMethod method) {
        this.method = method;
        Body b = method.retrieveActiveBody();
        this.graph = new BriefUnitGraph(b);
    }

    public void analyze() {
        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(graph.getBody());
        loops = loopFinder.loops();

        Stmt stmt = (Stmt) graph.getHeads().get(0);
        visit(stmt, graph, 0);
    }

    public Map<Loop, Integer> getLoopedApiCalls() {
        return Collections.unmodifiableMap(loopedApiCalls);
    }

    public Collection<Integer> getPathApiCalls() {
        return Collections.unmodifiableList(pathApiCalls);
    }

    public Collection<SootMethod> getUserMethodCalls() {
        return Collections.unmodifiableSet(userMethodCalls);
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
                } else if (isUserMethodCall(invocation.getMethod())) {
                    userMethodCalls.add(invocation.getMethod());
                }
            }
        }
        loopedApiCalls.put(loop, apiCallCount);
    }

    public void visit(Stmt stmt, UnitGraph graph, int apiCallCount) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invocation = stmt.getInvokeExpr();
            if (isApiCall(invocation)) {
                apiCallCount++;
            } else if (isUserMethodCall(invocation.getMethod())) {
                userMethodCalls.add(invocation.getMethod());
            }
        }

        Collection<Unit> children = graph.getSuccsOf(stmt);

        Loop loop = findLoop(stmt);
        if (loop != null) {
            analyzeLoop(loop, 1);
            children = new HashSet<Unit>();
            for (Stmt exit : loop.getLoopExits()) {
                for (Stmt exitTarget : loop.targetsOfLoopExit(exit)) {
                    if (exitTarget instanceof JIdentityStmt) {
                        if (((JIdentityStmt) exitTarget).getRightOp() instanceof JCaughtExceptionRef) {
                            continue;
                        }
                    }
                    children.add(exitTarget);
                }
            }
        }

        for (Unit child : children) {
            visit((Stmt) child, graph, apiCallCount);
        }
        if (children.isEmpty()) {
            pathApiCalls.add(apiCallCount);
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

    private boolean isUserMethodCall(SootMethod target) {
        String userPackage = method.getDeclaringClass().getJavaPackageName();
        String targetPackage = target.getDeclaringClass().getJavaPackageName();
        return targetPackage.startsWith(userPackage);
    }

    private Loop findLoop(Stmt stmt) {
        for (Loop loop : loops) {
            if (loop.getHead().equals(stmt)) {
                return loop;
            }
        }
        return null;
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
