/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.apimgt.impl;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.api.model.Tag;
import org.wso2.carbon.apimgt.handlers.security.stub.types.APIKeyMapping;
import org.wso2.carbon.apimgt.impl.internal.APIManagerComponent;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIAuthenticationAdminClient;
import org.wso2.carbon.apimgt.impl.utils.APINameComparator;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.utils.APIVersionComparator;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides the core API store functionality. It is implemented in a very
 * self-contained and 'pure' manner, without taking requirements like security into account,
 * which are subject to frequent change. Due to this 'pure' nature and the significance of
 * the class to the overall API management functionality, the visibility of the class has
 * been reduced to package level. This means we can still use it for internal purposes and
 * possibly even extend it, but it's totally off the limits of the users. Users wishing to
 * programmatically access this functionality should use one of the extensions of this
 * class which is visible to them. These extensions may add additional features like
 * security to this class.
 */
class APIConsumerImpl extends AbstractAPIManager implements APIConsumer {

    private static final Log log = LogFactory.getLog(APIConsumerImpl.class);
    private boolean isTenantModeStoreView;
    private String requestedTenant;
    /* Map to Store APIs against Tag */
    private Map<String, Set<API>> taggedAPIs;

    public APIConsumerImpl() throws APIManagementException {
        super();
    }

    public APIConsumerImpl(String username) throws APIManagementException {
        super(username);
    }

    public Subscriber getSubscriber(String subscriberId) throws APIManagementException {
        Subscriber subscriber = null;
        try {
            subscriber = apiMgtDAO.getSubscriber(subscriberId);
        } catch (APIManagementException e) {
            handleException("Failed to get Subscriber", e);
        }
        return subscriber;
    }


    /**
     * Returns the set of APIs with the given tag from the taggedAPIs Map
     *
     * @param tag
     * @return
     * @throws APIManagementException
     */
    public Set<API> getAPIsWithTag(String tag) throws APIManagementException {
        return taggedAPIs.get(tag);
    }


    /**
     * Returns the set of APIs with the given tag, retrieved from registry
     *
     * @param registry - Current registry; tenant/SuperTenant
     * @param tag
     * @return
     * @throws APIManagementException
     */
    private Set<API> getAPIsWithTag(Registry registry, String tag, String requestedTenantDomain, boolean isTenantMode, String domainOfIteratedTenant) throws APIManagementException {
        Set<API> apiSet = new TreeSet<API>(new APINameComparator());
        try {
            String resourceByTagQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/resource-by-tag";
            Map<String, String> params = new HashMap<String, String>();
            params.put("1", tag);
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.RESOURCE_UUID_RESULT_TYPE);
            Collection collection = registry.executeQuery(resourceByTagQueryPath, params);

            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                    APIConstants.API_KEY);

            for (String row : collection.getChildren()) {
                String uuid = row.substring(row.indexOf(";") + 1, row.length());
                GenericArtifact genericArtifact = artifactManager.getGenericArtifact(uuid);
                if (genericArtifact != null) {
                    String visibility = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY);
                    String visibleTenantDomains = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS);

                    boolean isPublicVisibility = APIConstants.API_GLOBAL_VISIBILITY.equals(visibility);
                    boolean isControlledVisibility = APIConstants.API_CONTROLLED_VISIBILITY.equals(visibility);
                    boolean isRestrictedVisibility = APIConstants.API_RESTRICTED_VISIBILITY.equals(visibility);

                    if (isPublicVisibility && !isTenantMode) {
                        apiSet.add(APIUtil.getAPI(genericArtifact));
                    }
                    if (isControlledVisibility && isTenantMode) {
                        if (isAllowedTenantDomain(visibleTenantDomains, requestedTenantDomain)) {
                            apiSet.add(APIUtil.getAPI(genericArtifact));
                        }
                    }
                    else if(isRestrictedVisibility &&
                            isArtifactViewable(genericArtifact, requestedTenantDomain, domainOfIteratedTenant)){
                        apiSet.add(APIUtil.getAPI(genericArtifact));
                    }
                    if (domainOfIteratedTenant.equals(requestedTenantDomain)) {
                         if(isLoggedinTenantSearchingOwnAPIs(genericArtifact))  {
                          apiSet.add(APIUtil.getAPI(genericArtifact));
                         }
                    }
                }
            }
        } catch (Exception e) {
            handleException("Failed to get API for tag " + tag, e);
        }
        return apiSet;
    }

    /**
     * The method to get APIs to Store view
     *
     * @param requestedTenantDomain tenantDomain
     * @return Set<API>  Set of APIs
     * @throws APIManagementException
     */
    public Set<API> getAllPublishedAPIs(String requestedTenantDomain) throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        boolean isTenantModeStoreView = (requestedTenantDomain != null && !requestedTenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME));
        try {
            Set<GenericArtifact> tenantArtifacts = new HashSet<GenericArtifact>();
            //First check store is running in tenant unaware mode or not
            //if(tenantDomain==null||!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME) ) {
            //Retrieve existing tenants array to start getting APIs from tenants registry spaces
            Tenant[] tenants = APIUtil.getAllTenantsWithSuperTenant().toArray(
                    new Tenant[APIUtil.getAllTenantsWithSuperTenant().size()]);

            //If the instance is having tenants [running in tenant mode],then initiate each tenant registry space and get APIs from each tenant space
            //according to API visibility
            if (tenants != null && tenants.length != 0) {
                Registry registry;
                
                for (Tenant tenant : tenants) {

                    loadTenantRegistry(tenant.getId());

                    //First,if the Store is in anonymous view,try to get anonymous user allowed APIs from tenant spaces
                    if (tenantDomain == null) {
                        registry = ServiceReferenceHolder.getInstance().
                                getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenant.getId());
                    } else if (tenantId == tenant.getId()) { //Then if going to retrieve same tenant registry equal to current registry instance
                        registry = this.registry;
                    } else {
                        registry = ServiceReferenceHolder.getInstance().
                                getRegistryService().getGovernanceSystemRegistry(tenant.getId());

                    }
                    Set<GenericArtifact> tenantAPIs = getTenantAPIs(registry, requestedTenantDomain, tenant.getDomain(), isTenantModeStoreView);
                    if (tenantAPIs != null) {
                        tenantArtifacts.addAll(tenantAPIs);
                    }
                }
            }

            Set<GenericArtifact> genericArtifacts = new HashSet<GenericArtifact>();

            if (tenantArtifacts != null && tenantArtifacts.size() != 0) {
                genericArtifacts.addAll(tenantArtifacts);
            }
            if (genericArtifacts == null || genericArtifacts.size() == 0) {
                return apiSortedSet;
            }

            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean displayMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean displayAPIsWithMultipleStatus = isAllowDisplayAPIsWithMultipleStatus();
            for (GenericArtifact artifact : genericArtifacts) {
                // adding the API provider can mark the latest API .
                String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                API api = null;
                //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                if (!displayAPIsWithMultipleStatus) {
                    // then we are only interested in published APIs here...
                    if (status.equals(APIConstants.PUBLISHED)) {
                        api = APIUtil.getAPI(artifact);
                    }
                } else {   // else we are interested in both deprecated/published APIs here...
                    if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                        api = APIUtil.getAPI(artifact);

                    }

                }
                if (api != null) {
                    String key;
                    //Check the configuration to allow showing multiple versions of an API true/false
                    if (!displayMultipleVersions) { //If allow only showing the latest version of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                        API existingAPI = latestPublishedAPIs.get(key);
                        if (existingAPI != null) {
                            // If we have already seen an API with the same name, make sure
                            // this one has a higher version number
                            if (versionComparator.compare(api, existingAPI) > 0) {
                                latestPublishedAPIs.put(key, api);
                            }
                        } else {
                            // We haven't seen this API before
                            latestPublishedAPIs.put(key, api);
                        }
                    } else { //If allow showing multiple versions of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                .getVersion();
                        multiVersionedAPIs.add(api);
                    }
                }
            }
            if (!displayMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }


        } catch (RegistryException e) {
            handleException("Failed to get all published APIs", e);
        } catch (org.wso2.carbon.user.api.UserStoreException e1) {
            handleException("Failed to get all published APIs", e1);
        }
        return apiSortedSet;

    }

    private <T> T[] concatArrays(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }


    public Set<API> getTopRatedAPIs(int limit) throws APIManagementException {
        int returnLimit = 0;
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            GenericArtifact[] genericArtifacts = artifactManager.getAllGenericArtifacts();
            if (genericArtifacts == null || genericArtifacts.length == 0) {
                return apiSortedSet;
            }
            for (GenericArtifact genericArtifact : genericArtifacts) {
                String status = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                if (status.equals(APIConstants.PUBLISHED)) {
                    String artifactPath = genericArtifact.getPath();

                    float rating = registry.getAverageRating(artifactPath);
                    if (rating > APIConstants.TOP_TATE_MARGIN && (returnLimit < limit)) {
                        returnLimit++;
                        apiSortedSet.add(APIUtil.getAPI(genericArtifact, registry));
                    }
                }
            }
        } catch (RegistryException e) {
            handleException("Failed to get top rated API", e);
        }
        return apiSortedSet;
    }

    /**
     * Get the recently added APIs set
     *
     * @param limit                 no limit. Return everything else, limit the return list to specified value.
     * @param requestedTenantDomain This value is required when need to get tenant specific recently added APIs.In standalone mode,value is null
     * @return Set<API>
     * @throws APIManagementException
     */
    public Set<API> getRecentlyAddedAPIs(int limit, String requestedTenantDomain) throws APIManagementException {
        Set<API> recentlyAddedAPIs = new HashSet<API>();

        try {
            String loggedInUserDomain = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getDomain(((UserRegistry) this.registry).getTenantId());
            org.wso2.carbon.user.api.UserRealm  realm = ServiceReferenceHolder.getInstance().getRealmService().getTenantUserRealm(this.tenantId);
            String latestAPIQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/latest-apis";
            Map<String, String> params = new HashMap<String, String>();
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.RESOURCES_RESULT_TYPE);
            int resultSetSize = 0;
            Collection collection = registry.executeQuery(latestAPIQueryPath, params);
            resultSetSize = Math.min(limit, collection.getChildCount());
            String[] recentlyAddedAPIPaths = new String[resultSetSize];
            for (int i = 0; i < resultSetSize; i++) {
                recentlyAddedAPIPaths[i] = collection.getChildren()[i];
            }
            boolean isTenantModeStoreView = (requestedTenantDomain != null && !requestedTenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME));
            this.isTenantModeStoreView = isTenantModeStoreView;
            this.requestedTenant = requestedTenantDomain;
            String currentTenantDomain;
            if (tenantDomain != null) {
                currentTenantDomain = tenantDomain;
            } else {
                int currentTenantId = ServiceReferenceHolder.getUserRealm().getRealmConfiguration().getTenantId();
                currentTenantDomain = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getDomain(currentTenantId);
            }

            //Retrieve existing tenants array to start getting APIs from tenants registry spaces
            //All tenants includes super tenant
            Tenant[] tenants = APIUtil.getAllTenantsWithSuperTenant().toArray(new Tenant[0]);
            //If the store is running in tenant mode,first check the recently added APIs limit is already fulfilled or not.If not
            //iterating through each tenant's registry
            Registry tenantConfRegistry = null;
            if (limit > 0 && tenants != null && tenants.length != 0) {
                for (Tenant tenant : tenants) {

                    loadTenantRegistry(tenant.getId());

                    Registry tenantRegistry = null;
                    //First,if the Store is in anonymous view,try to get anonymous user allowed APIs from tenant spaces
                    if (tenantDomain == null) {
                        tenantConfRegistry = ServiceReferenceHolder.getInstance().
                                getRegistryService().getConfigUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenant.getId());
                        tenantRegistry = ServiceReferenceHolder.getInstance().
                                getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenant.getId());
                    } else {
                        tenantRegistry = ServiceReferenceHolder.getInstance().
                                getRegistryService().getGovernanceSystemRegistry(tenant.getId());
                        tenantConfRegistry = ServiceReferenceHolder.getInstance().
                                getRegistryService().getConfigUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenant.getId());
                    }

                    if (tenantRegistry != null && tenantConfRegistry.resourceExists(latestAPIQueryPath)) {
                        Collection tenantCollection = tenantRegistry.executeQuery(latestAPIQueryPath, params);

                        if (tenantCollection != null) {

                            Set<API> apiSortedSet = getAPIs(tenantRegistry, limit, tenantCollection.getChildren(), isTenantModeStoreView, tenant.getDomain(), requestedTenantDomain);
                            limit = limit - apiSortedSet.size();
                            recentlyAddedAPIs.addAll(apiSortedSet);
                        }
                    }
                }
               limit = 0;

            }
            return recentlyAddedAPIs;


        } catch (RegistryException e) {
            handleException("Failed to get recently added APIs", e);
            return null;
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException("Failed to get recently added APIs", e);
            return null;
        }


    }

    public Set<Tag> getAllTags() throws APIManagementException {
        Set<Tag> tagSet = new TreeSet<Tag>(new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        boolean isTenantModeStoreView = this.isTenantModeStoreView;
        taggedAPIs = new HashMap<String, Set<API>>();
        try {
            String tagsQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/tag-summary";
            Map<String, String> params = new HashMap<String, String>();
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.TAG_SUMMARY_RESULT_TYPE);
            /* First check store is running in tenant unware mode or not */
            //if (tenantDomain == null || !tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            /*Retrieve existing tenants array to start getting APIs from tenants registry spaces */
            Tenant[] tenantsWithSuperTenant = APIUtil.getAllTenantsWithSuperTenant().toArray(new Tenant[0]);
            /*If the instance is having tenants [running in tenant mode],then initiate each tenant registry space and
            get APIs from each tenant space according to API visibility*/
            if (tenantsWithSuperTenant != null && tenantsWithSuperTenant.length != 0) {
                for (Tenant tenant : tenantsWithSuperTenant) {
                    Registry govRegistry = ServiceReferenceHolder.getInstance().getRegistryService().
                            getGovernanceSystemRegistry( tenant.getId());
                    Registry confRegistry = ServiceReferenceHolder.getInstance().getRegistryService().
                            getConfigUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenant.getId());
                    Collection collection;
                    if (confRegistry.resourceExists(tagsQueryPath)) {
                        collection = govRegistry.executeQuery(tagsQueryPath, params);
                        for (String fullTag : collection.getChildren()) {
                            //remove hardcoded path value
                            String tagName = fullTag.substring(fullTag.indexOf(";") + 1, fullTag.indexOf(":"));
                            String tagOccurenceCountStr = fullTag.substring(fullTag.indexOf(":") + 1, fullTag.length());
                            int tagOccurenceCount = Integer.valueOf(tagOccurenceCountStr).intValue();
                            Set<API> apisWithTag = getAPIsWithTag(govRegistry, tagName, requestedTenant, isTenantModeStoreView, tenant.getDomain());
                            if (apisWithTag.size() != 0) {
                                //tagSet.add(new Tag(tagName, tagOccurenceCount));
                                /* Add the APIs against the tag name */
                                Iterator<API> it = apisWithTag.iterator();
                                while (it.hasNext()) {
                                    API api = it.next();
                                    String visibleTenants = api.getVisibleTenants();
                                    if (visibleTenants != null) {
                                        List<String> visibleTenantArr = Arrays.asList(visibleTenants.split(","));
                                        if (tenantDomain!=null && !visibleTenantArr.contains(tenantDomain)) {
                                            //apisWithTag.remove(api);
                                            it.remove();
                                        }
                                    }
                                }
                                if (apisWithTag.size() != 0) {
                                    if (taggedAPIs.containsKey(tagName)) {
                                        for (API api : apisWithTag) {
                                            taggedAPIs.get(tagName).add(api);
                                        }
                                    } else {
                                        taggedAPIs.put(tagName, apisWithTag);
                                    }
                                    tagSet.add(new Tag(tagName, tagOccurenceCount));
                                }
                            }
                        }
                    }
                }
            }


        } catch (RegistryException e) {
            handleException("Failed to get all the tags", e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException("Error in retrieving all the tags", e);
        }
        return tagSet;
    }

    public void rateAPI(APIIdentifier apiId, APIRating rating,
                        String user) throws APIManagementException {
        apiMgtDAO.addRating(apiId, rating.getRating(), user);

    }

    public void removeAPIRating(APIIdentifier apiId, String user) throws APIManagementException {
        apiMgtDAO.removeAPIRating(apiId, user);

    }

    public int getUserRating(APIIdentifier apiId, String user) throws APIManagementException {
        return apiMgtDAO.getUserRating(apiId, user);
    }

    public Set<API> getPublishedAPIsByProvider(String providerId, int limit)
            throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean displayMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean displayAPIsWithMultipleStatus = isAllowDisplayAPIsWithMultipleStatus();
            String providerPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                    providerId;
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                    APIConstants.API_KEY);
            Association[] associations = registry.getAssociations(providerPath,
                    APIConstants.PROVIDER_ASSOCIATION);
            if (associations.length < limit || limit == -1) {
                limit = associations.length;
            }
            for (int i = 0; i < limit; i++) {
                Association association = associations[i];
                String apiPath = association.getDestinationPath();
                Resource resource = registry.get(apiPath);
                String apiArtifactId = resource.getUUID();
                if (apiArtifactId != null) {
                    GenericArtifact artifact = artifactManager.getGenericArtifact(apiArtifactId);
                    // check the API status
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api = null;
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!displayAPIsWithMultipleStatus) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(artifact);
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(artifact);

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!displayMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                } else {
                    throw new GovernanceException("artifact id is null of " + apiPath);
                }
            }
            if (!displayMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }

        } catch (RegistryException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        }


    }

    public Set<API> getPublishedAPIsByProvider(String providerId, String loggedUsername, int limit)
            throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean allowMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean showAllAPIs = isAllowDisplayAPIsWithMultipleStatus();

            String providerDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(providerId));
            int id = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getTenantId(providerDomain);
            Registry registry = ServiceReferenceHolder.getInstance().
                    getRegistryService().getGovernanceSystemRegistry(id);

            org.wso2.carbon.user.api.AuthorizationManager manager = ServiceReferenceHolder.getInstance().
                    getRealmService().getTenantUserRealm(id).
                    getAuthorizationManager();

            String providerPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                    providerId;
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                    APIConstants.API_KEY);
            Association[] associations = registry.getAssociations(providerPath,
                    APIConstants.PROVIDER_ASSOCIATION);
//            if (associations.length < limit || limit == -1) {
//                limit = associations.length;
//            }
            int publishedAPICount = 0;

            for (int i = 0; i < associations.length; i++) {

                if (publishedAPICount >= limit) {
                    break;
                }

                Association association = associations[i];
                String apiPath = association.getDestinationPath();

                Resource resource;
                String path = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                        RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + apiPath);
                boolean checkAuthorized = false;
                String userNameWithoutDomain = loggedUsername;

                String loggedDomainName = "";
                if (!"".equals(loggedUsername) &&
                        !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(super.tenantDomain)) {
                    String [] nameParts = loggedUsername.split("@");
                    loggedDomainName = nameParts[1];
                    userNameWithoutDomain = nameParts[0];
                }

               if(loggedUsername.equals("")){
                // Anonymous user is viewing.
                checkAuthorized = manager.isRoleAuthorized(APIConstants.ANONYMOUS_ROLE, path, ActionConstants.GET);
                }else {
                // Some user is logged in.
                    checkAuthorized = manager.isUserAuthorized(userNameWithoutDomain, path, ActionConstants.GET);
                }

                String apiArtifactId = null;
                if (checkAuthorized) {
                    resource = registry.get(apiPath);
                    apiArtifactId = resource.getUUID();
                }

                if (apiArtifactId != null) {
                    GenericArtifact artifact = artifactManager.getGenericArtifact(apiArtifactId);

                    if(!this.isArtifactViewable(artifact,loggedDomainName,providerDomain)){
                        continue;
                    }
                    // check the API status
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api = null;
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!showAllAPIs) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(artifact);
                           // if (checkDomainVisibility(loggedDomainName, api)) break;
                            publishedAPICount++;
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(artifact);
                           // if (checkDomainVisibility(loggedDomainName, api)) break;
                            publishedAPICount++;

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                }
            }
            if (!allowMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }

        } catch (RegistryException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        } catch (UserStoreException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        }


    }

    /**
     * When the logged in tenant domain and the API is passed
     * The function checks if the said API should be made visible to the domain.
     *
     * @param loggedDomainName
     * @param api
     * @return
     */
    private boolean checkDomainVisibility(String loggedDomainName, API api) {
        String visibleTenants = api.getVisibleTenants();
        if (visibleTenants != null) {
            List<String> visibleTenantArr = Arrays.asList(visibleTenants.split(","));
            if (!visibleTenantArr.contains(loggedDomainName)) {
                return true;
            }
        }
        return false;
    }

    public Set<API> searchAPI(String searchTerm, String searchType, String requestedTenantDomain) throws APIManagementException {
        Set<API> apiSet = new HashSet<API>();
        try {
            Tenant[] tenants = APIUtil.getAllTenantsWithSuperTenant().toArray(new Tenant[0]);
            boolean isAnonymousUserLoggedin = ((UserRegistry)this.registry).getUserName().equals(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);

            if (tenants != null && tenants.length != 0) {
                for (Tenant tenant : tenants) {
                    Registry registry;

                        //First,if the Store is in anonymous view,try to get anonymous user allowed APIs from tenant spaces
                        if (isAnonymousUserLoggedin) {
                            registry = ServiceReferenceHolder.getInstance().
                                    getRegistryService().getGovernanceUserRegistry(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, tenant.getId());
                        //
                        } else  if(tenantDomain != null && tenantDomain.equals(requestedTenantDomain) && tenantDomain.equals(tenant.getDomain())){                            // If logged in user is searching own APIS.

                            registry = this.registry;

                        } else {
                            registry = ServiceReferenceHolder.getInstance().
                                    getRegistryService().getGovernanceSystemRegistry(tenant.getId());
                        }
                        apiSet.addAll(searchAPI(registry, searchTerm, searchType, requestedTenantDomain, tenant.getDomain()));
                    }
                }
        } catch (RegistryException e) {
            handleException("Failed to Search APIs", e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException("Failed to Search APIs", e);
        }
        return apiSet;
    }

    public Set<API> searchAPI(Registry registry, String searchTerm, String searchType, String requestedTenantDomain, String iteratedTenantDomain) throws APIManagementException {
        SortedSet<API> apiSet = new TreeSet<API>(new APINameComparator());
        String regex = "(?i)[a-zA-Z0-9_.-|]*" + searchTerm.trim() + "(?i)[a-zA-Z0-9_.-|]*";
        Pattern pattern;
        Matcher matcher;
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            if (artifactManager != null) {
                GenericArtifact[] genericArtifacts = artifactManager
                        .getAllGenericArtifacts();
                if (genericArtifacts == null || genericArtifacts.length == 0) {
                    return apiSet;
                }
                pattern = Pattern.compile(regex);

                for (GenericArtifact artifact : genericArtifacts) {

                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    if(isArtifactViewable(artifact,requestedTenantDomain,iteratedTenantDomain)) {
                        if (searchType.equalsIgnoreCase("Provider")) {
                            String api = APIUtil.replaceEmailDomainBack(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER));
                            matcher = pattern.matcher(api);
                        } else if (searchType.equalsIgnoreCase("Version")) {
                            String api = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
                            matcher = pattern.matcher(api);
                        } else if (searchType.equalsIgnoreCase("Context")) {
                            String api = artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT);
                            matcher = pattern.matcher(api);
                        } else {
                            String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
                            matcher = pattern.matcher(apiName);
                        }
                        if (isAllowDisplayAPIsWithMultipleStatus()) {
                            if (matcher.matches() && (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED))) {
                                apiSet.add(APIUtil.getAPI(artifact, registry));
                            }
                        } else {
                            if (matcher.matches() && status.equals(APIConstants.PUBLISHED)) {
                                apiSet.add(APIUtil.getAPI(artifact, registry));
                            }
                        }
                    }
                }
            }
        } catch (RegistryException e) {
            handleException("Failed to search APIs with type", e);
        } catch (UserStoreException e) {
            handleException("Failed to search APIs with type", e);
        }
        return apiSet;
    }

    public Set<SubscribedAPI> getSubscribedAPIs(Subscriber subscriber) throws APIManagementException {
        Set<SubscribedAPI> subscribedAPIs = null;
        try {
            subscribedAPIs = apiMgtDAO.getSubscribedAPIs(subscriber);
        } catch (APIManagementException e) {
            handleException("Failed to get APIs of " + subscriber.getName(), e);
        }
        return subscribedAPIs;
    }

    public Set<APIIdentifier> getAPIByConsumerKey(String accessToken) throws APIManagementException {
        try {
            return apiMgtDAO.getAPIByConsumerKey(accessToken);
        } catch (APIManagementException e) {
            handleException("Error while obtaining API from API key", e);
        }
        return null;
    }

    public boolean isSubscribed(APIIdentifier apiIdentifier, String userId)
            throws APIManagementException {
        boolean isSubscribed;
        try {
            isSubscribed = apiMgtDAO.isSubscribed(apiIdentifier, userId);
        } catch (APIManagementException e) {
            String msg = "Failed to check if user(" + userId + ") has subscribed to " + apiIdentifier;
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return isSubscribed;
    }

    public void addSubscription(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        API api = getAPI(identifier);
        if (api.getStatus().equals(APIStatus.PUBLISHED)) {
            apiMgtDAO.addSubscription(identifier, api.getContext(), applicationId);
            invalidateCachedKeys(applicationId, identifier);
        } else {
            throw new APIManagementException("Subscriptions not allowed on APIs in the state: " +
                    api.getStatus().getStatus());
        }
    }

    public void removeSubscription(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        apiMgtDAO.removeSubscription(identifier, applicationId);
        invalidateCachedKeys(applicationId, identifier);
    }

    private void invalidateCachedKeys(int applicationId, APIIdentifier identifier) throws APIManagementException {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        if (config.getFirstProperty(APIConstants.API_GATEWAY_SERVER_URL) == null) {
            return;
        }

        Set<String> keys = apiMgtDAO.getApplicationKeys(applicationId);
        if (keys.size() > 0) {
            List<APIKeyMapping> mappings = new ArrayList<APIKeyMapping>();
            API api = getAPI(identifier);
            for (String key : keys) {
                APIKeyMapping mapping = new APIKeyMapping();
                mapping.setKey(key);
                mapping.setApiVersion(identifier.getVersion());
                mapping.setContext(api.getContext());
                mappings.add(mapping);
            }

            try {
                APIAuthenticationAdminClient client = new APIAuthenticationAdminClient();
                client.invalidateKeys(mappings);
            } catch (AxisFault axisFault) {
                log.warn("Error while invalidating API keys at the gateway", axisFault);
            }
        }
    }

    public void removeSubscriber(APIIdentifier identifier, String userId)
            throws APIManagementException {
        throw new UnsupportedOperationException("Unsubscribe operation is not yet implemented");
    }

    public void updateSubscriptions(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        API api = getAPI(identifier);
        apiMgtDAO.updateSubscriptions(identifier, api.getContext(), applicationId);
    }

    public void addComment(APIIdentifier identifier, String commentText, String user) throws APIManagementException {
        apiMgtDAO.addComment(identifier, commentText, user);
    }

    public org.wso2.carbon.apimgt.api.model.Comment[] getComments(APIIdentifier identifier)
            throws APIManagementException {
        return apiMgtDAO.getComments(identifier);
    }

    public void addApplication(Application application, String userId)
            throws APIManagementException {
        apiMgtDAO.addApplication(application, userId);
    }

    public void updateApplication(Application application) throws APIManagementException {
        apiMgtDAO.updateApplication(application);
    }

    public void removeApplication(Application application) throws APIManagementException {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        boolean gatewayExists = config.getFirstProperty(APIConstants.API_GATEWAY_SERVER_URL) != null;
        Set<SubscribedAPI> apiSet = null;
        Set<String> keys = null;
        if (gatewayExists) {
            keys = apiMgtDAO.getApplicationKeys(application.getId());
            apiSet = getSubscribedAPIs(application.getSubscriber());
        }
        apiMgtDAO.deleteApplication(application);

        if (gatewayExists && apiSet != null && keys != null) {
            Set<SubscribedAPI> removables = new HashSet<SubscribedAPI>();
            for (SubscribedAPI api : apiSet) {
                if (!api.getApplication().getName().equals(application.getName())) {
                    removables.add(api);
                }
            }

            for (SubscribedAPI api : removables) {
                apiSet.remove(api);
            }

            List<APIKeyMapping> mappings = new ArrayList<APIKeyMapping>();
            for (String key : keys) {
                for (SubscribedAPI api : apiSet) {
                    APIKeyMapping mapping = new APIKeyMapping();
                    API apiDefinition = getAPI(api.getApiId());
                    mapping.setApiVersion(api.getApiId().getVersion());
                    mapping.setContext(apiDefinition.getContext());
                    mapping.setKey(key);
                    mappings.add(mapping);
                }
            }

            if (mappings.size() > 0) {
                try {
                    APIAuthenticationAdminClient client = new APIAuthenticationAdminClient();
                    client.invalidateKeys(mappings);
                } catch (AxisFault axisFault) {
                    // Just logging the error is enough - We have already deleted the application
                    // which is what's important
                    log.warn("Error while invalidating API keys at the gateway", axisFault);
                }
            }
        }
    }

    public Application[] getApplications(Subscriber subscriber) throws APIManagementException {
        return apiMgtDAO.getApplications(subscriber);
    }

    public boolean isApplicationTokenExists(String accessToken) throws APIManagementException {
        return apiMgtDAO.isAccessTokenExists(accessToken);
    }

    public Set<SubscribedAPI> getSubscribedIdentifiers(Subscriber subscriber, APIIdentifier identifier)
            throws APIManagementException {
        Set<SubscribedAPI> subscribedAPISet = new HashSet<SubscribedAPI>();
        Set<SubscribedAPI> subscribedAPIs = getSubscribedAPIs(subscriber);
        for (SubscribedAPI api : subscribedAPIs) {
            if (api.getApiId().equals(identifier)) {
                subscribedAPISet.add(api);
            }
        }
        return subscribedAPISet;
    }

    /**
     * Returns a list of pre-defined # {@link org.wso2.carbon.apimgt.api.model.Tier} in the system.
     *
     * @return Set<Tier>
     */
    public Set<Tier> getTiers() throws APIManagementException {
        Set<Tier> tiers = new TreeSet<Tier>(new Comparator<Tier>() {
            public int compare(Tier o1, Tier o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Map<String, Tier> tierMap = APIUtil.getTiers();
        tiers.addAll(tierMap.values());
        return tiers;
    }

    private boolean isAllowDisplayAPIsWithMultipleStatus() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String displayAllAPIs = config.getFirstProperty(APIConstants.API_STORE_DISPLAY_ALL_APIS);
        if (displayAllAPIs == null) {
            log.warn("The configurations related to show deprecated APIs in APIStore " +
                    "are missing in api-manager.xml.");
            return false;
        }
        return Boolean.parseBoolean(displayAllAPIs);
    }

    private boolean isAllowDisplayMultipleVersions() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();

        String displayMultiVersions = config.getFirstProperty(APIConstants.API_STORE_DISPLAY_MULTIPLE_VERSIONS);
        if (displayMultiVersions == null) {
            log.warn("The configurations related to show multiple versions of API in APIStore " +
                    "are missing in api-manager.xml.");
            return false;
        }
        return Boolean.parseBoolean(displayMultiVersions);
    }

    /**
     * Get the APIs own by each tenants
     *
     * @param registry               Governance registry space for each tenant
     * @param requestedTenantDomain  tenant domain come with the request
     * @param domainOfIteratedTenant tenant domain of current iterating tenant
     * @param isTenantMode           is the Store running in global/tenant mode
     * @return Set<GenericArtifact> Set of APIs
     * @throws APIManagementException
     * @throws GovernanceException
     */
    public Set<GenericArtifact> getTenantAPIs(Registry registry, String requestedTenantDomain, String domainOfIteratedTenant,
                                              boolean isTenantMode)
            throws APIManagementException, GovernanceException {

        try {

            String loggedInUserDomain = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getDomain(((UserRegistry) this.registry).getTenantId());
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            if (artifactManager != null) {
                GenericArtifact[] genericArtifacts = artifactManager.getAllGenericArtifacts();
                if (genericArtifacts == null || genericArtifacts.length == 0) {
                    return null;
                }
                Set<GenericArtifact> filteredArtifacts = new HashSet<GenericArtifact>();
                for (GenericArtifact artifact : genericArtifacts) {
                    String visibility = artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY);
                    String visibleTenantDomains = artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS);

                    boolean isPublicVisibility = visibility.equals(APIConstants.API_GLOBAL_VISIBILITY);
                    boolean isControlledVisibility = visibility.equals(APIConstants.API_CONTROLLED_VISIBILITY);
                    boolean isRestrictedVisibility = visibility.equals(APIConstants.API_RESTRICTED_VISIBILITY);
                    /*First check whether the Store running in global mode [/Store]*/

                    if (!isTenantMode) {
                        /*If yes,then filter the APIs with 'public visibility' and if the store running in a
                              logged user mode, all APIs visible to/shared with said tenant will be displayed*/
                        //if (isPublicVisibility || domainOfIteratedTenant.equals(requestedTenantDomain)) {
                        if (isPublicVisibility) {
                            filteredArtifacts.add(artifact);
                        } else if(isRestrictedVisibility &&
                                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(loggedInUserDomain) &&
                                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domainOfIteratedTenant)){
                            // Adds a restricted API for when a user logs in with matching role.
                            // This block verifies if the SuperTenant is logged in and APIs created by ST are iterated
                            filteredArtifacts.add(artifact);
                        }
                    } else { /*If the Store running in tenant base mode [/store/?tenant=...],then filer APIs own by that tenant and additionally
	                			the APIs with 'Controlled' visibility from other tenants,which can be accessible by current tenant.*/

                        if (requestedTenantDomain != null && requestedTenantDomain.equals(domainOfIteratedTenant) ||
                                (isControlledVisibility && isAllowedTenantDomain(visibleTenantDomains, requestedTenantDomain))) {
                            filteredArtifacts.add(artifact);
                        } else if(isRestrictedVisibility && requestedTenantDomain.equals(domainOfIteratedTenant) && domainOfIteratedTenant.equals(loggedInUserDomain)){
                            // Adds a restricted APIs for a user in a tenant domain.
                            filteredArtifacts.add(artifact);
                        }

                    }
                }
                return filteredArtifacts;

            }
        } catch (RegistryException e) {
            handleException("Failed to get all publishers", e);
            return null;
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            handleException(e.getMessage(), e);
        }
        return null;

    }

    private boolean isAllowedTenantDomain(String allowedTenants, String inputTenantDomain) {
        if (allowedTenants != null) {
            String[] tenants = allowedTenants.split(",");
            if (allowedTenants.split(",").length > 1) {
                for (int i = 0; i < tenants.length; i++) {
                    if (tenants[i].equals(inputTenantDomain)) {
                        return true;
                    }
                }
                return false;
            } else {
                if (allowedTenants.equals(inputTenantDomain)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public void updateAccessAllowDomains(String accessToken, String[] accessAllowDomains)
            throws APIManagementException {
        apiMgtDAO.updateAccessAllowDomains(accessToken, accessAllowDomains);
    }

    /**
     * Returned an API set from a set of registry paths
     *
     * @param registry Registry object from which the APIs retrieving,
     * @param limit Specifies the number of APIs to add.
     * @param apiPaths Array of API paths.
     * @param isTenantMode Differentiates between Tenant mode and Public mode.
     * @param domainOfIteratedTenant Domain for the tenant being Iterated.
     * @param requestedTenantDomain Domain of the tenant being viewed.
     * @return Set<API> set of APIs
     * @throws RegistryException
     * @throws APIManagementException
     */
    private Set<API> getAPIs(Registry registry, int limit,String[] apiPaths, boolean isTenantMode, String domainOfIteratedTenant, String requestedTenantDomain)
            throws RegistryException, APIManagementException, org.wso2.carbon.user.api.UserStoreException {

        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());

        Boolean allowMultipleVersions = isAllowDisplayMultipleVersions();
        Boolean showAllAPIs = isAllowDisplayAPIsWithMultipleStatus();
        Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
        List<API> multiVersionedAPIs = new ArrayList<API>();
        Comparator<API> versionComparator = new APIVersionComparator();

        //Find UUID
        GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                APIConstants.API_KEY);
        for (int a = 0; a < apiPaths.length && limit > 0; a++) {
            Resource resource = registry.get(apiPaths[a]);
            if (resource != null) {
                GenericArtifact genericArtifact = artifactManager.getGenericArtifact(resource.getUUID());
                String visibility = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY);
                API api = null;
                String status = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                if(isArtifactViewable(genericArtifact,requestedTenantDomain, domainOfIteratedTenant)){
                    if (!showAllAPIs) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(genericArtifact, registry);
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(genericArtifact, registry);

                        }

                    }
                }
                if (api != null) {
                    String key;
                    limit--;
                    //Check the configuration to allow showing multiple versions of an API true/false
                    if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                        API existingAPI = latestPublishedAPIs.get(key);
                        if (existingAPI != null) {
                            // If we have already seen an API with the same name, make sure
                            // this one has a higher version number
                            if (versionComparator.compare(api, existingAPI) > 0) {
                                latestPublishedAPIs.put(key, api);
                            }
                        } else {
                            // We haven't seen this API before
                            latestPublishedAPIs.put(key, api);
                        }
                    } else { //If allow showing multiple versions of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                .getVersion();
                        multiVersionedAPIs.add(api);
                    }
                }

            }
        }
        if (!allowMultipleVersions) {
            for (API api : latestPublishedAPIs.values()) {
                apiSortedSet.add(api);
            }
            return apiSortedSet;
        } else {
            for (API api : multiVersionedAPIs) {
                apiVersionsSortedSet.add(api);
            }
            return apiVersionsSortedSet;

        }
    }

    private boolean isArtifactViewable(GenericArtifact artifact, String requestedTenantDomain, String iteratedTenantDomain) throws GovernanceException,UserStoreException{

        String visibility = artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY);
        boolean isPublicVisibility = visibility.equals(APIConstants.API_GLOBAL_VISIBILITY);
        boolean isControlledVisibility = visibility.equals(APIConstants.API_CONTROLLED_VISIBILITY);
        boolean isRestrictedVisibility = visibility.equals(APIConstants.API_RESTRICTED_VISIBILITY);
        String visibleTenantDomains = artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS);
        boolean isLoggedinTenantRequestingOwnAPIs = ((requestedTenantDomain != null) && (requestedTenantDomain.equals(iteratedTenantDomain))) &&
                requestedTenantDomain.equals(this.tenantDomain);
        boolean isUserLoggedIn = !((UserRegistry)this.registry).getUserName().equals(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME);
        boolean isAUserLoggedInSuperTenantMode = isUserLoggedIn && MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(this.tenantDomain);

        return ((this.isTenantModeStoreView &&
                ( isUserLoggedIn && ((isLoggedinTenantRequestingOwnAPIs && (isControlledVisibility ||
                        (isRestrictedVisibility && isLoggedinTenantSearchingOwnAPIs(artifact)))) ||
                        (this.tenantDomain.equals(requestedTenantDomain) && isControlledVisibility && isAllowedTenantDomain(visibleTenantDomains,requestedTenantDomain))) ||
                        !isUserLoggedIn && ((isControlledVisibility && isAllowedTenantDomain(visibleTenantDomains,requestedTenantDomain)))||
                        (requestedTenantDomain.equals(iteratedTenantDomain)) && !isRestrictedVisibility)) ||
                !this.isTenantModeStoreView && ((isPublicVisibility) ||
                        ((isUserLoggedIn && ((isAUserLoggedInSuperTenantMode && (isRestrictedVisibility &&iteratedTenantDomain.equals(this.tenantDomain) && isLoggedinTenantSearchingOwnAPIs(artifact)  )  ) )))) );
    }

    private boolean isLoggedinTenantSearchingOwnAPIs(GenericArtifact genericArtifact) throws GovernanceException,UserStoreException{
        String [] permittedRoles =  genericArtifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES) != null?
                genericArtifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES).split(","):null;
        String [] currentUserRoles = new String[0];
        try {
            //We need to user store manager of logged in tenant
            if(tenantId!=0){
            currentUserRoles = ((UserRegistry) ((UserAwareAPIConsumer) this).registry).
                    getUserRealm().getUserStoreManager().getRoleListOfUser(((UserRegistry)this.registry).getUserName());
            }
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
           log.error("cannot retrieve user role list for tenant" + tenantDomain);
        }
        if(permittedRoles != null && currentUserRoles != null){
            for(String permittedRole : permittedRoles){
                for(String currentUserRole : currentUserRoles){
                    if(currentUserRole.equals(permittedRole)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean loadTenantRegistry(int tenantId){
        TenantRegistryLoader tenantRegistryLoader = APIManagerComponent.getTenantRegistryLoader();

        if (!registryInitializedTenants.contains(tenantId)) {
            try {
                tenantRegistryLoader.loadTenantRegistry(tenantId);
                registryInitializedTenants.add(tenantId);
                return true;
            } catch (Exception e) {
                log.error("Error while loading registry of Tenant + " + tenantId + " " + e.getMessage());
            }
        }
        return false;
    }
}
