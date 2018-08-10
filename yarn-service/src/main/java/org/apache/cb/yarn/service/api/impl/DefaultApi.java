package org.apache.cb.yarn.service.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.GenericType;

import org.apache.cb.yarn.service.api.ApiClient;
import org.apache.cb.yarn.service.api.ApiException;
import org.apache.cb.yarn.service.api.ApiResponse;
import org.apache.cb.yarn.service.api.Configuration;
import org.apache.cb.yarn.service.api.Pair;
import org.apache.cb.yarn.service.api.records.Component;
import org.apache.cb.yarn.service.api.records.Service;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-08-08T16:59:40.572+02:00")
public class DefaultApi {
    private ApiClient apiClient;

    public DefaultApi() {
        this(Configuration.getDefaultApiClient());
    }

    public DefaultApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * (TBD) List of services running in the cluster.
     * Get a list of all currently running services (response includes a minimal projection of the service info). For more details do a GET on a specific service name.
     *
     * @return List&lt;Service&gt;
     * @throws ApiException if fails to make API call
     */
    public List<Service> appV1ServicesGet() throws ApiException {
        return appV1ServicesGetWithHttpInfo().getData();
    }

    /**
     * (TBD) List of services running in the cluster.
     * Get a list of all currently running services (response includes a minimal projection of the service info). For more details do a GET on a specific service name.
     *
     * @return ApiResponse&lt;List&lt;Service&gt;&gt;
     * @throws ApiException if fails to make API call
     */
    public ApiResponse<List<Service>> appV1ServicesGetWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/app/v1/services";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        GenericType<List<Service>> localVarReturnType = new GenericType<List<Service>>() {
        };
        return apiClient.invokeAPI(localVarPath, "GET", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Create a service
     * Create a service. The request JSON is a service object with details required for creation. If the request is successful it returns 202 Accepted. A success of this API only confirms success in submission of the service creation request. There is no guarantee that the service will actually reach a RUNNING state. Resource availability and several other factors determines if the service will be deployed in the cluster. It is expected that clients would subsequently call the GET API to get details of the service and determine its state.
     *
     * @param service Service request object (required)
     * @throws ApiException if fails to make API call
     */
    public void appV1ServicesPost(Service service) throws ApiException {

        appV1ServicesPostWithHttpInfo(service);
    }

    /**
     * Create a service
     * Create a service. The request JSON is a service object with details required for creation. If the request is successful it returns 202 Accepted. A success of this API only confirms success in submission of the service creation request. There is no guarantee that the service will actually reach a RUNNING state. Resource availability and several other factors determines if the service will be deployed in the cluster. It is expected that clients would subsequently call the GET API to get details of the service and determine its state.
     *
     * @param service Service request object (required)
     * @throws ApiException if fails to make API call
     */
    public ApiResponse<Void> appV1ServicesPostWithHttpInfo(Service service) throws ApiException {
        Object localVarPostBody = service;

        // verify the required parameter 'service' is set
        if (service == null) {
            throw new ApiException(400, "Missing the required parameter 'service' when calling appV1ServicesPost");
        }

        // create path and map variables
        String localVarPath = "/app/v1/services";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};


        return apiClient.invokeAPI(localVarPath, "POST", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, null);
    }

    /**
     * Flex a component&#39;s number of instances.
     * Set a component&#39;s desired number of instanes
     *
     * @param serviceName   Service name (required)
     * @param componentName Component name (required)
     * @param component     The definition of a component which contains the updated number of instances. (required)
     * @throws ApiException if fails to make API call
     */
    public void appV1ServicesServiceNameComponentsComponentNamePut(String serviceName, String componentName, Component component) throws ApiException {

        appV1ServicesServiceNameComponentsComponentNamePutWithHttpInfo(serviceName, componentName, component);
    }

    /**
     * Flex a component&#39;s number of instances.
     * Set a component&#39;s desired number of instanes
     *
     * @param serviceName   Service name (required)
     * @param componentName Component name (required)
     * @param component     The definition of a component which contains the updated number of instances. (required)
     * @throws ApiException if fails to make API call
     */
    public ApiResponse<Void> appV1ServicesServiceNameComponentsComponentNamePutWithHttpInfo(String serviceName, String componentName, Component component) throws ApiException {
        Object localVarPostBody = component;

        // verify the required parameter 'serviceName' is set
        if (serviceName == null) {
            throw new ApiException(400, "Missing the required parameter 'serviceName' when calling appV1ServicesServiceNameComponentsComponentNamePut");
        }

        // verify the required parameter 'componentName' is set
        if (componentName == null) {
            throw new ApiException(400, "Missing the required parameter 'componentName' when calling appV1ServicesServiceNameComponentsComponentNamePut");
        }

        // verify the required parameter 'component' is set
        if (component == null) {
            throw new ApiException(400, "Missing the required parameter 'component' when calling appV1ServicesServiceNameComponentsComponentNamePut");
        }

        // create path and map variables
        String localVarPath = "/app/v1/services/{service_name}/components/{component_name}"
                .replaceAll("\\{" + "service_name" + "\\}", apiClient.escapeString(serviceName.toString()))
                .replaceAll("\\{" + "component_name" + "\\}", apiClient.escapeString(componentName.toString()));

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};


        return apiClient.invokeAPI(localVarPath, "PUT", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, null);
    }

    /**
     * Destroy a service
     * Destroy a service and release all resources. This API might have to return JSON data providing location of logs (TBD), etc.
     *
     * @param serviceName Service name (required)
     * @throws ApiException if fails to make API call
     */
    public void appV1ServicesServiceNameDelete(String serviceName) throws ApiException {

        appV1ServicesServiceNameDeleteWithHttpInfo(serviceName);
    }

    /**
     * Destroy a service
     * Destroy a service and release all resources. This API might have to return JSON data providing location of logs (TBD), etc.
     *
     * @param serviceName Service name (required)
     * @throws ApiException if fails to make API call
     */
    public ApiResponse<Void> appV1ServicesServiceNameDeleteWithHttpInfo(String serviceName) throws ApiException {
        Object localVarPostBody = null;

        // verify the required parameter 'serviceName' is set
        if (serviceName == null) {
            throw new ApiException(400, "Missing the required parameter 'serviceName' when calling appV1ServicesServiceNameDelete");
        }

        // create path and map variables
        String localVarPath = "/app/v1/services/{service_name}"
                .replaceAll("\\{" + "service_name" + "\\}", apiClient.escapeString(serviceName.toString()));

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};


        return apiClient.invokeAPI(localVarPath, "DELETE", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, null);
    }

    /**
     * Get details of a service.
     * Return the details (including containers) of a running service
     *
     * @param serviceName Service name (required)
     * @return Service
     * @throws ApiException if fails to make API call
     */
    public Service appV1ServicesServiceNameGet(String serviceName) throws ApiException {
        return appV1ServicesServiceNameGetWithHttpInfo(serviceName).getData();
    }

    /**
     * Get details of a service.
     * Return the details (including containers) of a running service
     *
     * @param serviceName Service name (required)
     * @return ApiResponse&lt;Service&gt;
     * @throws ApiException if fails to make API call
     */
    public ApiResponse<Service> appV1ServicesServiceNameGetWithHttpInfo(String serviceName) throws ApiException {
        Object localVarPostBody = null;

        // verify the required parameter 'serviceName' is set
        if (serviceName == null) {
            throw new ApiException(400, "Missing the required parameter 'serviceName' when calling appV1ServicesServiceNameGet");
        }

        // create path and map variables
        String localVarPath = "/app/v1/services/{service_name}"
                .replaceAll("\\{" + "service_name" + "\\}", apiClient.escapeString(serviceName.toString()));

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};

        GenericType<Service> localVarReturnType = new GenericType<Service>() {
        };
        return apiClient.invokeAPI(localVarPath, "GET", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Update a service or upgrade the binary version of the components of a running service
     * Update the runtime properties of a service. Currently the following operations are supported - update lifetime, stop/start a service. The PUT operation is also used to orchestrate an upgrade of the service containers to a newer version of their artifacts (TBD).
     *
     * @param serviceName Service name (required)
     * @param service     The updated service definition. It can contain the updated lifetime of a service or the desired state (STOPPED/STARTED) of a service to initiate a start/stop operation against the specified service (required)
     * @throws ApiException if fails to make API call
     */
    public void appV1ServicesServiceNamePut(String serviceName, Service service) throws ApiException {

        appV1ServicesServiceNamePutWithHttpInfo(serviceName, service);
    }

    /**
     * Update a service or upgrade the binary version of the components of a running service
     * Update the runtime properties of a service. Currently the following operations are supported - update lifetime, stop/start a service. The PUT operation is also used to orchestrate an upgrade of the service containers to a newer version of their artifacts (TBD).
     *
     * @param serviceName Service name (required)
     * @param service     The updated service definition. It can contain the updated lifetime of a service or the desired state (STOPPED/STARTED) of a service to initiate a start/stop operation against the specified service (required)
     * @throws ApiException if fails to make API call
     */
    public ApiResponse<Void> appV1ServicesServiceNamePutWithHttpInfo(String serviceName, Service service) throws ApiException {
        Object localVarPostBody = service;

        // verify the required parameter 'serviceName' is set
        if (serviceName == null) {
            throw new ApiException(400, "Missing the required parameter 'serviceName' when calling appV1ServicesServiceNamePut");
        }

        // verify the required parameter 'service' is set
        if (service == null) {
            throw new ApiException(400, "Missing the required parameter 'service' when calling appV1ServicesServiceNamePut");
        }

        // create path and map variables
        String localVarPath = "/app/v1/services/{service_name}"
                .replaceAll("\\{" + "service_name" + "\\}", apiClient.escapeString(serviceName.toString()));

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};


        return apiClient.invokeAPI(localVarPath, "PUT", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, null);
    }

    /**
     * Get current version of the API server.
     * Get current version of the API server.
     *
     * @throws ApiException if fails to make API call
     */
    public void appV1ServicesVersionGet() throws ApiException {

        appV1ServicesVersionGetWithHttpInfo();
    }

    /**
     * Get current version of the API server.
     * Get current version of the API server.
     *
     * @throws ApiException if fails to make API call
     */
    public ApiResponse<Void> appV1ServicesVersionGetWithHttpInfo() throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/app/v1/services/version";

        // query params
        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();


        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[]{};


        return apiClient.invokeAPI(localVarPath, "GET", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, null);
    }
}
