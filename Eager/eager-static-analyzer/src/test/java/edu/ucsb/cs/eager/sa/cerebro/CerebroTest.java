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

import junit.framework.TestCase;
import soot.SootMethod;

import java.util.Map;

public class CerebroTest extends TestCase {

    public void testCerebro() {
        Cerebro cerebro = new Cerebro("", "net.eager.testing.TestClass3");
        cerebro.setWholeProgramMode(true);
        cerebro.setLoadNecessaryClasses(false);

        Map<SootMethod,CFGAnalyzer> results = cerebro.analyze();
        assertEquals(5, results.size());
        boolean mainFound = false;
        for (Map.Entry<SootMethod,CFGAnalyzer> entry : results.entrySet()) {
            if (entry.getKey().getName().equals("main")) {
                assertEquals(entry.getValue().getMaxApiCalls(), 4);
                mainFound = true;
            }
        }
        assertTrue(mainFound);
    }
}
