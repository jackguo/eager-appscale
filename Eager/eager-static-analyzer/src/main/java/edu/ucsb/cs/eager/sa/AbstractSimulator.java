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
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSimulator implements InstructionSimulator {

    private Map<SootMethod,SimulationManager> cache = new HashMap<SootMethod,SimulationManager>();

    private Set<String> userPackages = new HashSet<String>();
    private Set<String> specialPackages = new HashSet<String>();

    public void addUserPackage(String pkg) {
        userPackages.add(pkg);
    }

    public void addSpecialPackage(String pkg) {
        specialPackages.add(pkg);
    }

    private SimulationManager getSimulationManager(SootMethod method) {
        SimulationManager manager = cache.get(method);
        if (manager == null) {
            UnitGraph subGraph = new BriefUnitGraph(method.retrieveActiveBody());
            manager = new SimulationManager(subGraph, this);
            cache.put(method, manager);
        }
        return manager;
    }

    public double simulateInstruction(Stmt stmt) {
        if (stmt.containsInvokeExpr()) {
            InvokeExpr invocation = stmt.getInvokeExpr();
            SootMethod method = invocation.getMethod();
            String pkg = method.getDeclaringClass().getPackageName();
            if (userPackages.contains(pkg)) {
                // If it's a user-defined function, run the simulation recursively
                SimulationManager manager = getSimulationManager(method);
                return manager.simulate(1, false);
            } else if (specialPackages.contains(pkg)) {
                return simulateSpecialInvokeInstruction(invocation);
            }
            return simulateRegularInvokeInstruction(invocation);
        }
        return simulateNonInvokeInstruction(stmt);
    }

    protected abstract double simulateSpecialInvokeInstruction(InvokeExpr invocation);

    protected abstract double simulateRegularInvokeInstruction(InvokeExpr invocation);

    protected abstract double simulateNonInvokeInstruction(Stmt stmt);
}
