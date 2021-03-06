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

package edu.ucsb.cs.eager.service;

import edu.ucsb.cs.eager.dao.EagerDependencyMgtDAO;
import edu.ucsb.cs.eager.internal.EagerAPIManagementComponent;
import edu.ucsb.cs.eager.models.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;

import java.util.*;

public class EagerAdmin {

    private static final Log log = LogFactory.getLog(EagerAdmin.class);

    private EagerDependencyMgtDAO dao = new EagerDependencyMgtDAO();

    public boolean isAPIAvailable(APIInfo api) throws EagerException {
        try {
            String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
            APIProvider provider = getAPIProvider(eagerAdmin);
            APIIdentifier apiId = new APIIdentifier(eagerAdmin, api.getName(), api.getVersion());
            return provider.isAPIAvailable(apiId);
        } catch (APIManagementException e) {
            handleException("Error while checking for the existence of API", e);
            return false;
        }
    }

    public APIInfo[] getAPIsWithContext(String context) throws EagerException {
        /*String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
        try {
            APIProvider provider = getAPIProvider(eagerAdmin);
            List<API> apiList = provider.getAllAPIs();
            List<APIInfo> results = new ArrayList<APIInfo>();
            for (API api : apiList) {
                if (api.getContext().equals(context)) {
                    results.add(new APIInfo(api.getId()));
                }
            }
            return results.toArray(new APIInfo[results.size()]);
        } catch (APIManagementException e) {
            handleException("Error while retrieving APIs", e);
            return null;
        }*/
        return dao.getAPIsWithContext(context);
    }

    public String validateDependencies(ApplicationInfo app) throws EagerException {
        String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
        for (DependencyInfo dependency : app.getDependencies()) {
            APIIdentifier apiId = new APIIdentifier(eagerAdmin, dependency.getName(),
                    dependency.getVersion());
            APIInfo dependencyApi = new APIInfo(apiId);
            if (!isAPIAvailable(dependencyApi)) {
                return "Dependency " + toPrintableName(dependency) + " does not exist";
            }
        }

        DependencyGraph graph = new DependencyGraph(app.getName(), app.getVersion(), app.getDependencies());
        if (graph.hasCycle()) {
            return "Cyclic dependency detected";
        }
        return null;
    }

    private String toPrintableName(DependencyInfo api) {
        return api.getName() + "-v" + api.getVersion();
    }

    /**
     * Record the dependencies of an application
     *
     * @param app Dependent application
     * @return a boolean value indicating success or failure
     */
    public boolean recordDependencies(ApplicationInfo app) throws EagerException {
        return dao.recordDependencies(app);
    }

    /**
     * Get the information required to perform dependency checking.
     *
     * @param api A potential dependency API
     * @return A ValidationInfo object carrying the specification of the API and its dependents
     */
    public ValidationInfo getValidationInfo(APIInfo api) throws EagerException {
        ValidationInfo info = new ValidationInfo();
        info.setSpecification(dao.getAPISpec(api));
        info.setDependents(dao.getDependents(api));
        return info;
    }

    public boolean createAPI(APIInfo api, String specification) throws EagerException {
        if (isAPIAvailable(api)) {
            return false;
        }

        try {
            String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
            APIProvider provider = getAPIProvider(eagerAdmin);
            APIIdentifier apiId = new APIIdentifier(eagerAdmin, api.getName(), api.getVersion());
            API newAPI = new API(apiId);
            newAPI.setContext("/" + api.getName().toLowerCase());
            newAPI.setUrl("http://eager4appscale.com");
            newAPI.setStatus(APIStatus.CREATED);

            String[] methods = new String[] {
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
            };
            Set<URITemplate> templates = new HashSet<URITemplate>();
            for (String method : methods) {
                URITemplate template = new URITemplate();
                template.setHTTPVerb(method);
                template.setUriTemplate("/*");
                template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                template.setResourceURI("http://eager4appscale.com");
                templates.add(template);
            }
            newAPI.setUriTemplates(templates);
            newAPI.setLastUpdated(new Date());
            newAPI.setVisibility(APIConstants.API_GLOBAL_VISIBILITY);
            newAPI.addAvailableTiers(provider.getTiers());
            provider.addAPI(newAPI);
            log.info("Registered API: " + api.getName() + "-v" + api.getVersion());

            dao.saveAPISpec(api, specification);
            return true;
        } catch (APIManagementException e) {
            handleException("Error while creating new API", e);
            return false;
        }
    }

    public boolean updateAPISpec(APIInfo api, String specification) throws EagerException {
        if (!isAPIAvailable(api)) {
            return false;
        }

        dao.updateAPISpec(api, specification);
        return true;
    }

    /**
     * Publish the specified API list to the API Store and Gateway.
     *
     * @param apiList List of APIs to be published
     * @param url Backend URL to which APIs should forward traffic
     * @return true if the operation is successful -- Never returns false. Throws an exception
     * to indicate failure
     * @throws EagerException If any of the APIs does not exist or if some other
     * runtime error occurs
     */
    public boolean publishAPIs(APIInfo[] apiList, String url) throws EagerException {
        for (APIInfo api : apiList) {
            if (!isAPIAvailable(api)) {
                throw new EagerException("API " + api.getName() + "-v" +
                        api.getVersion() + " does not exist");
            }
        }

        for (APIInfo api : apiList) {
            try {
                String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
                APIProvider provider = getAPIProvider(eagerAdmin);
                APIIdentifier apiId = new APIIdentifier(eagerAdmin, api.getName(), api.getVersion());
                API existingAPI = provider.getAPI(apiId);
                if (existingAPI.getStatus() != APIStatus.PUBLISHED) {
                    existingAPI.setUrl(url);
                    String[] methods = new String[] {
                            "GET", "POST", "PUT", "DELETE", "OPTIONS"
                    };
                    Set<URITemplate> templates = new HashSet<URITemplate>();
                    for (String method : methods) {
                        URITemplate template = new URITemplate();
                        template.setHTTPVerb(method);
                        template.setUriTemplate("/*");
                        template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
                        template.setResourceURI(url);
                        templates.add(template);
                    }
                    existingAPI.setUriTemplates(templates);
                    existingAPI.setLastUpdated(new Date());
                    provider.updateAPI(existingAPI);
                    provider.changeAPIStatus(existingAPI, APIStatus.PUBLISHED, eagerAdmin, true);
                }
            } catch (APIManagementException e) {
                handleException("Error while publishing API", e);
            }
        }
        return true;
    }

    private APIProvider getAPIProvider(String providerName) throws APIManagementException {
        return APIManagerFactory.getInstance().getAPIProvider(providerName);
    }

    private void handleException(String msg, Exception ex) throws EagerException {
        log.error(msg, ex);
        throw new EagerException(msg, ex);
    }

}
