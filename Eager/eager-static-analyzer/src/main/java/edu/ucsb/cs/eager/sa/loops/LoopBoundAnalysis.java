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

import soot.Body;
import soot.IntType;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AddExpr;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class LoopBoundAnalysis extends ForwardFlowAnalysis<Stmt,ProgramState> {

    private Collection<Loop> loops;
    private Body body;
    private Stmt mergeStmt;
    private ProgramState mergeState;
    private Set<Value> variables = new HashSet<Value>();
    private Map<Stmt,Set<Value>> loopInvariants = new HashMap<Stmt, Set<Value>>();

    public LoopBoundAnalysis(Body body) {
        super((DirectedGraph) new BriefUnitGraph(body));
        this.body = body;
    }

    public void analyze() {
        Iterator<Stmt> iterator = graph.iterator();
        while (iterator.hasNext()) {
            Stmt stmt = iterator.next();
            for (ValueBox value : stmt.getDefBoxes()) {
                Value v = value.getValue();
                if (v.getType() instanceof IntType) {
                    variables.add(v);
                }
            }
        }

        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(body);
        loops = loopFinder.loops();

        // invariant analysis
        for (Loop loop : loops) {
            Set<Value> invariants = new HashSet<Value>(variables);
            for (Stmt stmt : loop.getLoopStatements()) {
                for (ValueBox value : stmt.getDefBoxes()) {
                    Value v = value.getValue();
                    if (v.getType() instanceof IntType) {
                        invariants.remove(v);
                    }
                }
            }
            loopInvariants.put(loop.getHead(), invariants);
        }

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
                } else if (rightOp instanceof AddExpr) {
                    Value op1 = ((AddExpr) rightOp).getOp1();
                    Value op2 = ((AddExpr) rightOp).getOp2();
                    if (op1 instanceof JimpleLocal && op2 instanceof IntConstant) {
                        out.updateState(op1, out.get(op1).add(((IntConstant) op2).value));
                    }
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
        ProgramState state = new ProgramState();
        for (Value v : variables) {
            state.updateState(v, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return state;
    }

    @Override
    protected void merge(Stmt next, ProgramState in1, ProgramState in2, ProgramState out) {
        mergeStmt = next;
        mergeState = getFlowAfter(next);
        super.merge(next, in1, in2, out);
        mergeStmt = null;
        mergeState = null;
    }

    @Override
    protected void merge(ProgramState in1, ProgramState in2, ProgramState out) {
        boolean widen = isLoopHead(mergeStmt);
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

        if (widen) {
            Set<Value> invariants = loopInvariants.get(mergeStmt);
            for (Value var : mergeState.getVariables()) {
                if (invariants.contains(var)) {
                    // restricted widening
                    continue;
                }
                IntegerInterval interval1 = mergeState.get(var);
                IntegerInterval interval2 = out.get(var);
                out.updateState(var, interval1.widen(interval2));
            }
        }
    }

    @Override
    protected void copy(ProgramState src, ProgramState dst) {
        src.copy(dst);
    }

    private boolean isLoopHead(Stmt stmt) {
        for (Loop loop : loops) {
            if (loop.getHead().equals(stmt)) {
                return true;
            }
        }
        return false;
    }
}
