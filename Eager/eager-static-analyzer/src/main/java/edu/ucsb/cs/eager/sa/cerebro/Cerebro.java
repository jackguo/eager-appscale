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
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.annotation.logic.Loop;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Cerebro {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("ccp", "cerebro-classpath", true, "Cerebro classpath");
        options.addOption("c", "class", true, "Class to be used as the starting point");
        options.addOption("dnc", "disable-necessary-classes", false, "Disable loading of necessary classes");

        CommandLine cmd;
        try {
            CommandLineParser parser = new BasicParser();
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage() + "\n");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Cerebro", options);
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

        boolean disableNecessaryClasses = false;
        if (cmd.hasOption("dnc")) {
            disableNecessaryClasses = true;
        }

        Cerebro cerebro = new Cerebro();
        cerebro.analyze(classPath, startingPoint, !disableNecessaryClasses);
    }

    private Set<SootMethod> analyzedMethods = new HashSet<SootMethod>();

    public void analyze(String classPath, String startingPoint, boolean loadNecessary) {
        XMansion.getInstance().clear();

        soot.options.Options.v().set_allow_phantom_refs(true);
        soot.options.Options.v().set_whole_program(true);
        Scene.v().setSootClassPath(Scene.v().getSootClassPath() + ":" + classPath);
        SootClass clazz = Scene.v().loadClassAndSupport(startingPoint);
        if (loadNecessary) {
            Scene.v().loadNecessaryClasses();
        }
        System.out.println("\n\nStarting the analysis of class: " + clazz.getName() + "\n");
        for (SootMethod method : clazz.getMethods()) {
            if (method.isPublic()) {
                analyzeMethod(method);
            }
        }
    }

    private void analyzeMethod(SootMethod method) {
        if (analyzedMethods.contains(method)) {
            return;
        }
        analyzedMethods.add(method);

        CFGAnalyzer analyzer = XMansion.getInstance().getAnalyzer(method);
        printResult(method, analyzer);

        // Analyzing the previous method will generally cause more methods to be
        // analyzed, whose results will be added to the XMansion. So here we iterate
        // through all the results present in XMansion, and print them out.
        Map<SootMethod,CFGAnalyzer> results = XMansion.getInstance().getResults();
        for (Map.Entry<SootMethod,CFGAnalyzer> entry : results.entrySet()) {
            if (analyzedMethods.contains(entry.getKey())) {
                continue;
            }
            analyzedMethods.add(entry.getKey());
            printResult(entry.getKey(), entry.getValue());
        }
    }

    private void printResult(SootMethod method, CFGAnalyzer analyzer) {
        String msg = "Analyzing: " + method.getDeclaringClass().getName() + "#" +
                method.getName() + "()";
        System.out.println(msg);
        for (int i = 0; i < msg.length(); i++) {
            System.out.print("=");
        }
        System.out.println();

        Collection<Integer> pathApiCalls = analyzer.getPathApiCalls();
        System.out.println("Distinct paths through the code: " + pathApiCalls.size());
        System.out.print("API calls in paths: [");
        for (int count : pathApiCalls) {
            System.out.print(" " + count);
        }
        System.out.println(" ]");

        Map<Loop,Integer> loopedApiCalls = analyzer.getLoopedApiCalls();
        System.out.println("Loops: " + loopedApiCalls.size());
        if (loopedApiCalls.size() > 0) {
            System.out.println("API calls in loops: ");
            for (Map.Entry<Loop,Integer> entry : loopedApiCalls.entrySet()) {
                System.out.println("  " + entry.getKey().getHead() + ": " + entry.getValue());
            }
        }

        Collection<SootMethod> calledUserMethods = analyzer.getUserMethodCalls();
        if (calledUserMethods.size() > 0) {
            System.out.println("Called user-defined methods: ");
            for (SootMethod calledMethod : calledUserMethods) {
                System.out.println("  " + calledMethod.getDeclaringClass().getName() + "#" +
                        calledMethod.getName() + "()");
            }
        }

        System.out.println();
    }

}
