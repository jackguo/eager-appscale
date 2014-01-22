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

import junit.framework.TestCase;

public class DependencyGraphTest extends TestCase {

    public void testGraph1() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        graph.createEdge("A", "1.0", "B", "1.0");
        assertEquals(2, graph.getVertices());
        assertEquals(1, graph.getEdges());
        assertFalse(graph.hasCycle());
    }

    public void testGraph2() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        graph.createEdge("A", "1.0", "B", "1.0");
        graph.createEdge("B", "1.0", "C", "1.0");
        assertEquals(3, graph.getVertices());
        assertEquals(2, graph.getEdges());
        assertFalse(graph.hasCycle());
    }

    public void testGraph3() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        graph.createEdge("A", "1.0", "B", "1.0");
        graph.createEdge("A", "1.0", "B", "1.0");
        assertEquals(2, graph.getVertices());
        assertEquals(1, graph.getEdges());
        assertFalse(graph.hasCycle());
    }

    public void testGraph4() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        graph.createEdge("A", "1.0", "B", "1.0");
        graph.createEdge("B", "1.0", "A", "1.0");
        assertEquals(2, graph.getVertices());
        assertEquals(2, graph.getEdges());
        assertTrue(graph.hasCycle());
    }

    public void testGraph5() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        graph.createEdge("A", "1.0", "A", "1.0");
        assertEquals(1, graph.getVertices());
        assertEquals(1, graph.getEdges());
        assertTrue(graph.hasCycle());
    }

    public void testGraph6() throws Exception {
        DependencyGraph graph = new DependencyGraph();
        graph.createEdge("A", "1.0", "B", "1.0");
        graph.createEdge("C", "1.0", "B", "1.0");
        assertEquals(3, graph.getVertices());
        assertEquals(2, graph.getEdges());
        assertFalse(graph.hasCycle());
    }
}
