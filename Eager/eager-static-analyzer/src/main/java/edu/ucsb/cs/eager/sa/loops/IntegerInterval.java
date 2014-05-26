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

package edu.ucsb.cs.eager.sa.loops;

public class IntegerInterval {

    private int lowerBound;
    private int upperBound;

    public IntegerInterval(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerInterval) {
            IntegerInterval other = (IntegerInterval) obj;
            return other.lowerBound == lowerBound && other.upperBound == upperBound;
        }
        return false;
    }

    public IntegerInterval sup(IntegerInterval interval) {
        if (interval != null) {
            return new IntegerInterval(Math.min(lowerBound, interval.lowerBound),
                    Math.max(upperBound, interval.upperBound));
        } else {
            return new IntegerInterval(lowerBound, upperBound);
        }
    }

    public IntegerInterval add(int value) {
        int lBound = lowerBound, uBound = upperBound;
        if (lowerBound != Integer.MIN_VALUE) {
            lBound += value;
        }
        if (upperBound != Integer.MAX_VALUE) {
            uBound += value;
        }
        return new IntegerInterval(lBound, uBound);
    }

    public IntegerInterval widen(IntegerInterval interval) {
        int lBound, uBound;
        if (interval.lowerBound < lowerBound) {
            lBound = Integer.MIN_VALUE;
        } else {
            lBound = interval.lowerBound;
        }

        if (interval.upperBound > upperBound) {
            uBound = Integer.MAX_VALUE;
        } else {
            uBound = interval.upperBound;
        }
        return new IntegerInterval(lBound, uBound);
    }

    public IntegerInterval gt(int value) {
        return gt(new IntegerInterval(value, value));
    }

    public IntegerInterval gt(IntegerInterval interval) {
        int lBound, uBound;
        if (upperBound > interval.upperBound) {
            uBound = upperBound;
        } else {
            uBound = Integer.MAX_VALUE;
        }

        if (lowerBound > interval.upperBound) {
            lBound = lowerBound;
        } else {
            lBound = interval.upperBound + 1;
        }
        return new IntegerInterval(lBound, uBound);
    }

    public IntegerInterval lte(int value) {
        return lte(new IntegerInterval(value, value));
    }

    public IntegerInterval lte(IntegerInterval interval) {
        int lBound, uBound;
        if (upperBound > interval.lowerBound) {
            uBound = interval.upperBound;
        } else {
            uBound = upperBound;
        }

        if (lowerBound > interval.lowerBound) {
            lBound = interval.lowerBound;
        } else {
            lBound = lowerBound;
        }
        return new IntegerInterval(lBound, uBound);
    }

    @Override
    public String toString() {
        return "[" + lowerBound + "," + upperBound + "]";
    }
}
