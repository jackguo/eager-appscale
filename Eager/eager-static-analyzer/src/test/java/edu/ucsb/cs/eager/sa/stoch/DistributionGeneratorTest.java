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

import junit.framework.TestCase;

public class DistributionGeneratorTest extends TestCase {

    public void testExponential() {
        System.out.println("Exponential distribution:");
        ExponentialDistributionGenerator gen = new ExponentialDistributionGenerator(0.5);
        for (int i = 0; i < 10; i++) {
            System.out.println(gen.next());
        }
        System.out.println();
    }

    public void testGeometric() {
        System.out.println("Geometric distribution:");
        GeometricDistributionGenerator gen = new GeometricDistributionGenerator(0.5);
        for (int i = 0; i < 10; i++) {
            System.out.println(gen.next());
        }
        System.out.println();
    }

    public void testPoisson() {
        System.out.println("Poisson distribution:");
        PoissonDistributionGenerator gen = new PoissonDistributionGenerator(1);
        for (int i = 0; i < 10; i++) {
            System.out.println(gen.next());
        }
        System.out.println();
    }

    public void testStdNormal() {
        System.out.println("Standard normal distribution:");
        StandardNormalDistributionGenerator gen = new StandardNormalDistributionGenerator();
        for (int i = 0; i < 10; i++) {
            System.out.println(gen.next());
        }
        System.out.println();
    }

    public void testNormal() {
        System.out.println("Normal distribution:");
        NormalDistributionGenerator gen = new NormalDistributionGenerator(5, 2.5);
        for (int i = 0; i < 10; i++) {
            System.out.println(gen.next());
        }
        System.out.println();
    }
}
