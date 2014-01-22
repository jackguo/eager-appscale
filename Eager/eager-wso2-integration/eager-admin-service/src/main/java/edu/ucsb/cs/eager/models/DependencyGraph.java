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

package edu.ucsb.cs.eager.models;

import edu.ucsb.cs.eager.dao.EagerDependencyMgtDAO;

import java.util.HashSet;
import java.util.Set;

public class DependencyGraph {

    private Set<Vertex> vertices = new HashSet<Vertex>();
    private Set<Edge> edges = new HashSet<Edge>();

    private static final EagerDependencyMgtDAO dao = new EagerDependencyMgtDAO();
    private String rootName;
    private String rootVersion;

    private static final int WHITE = 1;
    private static final int GREY = 2;
    private static final int BLACK = 3;

    protected DependencyGraph() {

    }

    public DependencyGraph(String rootName, String rootVersion,
                           DependencyInfo[] dependencies) throws EagerException {
        this.rootName = rootName;
        this.rootVersion = rootVersion;
        buildRecursively(rootName, rootVersion, dependencies, true);
    }

    private void buildRecursively(String rootName, String rootVersion,
                                  DependencyInfo[] dependencies, boolean startPoint) throws EagerException {
        if (!startPoint) {
            if (this.rootName.equals(rootName) && this.rootVersion.equals(rootVersion)) {
                return;
            }
            dependencies = getDependencies(rootName, rootVersion);
        }
        for (DependencyInfo dependency : dependencies) {
            createEdge(rootName, rootVersion, dependency.getName(), dependency.getVersion());
            buildRecursively(dependency.getName(), dependency.getVersion(), null, false);
        }
    }

    private DependencyInfo[] getDependencies(String name, String version) throws EagerException {
        return dao.getDependencies(name, version);
    }

    public void createEdge(String dependentName, String dependentVersion,
                            String dependencyName, String dependencyVersion) {

        Vertex dependent = getOrCreate(dependentName, dependentVersion);
        Vertex dependency = getOrCreate(dependencyName, dependencyVersion);
        edges.add(new Edge(dependent, dependency));
    }

    protected int getVertices() {
        return vertices.size();
    }

    protected int getEdges() {
        return edges.size();
    }

    private Vertex getOrCreate(String name, String version) {
        for (Vertex v : vertices) {
            if (v.name.equals(name) && v.version.equals(version)) {
                return v;
            }
        }
        Vertex v = new Vertex(name, version);
        vertices.add(v);
        return v;
    }

    public boolean hasCycle() {
        // Based on http://www.cs.berkeley.edu/~kamil/teaching/sp03/041403.pdf
        for (Vertex v : vertices) {
            if (v.color == WHITE) {
                if (visit(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean visit(Vertex v) {
        v.color = GREY;
        for (Edge e : edges) {
            if (e.dependent.equals(v)) {
                if (e.dependency.color == GREY) {
                    return true;
                } else if (e.dependency.color == WHITE) {
                    if (visit(e.dependency)) {
                        return true;
                    }
                }
            }
        }
        v.color = BLACK;
        return false;
    }

    private static class Vertex {
        private String name;
        private String version;
        private int color;

        public Vertex(String name, String version) {
            this.name = name;
            this.version = version;
            this.color = WHITE;
        }

        @Override
        public int hashCode() {
            return (name + version).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Vertex) {
                Vertex other = (Vertex) obj;
                return name.equals(other.name) && version.equals(other.version);
            }
            return false;
        }
    }

    private static class Edge {
        private Vertex dependent;
        private Vertex dependency;

        private Edge(Vertex dependent, Vertex dependency) {
            this.dependent = dependent;
            this.dependency = dependency;
        }

        @Override
        public int hashCode() {
            return dependent.hashCode() ^ dependency.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Edge) {
                Edge other = (Edge) obj;
                return dependent.equals(other.dependent) && dependency.equals(other.dependency);
            }
            return false;
        }
    }
}
