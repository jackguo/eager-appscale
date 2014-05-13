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

import soot.jimple.Stmt;

import java.util.LinkedHashSet;
import java.util.Set;

public class PerformanceGraph {

    private Set<PerformanceInfo> vertices = new LinkedHashSet<PerformanceInfo>();
    private Set<Edge> edges = new LinkedHashSet<Edge>();

    public void addVertex(PerformanceInfo vertex) {
        vertices.add(vertex);
    }

    public void createEdge(PerformanceInfo source, PerformanceInfo target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target must not be null");
        }
        Edge edge = new Edge();
        edge.source = source;
        edge.target = target;
        edges.add(edge);
    }

    public PerformanceInfo findVertex(Stmt stmt) {
        for (PerformanceInfo vertex : vertices) {
            if (vertex.stmt.equals(stmt)) {
                return vertex;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Edge edge : edges) {
            builder.append(edge.source).append("  -->  ").append(
                    edge.target.toStringTarget()).append('\n');
        }
        if (edges.isEmpty()) {
            for (PerformanceInfo vertex : vertices) {
                builder.append(vertex).append('\n');
            }
        }
        return builder.toString();
    }

    static class PerformanceInfo {
        Stmt stmt;
        boolean loop = false;
        PerformanceGraph loopContent;

        PerformanceInfo(Stmt stmt) {
            this.stmt = stmt;
        }

        @Override
        public String toString() {
            if (loop) {
                return stmt.toString() + " [loop]";
            }
            return stmt.toString();
        }

        public String toStringTarget() {
            if (loop) {
                StringBuilder builder = new StringBuilder();
                builder.append(stmt.toString()).append(" [loop]\n");
                builder.append(">>>\n");
                builder.append(loopContent);
                builder.append("<<<");
                return builder.toString();
            }
            return stmt.toString();
        }
    }

    private static class Edge {
        private PerformanceInfo source;
        private PerformanceInfo target;
    }
}
