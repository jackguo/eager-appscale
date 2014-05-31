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

import org.apache.commons.cli.*;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class Cerebro {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("ccp", true, "Cerebro classpath");
        options.addOption("c", true, "Class to be used as the starting point");

        CommandLine cmd;
        try {
            CommandLineParser parser = new BasicParser();
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            System.err.println("Error while parsing command line arguments");
            e.printStackTrace();
            return;
        }

        String classPath = cmd.getOptionValue("ccp");
        if (classPath == null) {
            System.err.println("Cerebro classpath (ccp) argument is required");
            return;
        }

        String startingPoint = cmd.getOptionValue("c");
        if (startingPoint == null) {
            System.err.println("Starting point class (c) argument is required");
        }

        Cerebro cerebro = new Cerebro();
        cerebro.analyze(classPath, startingPoint);
    }

    public void analyze(String classPath, String startingPoint) {
        soot.options.Options.v().set_allow_phantom_refs(true);
        Scene.v().setSootClassPath(Scene.v().getSootClassPath() + ":" + classPath);

        SootClass clazz = Scene.v().loadClassAndSupport(startingPoint);
        Scene.v().loadNecessaryClasses();
        System.out.println("\n\nStarting the analysis of class: " + clazz.getName() + "\n");
        for (SootMethod method : clazz.getMethods()) {
            analyzeMethod(method);
        }
    }

    private static void analyzeMethod(SootMethod method) {
        System.out.println("Analyzing method: " + method.getName());
        System.out.println("======================================");
        Body b = method.retrieveActiveBody();
        UnitGraph g = new BriefUnitGraph(b);
        CFGAnalyzer analyzer = new CFGAnalyzer();
        analyzer.analyze(g);
        System.out.println();
    }

}
