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

package edu.ucsb.cs.eager.sa.loops.ai;

public class IntervalDomain extends AbstractStateDomain {

    private Integer lowerBound;
    private Integer upperBound;

    public IntervalDomain(Integer lowerBound, Integer upperBound) {
        if (lowerBound != null && upperBound != null && lowerBound > upperBound) {
            throw new IllegalArgumentException("Lower bound must be <= upper bound");
        }
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public int getStates() {
        if (lowerBound != null && upperBound != null) {
            return upperBound - lowerBound + 1;
        }
        return super.getStates();
    }

    public Integer getLowerBound() {
        return lowerBound;
    }

    public Integer getUpperBound() {
        return upperBound;
    }
}
