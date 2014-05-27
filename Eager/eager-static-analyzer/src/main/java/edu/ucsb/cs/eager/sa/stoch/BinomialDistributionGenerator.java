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

public class BinomialDistributionGenerator extends RandomDistributionGenerator {

    private int n;
    private double p;
    private Random random[];

    public BinomialDistributionGenerator(int n, double p) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be > 0");
        }
        if (p <= 0 || p >= 1) {
            throw new IllegalArgumentException("p must be strictly between 0 and 1");
        }
        this.n = n;
        this.p = p;
        this.random = new Random[n];
        for (int i = 0; i < n; i++) {
            this.random[i] = new Random(getLongSeed());
        }
    }

    @Override
    public double next() {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            if (random[i].nextDouble() <= p) {
                sum += 1;
            }
        }
        return sum;
    }
}
