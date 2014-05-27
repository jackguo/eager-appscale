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

public class BernoulliDistributionGenerator extends RandomDistributionGenerator {

    private double p;

    public BernoulliDistributionGenerator(double p) {
        if (p <= 0 || p >= 1) {
            throw new IllegalArgumentException("p must be strictly between 0 and 1");
        }
        this.p = p;
    }

    @Override
    public double next() {
        double u = rand.nextDouble();
        if (u <= p) {
            return 1.0;
        } else {
            return 0.0;
        }
    }
}
