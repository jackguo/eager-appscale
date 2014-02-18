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

package edu.ucsb.cs.eager.dao;

import edu.ucsb.cs.eager.models.APIInfo;
import edu.ucsb.cs.eager.models.ApplicationInfo;
import edu.ucsb.cs.eager.models.DependencyInfo;
import edu.ucsb.cs.eager.models.EagerException;

public class CachedEagerDependencyMgtDAO extends EagerDependencyMgtDAO {

    private LRUCache<String,DependencyInfo[]> cache = new LRUCache<String, DependencyInfo[]>(1000);

    @Override
    public DependencyInfo[] getDependencies(String name, String version) throws EagerException {
        String key = getCacheKey(name, version);
        synchronized (key.intern()) {
            DependencyInfo[] results = cache.get(key);
            if (results == null) {
                results = super.getDependencies(name, version);
                cache.put(key, results);
            }
            return results;
        }
    }

    @Override
    public boolean recordDependencies(ApplicationInfo app) throws EagerException {
        String key = getCacheKey(app.getName(), app.getVersion());
        cache.remove(key);
        return super.recordDependencies(app);
    }

    private String getCacheKey(String name, String version) {
        return name + ":" + version;
    }
}
