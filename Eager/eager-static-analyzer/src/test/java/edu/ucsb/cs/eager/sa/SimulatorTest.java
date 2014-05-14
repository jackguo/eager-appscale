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

import junit.framework.TestCase;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class SimulatorTest extends TestCase {

    public void testPerformanceSimulator() {
        PerformanceSimulator simulator = new PerformanceSimulator();
        simulator.addUserPackage("net.eager.testing");

        SootClass c = Scene.v().loadClassAndSupport("net.eager.testing.TestClass");
        c.setApplicationClass();
        SootMethod m = c.getMethodByName("main");
        Body b = m.retrieveActiveBody();
        UnitGraph g = new BriefUnitGraph(b);
        SimulationManager manager = new SimulationManager(g, simulator);
        double result = manager.simulate(10, true);
        System.out.println("Average performance = " + result);
    }

    public void testAvailabilitySimulator() {
        AvailabilitySimulator simulator = new AvailabilitySimulator();
        simulator.addUserPackage("net.eager.testing");

        SootClass c = Scene.v().loadClassAndSupport("net.eager.testing.TestClass");
        c.setApplicationClass();
        SootMethod m = c.getMethodByName("main");
        Body b = m.retrieveActiveBody();
        UnitGraph g = new BriefUnitGraph(b);
        SimulationManager manager = new SimulationManager(g, simulator);
        double result = manager.simulate(10, true);
        System.out.println("Minimum availability = " + result + "%");
    }
}
