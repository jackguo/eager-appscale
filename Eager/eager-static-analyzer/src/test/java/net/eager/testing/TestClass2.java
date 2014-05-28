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

package net.eager.testing;

public class TestClass2 {

    public static void main(String[] args) {
        int i = 1;
        while (i <= 100) {
            int j = 1;
            while (j <= i) {
                j++;
            }
            i += 2;
        }

        for (int m = 0; m < 10; m++) {
            for (int n = 0; n < 5; n++) {
                System.out.println("ok");
            }
        }

        for (String arg : args) {
            System.out.println();
        }
    }
}
