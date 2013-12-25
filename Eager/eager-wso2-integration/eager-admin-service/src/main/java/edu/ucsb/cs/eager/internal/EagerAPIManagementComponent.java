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

package edu.ucsb.cs.eager.internal;

import org.apache.axis2.engine.ListenerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.wso2.apimgt.impl.services" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="listener.manager.service"
 * interface="org.apache.axis2.engine.ListenerManager" cardinality="0..1" policy="dynamic"
 * bind="setListenerManager" unbind="unsetListenerManager"
 *@scr.reference name="tenant.registryloader"
 * interface="org.wso2.carbon.registry.core.service.TenantRegistryLoader"
 * cardinality="1..1" policy="dynamic"
 * bind="setTenantRegistryLoader"
 * unbind="unsetTenantRegistryLoader"

 */
public class EagerAPIManagementComponent {

    private static final Log log = LogFactory.getLog(EagerAPIManagementComponent.class);

    private static String eagerAdmin = null;

    protected void activate(ComponentContext componentContext) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("EAGER API manager component activated");
        }
        eagerAdmin = ServerConfiguration.getInstance().getFirstProperty("Eager.Admin");
        if (eagerAdmin == null) {
            log.warn("EAGER admin user hasn't been configured in carbon.xml");
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        if (log.isDebugEnabled()) {
            log.debug("Deactivating API manager component");
        }
        eagerAdmin = null;
    }

    protected void setRegistryService(RegistryService registryService) {
        if (registryService != null && log.isDebugEnabled()) {
            log.debug("Registry service initialized");
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {

    }

    protected void setRealmService(RealmService realmService) {
        if (realmService != null && log.isDebugEnabled()) {
            log.debug("Realm service initialized");
        }
    }

    protected void unsetRealmService(RealmService realmService) {

    }

    protected void setListenerManager(ListenerManager listenerManager) {
        // We bind to the listener manager so that we can read the local IP
        // address and port numbers properly.
        log.debug("Listener manager bound to the API manager component");
    }

    protected void unsetListenerManager(ListenerManager listenerManager) {
        log.debug("Listener manager unbound from the API manager component");
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {

    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {

    }

    protected void setTenantRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {

    }

    protected void unsetTenantRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {

    }

    public static String getEagerAdmin() {
        if (eagerAdmin == null) {
            throw new IllegalStateException("EAGER admin user has not been set");
        }
        return eagerAdmin;
    }

}
