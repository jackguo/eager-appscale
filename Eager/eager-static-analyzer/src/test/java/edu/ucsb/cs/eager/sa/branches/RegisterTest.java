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

package edu.ucsb.cs.eager.sa.branches;

import edu.ucsb.cs.eager.sa.branches.tlat.Register;
import junit.framework.TestCase;

public class RegisterTest extends TestCase {

    public void testRegister() {
        Register register = new Register(16);
        assertEquals(0, register.toInt());

        register.pushAndShiftLeft(true);
        assertEquals(1, register.toInt());

        register.pushAndShiftLeft(true);
        assertEquals(3, register.toInt());

        register.pushAndShiftLeft(false);
        assertEquals(6, register.toInt());

        for (int i = 0; i < 16; i++) {
            register.pushAndShiftLeft(false);
        }
        assertEquals(0, register.toInt());

        for (int i = 0; i < 16; i++) {
            register.pushAndShiftLeft(true);
        }
        assertEquals(65535, register.toInt());
    }
}
