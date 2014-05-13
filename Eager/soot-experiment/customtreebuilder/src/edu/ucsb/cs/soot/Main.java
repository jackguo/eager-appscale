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

import edu.ucsb.cs.soot.graph.GraphBuilder;
import edu.ucsb.cs.soot.simulator.PerformanceSimulator;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        SootClass c = Scene.v().loadClassAndSupport("MyClass");
        c.setApplicationClass();
        SootMethod m = c.getMethodByName("main");
        Body b = m.retrieveActiveBody();
        UnitGraph g = new BriefUnitGraph(b);

        PerformanceSimulator simulator = new PerformanceSimulator(g);
        System.out.println("Performance: " + simulator.simulate(1000, true));

        //buildGraph(g);

        //GraphBuilder builder = new GraphBuilder();
        //builder.build(g).print();

        /*AnalysisWrapper wrapper = new AnalysisWrapper(g);
        Iterator<Unit> iterator = g.iterator();
        while (iterator.hasNext()) {
            Stmt s = (Stmt) iterator.next();
            System.out.println(s + " [" + wrapper.getCostAfter(s) + "]");
        }*/

        /*System.out.println("\n=======\n");
        Iterator<Unit> iterator = g.iterator();
        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();
            System.out.println(stmt.hashCode() + ": " + stmt);
            List<Unit> successors = g.getSuccsOf(stmt);
            for (Unit s : successors) {
                System.out.println(stmt.hashCode() + " --> " + s.hashCode());
            }
        }*/

        //new SimpleExpressionAnalysis(g);
    }

    private static Set<Stmt> visited = new HashSet<Stmt>();

    private static void buildGraph(UnitGraph g) {
        List<Unit> headers = g.getHeads();
        for (Unit head : headers) {
            visit(g, (Stmt) head, "START");
        }
    }

    private static void visit(UnitGraph g, Stmt stmt, String currentHead) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr inv = stmt.getInvokeExpr();
            String clazzName = inv.getMethod().getDeclaringClass().getName();
            if (clazzName.equals("DataStore")) {
                System.out.println(currentHead + "  -->  " + stmt);
                currentHead = stmt.toString();
            }
        }

        if (visited.contains(stmt)) {
            return;
        } else {
            visited.add(stmt);
        }

        List<Unit> children = g.getSuccsOf(stmt);
        if (children.isEmpty()) {
            System.out.println(currentHead + "  -->  END");
        } else {
            for (Unit child : children) {
                visit(g, (Stmt) child, currentHead);
            }
        }
    }
}
