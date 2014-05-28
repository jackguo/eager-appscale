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

public class ErlangDistributionGenerator extends RandomDistributionGenerator {

    /** Scale parameter */
    private double lambda;
    /** Shape parameter */
    private int k;
    private Random random[];

    public ErlangDistributionGenerator(double lambda, int k) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("lambda must be > 0");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be > 0");
        }
        this.lambda = lambda;
        this.k = k;
        this.random = new Random[k];
        for (int i = 0; i < k; i++) {
            this.random[i] = new Random(getLongSeed());
        }
    }

    @Override
    public double next() {
        double product = 1;
        for (int i = 0; i < k; i++) {
            product *= random[i].nextDouble();
        }
        return -1 * (1.0 / lambda) * Math.log(product);
    }
}
