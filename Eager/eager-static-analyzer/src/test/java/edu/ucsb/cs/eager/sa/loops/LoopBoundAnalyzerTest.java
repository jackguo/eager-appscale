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

import edu.ucsb.cs.eager.sa.PerformanceSimulator;
import edu.ucsb.cs.eager.sa.branches.RandomBranchSelector;
import junit.framework.TestCase;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.annotation.logic.Loop;

import java.util.*;

public class LoopBoundAnalyzerTest extends TestCase {

    public void testLoops() {
        PerformanceSimulator simulator = new PerformanceSimulator(new RandomBranchSelector());
        simulator.addUserPackage("net.eager.testing");
        simulator.addSpecialPackage("edu.ucsb.cs.eager.gae");

        SootClass c = Scene.v().loadClassAndSupport("net.eager.testing.TestClass2");
        c.setApplicationClass();
        SootMethod m = c.getMethodByName("main");
        Body b = m.retrieveActiveBody();

        LoopBoundAnalysis analysis = new LoopBoundAnalysis(b, false);
        Map<Loop,Integer> loopBounds = analysis.getLoopBounds();

        List<Integer> results = new ArrayList<Integer>(Arrays.asList(100, 100, 10, 5, -1));
        for (Map.Entry<Loop,Integer> entry : loopBounds.entrySet()) {
            System.out.println(entry.getKey().getHead() + " : " + entry.getValue());
            assertTrue(results.contains(entry.getValue()));
            results.remove(entry.getValue());
        }
        assertTrue(results.isEmpty());
    }
}
