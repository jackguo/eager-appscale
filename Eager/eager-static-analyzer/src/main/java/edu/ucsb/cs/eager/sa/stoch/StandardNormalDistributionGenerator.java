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

public class StandardNormalDistributionGenerator extends RandomDistributionGenerator {

    private Random rand1;
    private Random rand2;

    public StandardNormalDistributionGenerator() {
        this.rand1 = new Random(getLongSeed());
        this.rand2 = new Random(getLongSeed());
    }

    @Override
    public double next() {
        while (true) {
            double u1 = rand1.nextDouble();
            double u2 = rand2.nextDouble();
            double x = -1 * Math.log(u1);
            double y = (-1 * (x - 1) * (x - 1)) / 2.0;
            if (u2 <= Math.exp(y)) {
                if (rand.nextBoolean()) {
                    return x;
                } else {
                    return -1 * x;
                }
            }
        }
    }
}
