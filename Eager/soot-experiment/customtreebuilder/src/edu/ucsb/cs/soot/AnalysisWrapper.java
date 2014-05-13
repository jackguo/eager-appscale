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
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.UnitGraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AnalysisWrapper {

    private Map<Stmt, Integer> costBefore = new HashMap<Stmt, Integer>();
    private Map<Stmt, Integer> costAfter = new HashMap<Stmt, Integer>();

    public AnalysisWrapper(UnitGraph graph) {
        SimpleExpressionAnalysis analysis = new SimpleExpressionAnalysis(graph);
        analysis.analyze();
        Iterator<Unit> unitIt = graph.iterator();
        while (unitIt.hasNext()) {
            Stmt s = (Stmt) unitIt.next();
            costBefore.put(s, analysis.getFlowBefore(s).value);
            costAfter.put(s, analysis.getFlowAfter(s).value);
        }
    }

    public int getCostAfter(Stmt s) {
        return costAfter.get(s);
    }

    public int getCostBefore(Stmt s) {
        return costBefore.get(s);
    }
}
