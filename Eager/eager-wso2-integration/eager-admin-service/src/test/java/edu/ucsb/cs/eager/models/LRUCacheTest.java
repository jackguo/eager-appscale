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

package edu.ucsb.cs.eager.models;

import edu.ucsb.cs.eager.dao.LRUCache;
import junit.framework.TestCase;

public class LRUCacheTest extends TestCase {

    public void testCache1() {
        LRUCache<String,String> cache = new LRUCache<String, String>(10);
        cache.put("foo", "bar");
        assertEquals("bar", cache.get("foo"));
        assertNull(cache.get("not_foo"));
    }

    public void testCache2() {
        LRUCache<String,String> cache = new LRUCache<String, String>(10);
        for (int i = 0; i < 20; i++) {
            cache.put("key" + i, "value" + i);
        }
        assertEquals(10, cache.size());
        for (int i = 0; i < 10; i++) {
            assertNull(cache.get("key" + i));
        }
        for (int i = 10; i < 20; i++) {
            assertNotNull(cache.get("key" + i));
        }
    }
}
