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

package edu.ucsb.cs.soot.graph;

import soot.jimple.Stmt;

import java.util.LinkedHashSet;
import java.util.Set;

public class PerformanceGraph {

    private Set<PerformanceInfo> vertices = new LinkedHashSet<PerformanceInfo>();
    private Set<Edge> edges = new LinkedHashSet<Edge>();

    public void addVertex(PerformanceInfo pi) {
        vertices.add(pi);
    }

    public void createEdge(PerformanceInfo src, PerformanceInfo tgt) {
        addVertex(src);
        addVertex(tgt);
        Edge edge = new Edge();
        edge.source = src;
        edge.target = tgt;
        edges.add(edge);
    }

    public PerformanceInfo findVertex(Stmt stmt) {
        for (PerformanceInfo vertex : vertices) {
            if (vertex.getStatement().equals(stmt)) {
                return vertex;
            }
        }
        return null;
    }

    public void print() {
        for (Edge edge : edges) {
            System.out.println(edge.source + "  -->  " + edge.target);
        }
    }

    private static class Edge {
        PerformanceInfo source;
        PerformanceInfo target;
    }

}
