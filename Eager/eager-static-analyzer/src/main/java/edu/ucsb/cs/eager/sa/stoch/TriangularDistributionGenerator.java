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

package edu.ucsb.cs.eager.sa.stoch;

public class TriangularDistributionGenerator extends RandomDistributionGenerator {

    private double a;
    private double b;

    public TriangularDistributionGenerator(double a, double b) {
        if (a >= b) {
            throw new IllegalArgumentException("a must be < b");
        }
        this.a = a;
        this.b = b;
    }

    @Override
    public double next() {
        double u = rand.nextDouble();
        if (0 <= u && u < 0.5) {
            return (2 * a) + ((b - a) * Math.sqrt(2 * u));
        } else {
            return (2 * b) + ((a - b) * Math.sqrt(2 * (1 - u)));
        }
    }
}
