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

import soot.IntType;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.Iterator;

public class LoopBoundAnalysis extends ForwardFlowAnalysis<Stmt,ProgramState> {

    public LoopBoundAnalysis(DirectedGraph<Stmt> graph) {
        super(graph);
    }

    public void analyze() {
        doAnalysis();
    }

    @Override
    protected void flowThrough(ProgramState in, Stmt stmt, ProgramState out) {
        if (in != null) {
            in.copy(out);
        }
        if (stmt instanceof AssignStmt) {
            Value rightOp = ((AssignStmt) stmt).getRightOp();
            if (rightOp.getType() instanceof IntType) {
                Value leftOp = ((AssignStmt) stmt).getLeftOp();
                if (rightOp instanceof IntConstant) {
                    int value = ((IntConstant) rightOp).value;
                    out.updateState(leftOp, value, value);
                }
            }
        }
    }

    @Override
    protected ProgramState newInitialFlow() {
        return new ProgramState();
    }

    @Override
    protected ProgramState entryInitialFlow() {
        Iterator<Stmt> iterator = graph.iterator();
        ProgramState state = new ProgramState();
        while (iterator.hasNext()) {
            Stmt stmt = iterator.next();
            for (ValueBox value : stmt.getDefBoxes()) {
                Value v = value.getValue();
                if (v.getType() instanceof IntType) {
                    state.updateState(v, Integer.MIN_VALUE, Integer.MAX_VALUE);
                }
            }
        }
        return state;
    }

    @Override
    protected void merge(ProgramState in1, ProgramState in2, ProgramState out) {
        for (Value var : in1.getVariables()) {
            IntegerInterval interval1 = in1.get(var);
            IntegerInterval interval2 = in2.get(var);
            if (interval2 != null) {
                out.updateState(var, interval1.sup(interval2));
            } else {
                out.updateState(var, interval1);
            }
        }

        for (Value var : in2.getVariables()) {
            if (in1.get(var) == null) {
                out.updateState(var, in2.get(var));
            }
        }
    }

    @Override
    protected void copy(ProgramState src, ProgramState dst) {
        src.copy(dst);
    }
}
