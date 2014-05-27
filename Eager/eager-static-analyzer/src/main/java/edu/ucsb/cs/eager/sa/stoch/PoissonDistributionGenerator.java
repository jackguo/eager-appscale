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

public class PoissonDistributionGenerator extends RandomDistributionGenerator {

    private double lambda;

    public PoissonDistributionGenerator(double lambda) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("lambda must be > 0");
        }
        this.lambda = lambda;
    }

    @Override
    public double next() {
        double u = rand.nextDouble();
        double p0 = Math.exp(-1 * lambda);
        if (u <= p0) {
            return 0.0;
        }

        double sum = p0;
        double prev = p0;
        double n = 1.0;
        while (true) {
            double pn = (lambda / n) * prev;
            sum += pn;
            if (u <= sum) {
                return n;
            }
            prev = pn;
            n += 1.0;
        }
    }
}
