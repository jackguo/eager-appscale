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

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIStatus;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;

import java.util.*;

public class EagerAdmin {

    public boolean isAPIExists(String name, String version,
                               String providerName) throws APIManagementException {
        APIProvider provider = getAPIProvider(providerName);
        APIIdentifier apiId = new APIIdentifier(providerName, name, version);
        return provider.isAPIAvailable(apiId);
    }

    public APIIdentifier[] getAPIsWithContext(String context,
                                           String providerName) throws APIManagementException {
        APIProvider provider = getAPIProvider(providerName);
        List<API> apiList = provider.getAllAPIs();
        List<APIIdentifier> results = new ArrayList<APIIdentifier>();
        for (API api : apiList) {
            if (api.getContext().equals(context)) {
                results.add(api.getId());
            }
        }
        return results.toArray(new APIIdentifier[results.size()]);
    }

    public boolean createAndPublishAPI(String name, String version,
                                       String providerName) throws APIManagementException {
        APIProvider provider = getAPIProvider(providerName);
        APIIdentifier apiId = new APIIdentifier(providerName, name, version);
        API api = new API(apiId);
        api.setContext("/" + name.toLowerCase());
        api.setUrl("http://test.com");
        api.setStatus(APIStatus.CREATED);

        String[] methods = new String[] {
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        };
        Set<URITemplate> templates = new HashSet<URITemplate>();
        for (String method : methods) {
            URITemplate template = new URITemplate();
            template.setHTTPVerb(method);
            template.setUriTemplate("/*");
            template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
            template.setResourceURI("http://test.com");
            templates.add(template);
        }
        api.setUriTemplates(templates);
        api.setLastUpdated(new Date());
        provider.addAPI(api);
        provider.changeAPIStatus(api, APIStatus.PUBLISHED, providerName, true);
        return true;
    }

    private APIProvider getAPIProvider(String providerName) throws APIManagementException {
        return APIManagerFactory.getInstance().getAPIProvider(providerName);
    }

}
