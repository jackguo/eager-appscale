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

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Most concrete implementations of this abstract class are based on the algorithms
 * described in chapter 5 of "Computer Performance Modeling Handbook" by Stephen
 * Lavenberg.
 */
public abstract class RandomDistributionGenerator {

    protected Random rand = new Random();

    public abstract double next();

    /**
     * Code borrowed from http://www.javamex.com/tutorials/random_numbers/seeding_entropy.shtml
     * with thanks.
     *
     * @return a random seed
     */
    public long getLongSeed() {
        SecureRandom sec = new SecureRandom();
        byte[] sbuf = sec.generateSeed(8);
        ByteBuffer bb = ByteBuffer.wrap(sbuf);
        return bb.getLong();
    }

}
