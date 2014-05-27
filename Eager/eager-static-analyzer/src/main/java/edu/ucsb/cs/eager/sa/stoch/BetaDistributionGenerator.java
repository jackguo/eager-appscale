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

import java.util.Random;

public class BetaDistributionGenerator extends RandomDistributionGenerator {

    private double a;
    private double b;
    private Random rand1;
    private Random rand2;

    public BetaDistributionGenerator(double a, double b) {
        if (a <= 0 || b <= 0) {
            throw new IllegalArgumentException("a and b must be > 0");
        }
        this.a = a;
        this.b = b;
        this.rand1 = new Random(getLongSeed());
        this.rand2 = new Random(getLongSeed());
    }

    @Override
    public double next() {
        while (true) {
            double u1 = rand1.nextDouble();
            double u2 = rand2.nextDouble();
            double y1 = Math.pow(u1, (1.0 / a));
            double y2 = Math.pow(u2, (1.0 / b));
            if (y1 + y2 <= 1) {
                return y1 / (y1 + y2);
            }
        }
    }
}
