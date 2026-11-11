package com.cloudera.smartrouter.client.api;

import com.cloudera.smartrouter.client.ApiClient;
import com.cloudera.smartrouter.client.BaseApi;

import com.cloudera.smartrouter.client.model.ApiErrorResponse;
import com.cloudera.smartrouter.client.model.DecisionInputSnapshotsResponse;
import com.cloudera.smartrouter.client.model.HealthResponse;
import com.cloudera.smartrouter.client.model.ReadinessResponse;
import com.cloudera.smartrouter.client.model.RoutingDecision;
import com.cloudera.smartrouter.client.model.RoutingRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


public class DefaultApi extends BaseApi {

    public DefaultApi() {
        super(new ApiClient());
    }

    public DefaultApi(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * Per-provider decision-input snapshots (diagnostics)
     * Runs each decision input provider through the bounded, cached collection path and reports its outcome.
     * <p><b>200</b> - Snapshot of each decision input provider
     * @param routingRequest  (required)
     * @return DecisionInputSnapshotsResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public DecisionInputSnapshotsResponse providerSnapshots(RoutingRequest routingRequest) throws RestClientException {
        return providerSnapshotsWithHttpInfo(routingRequest).getBody();
    }

    /**
     * Per-provider decision-input snapshots (diagnostics)
     * Runs each decision input provider through the bounded, cached collection path and reports its outcome.
     * <p><b>200</b> - Snapshot of each decision input provider
     * @param routingRequest  (required)
     * @return ResponseEntity&lt;DecisionInputSnapshotsResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<DecisionInputSnapshotsResponse> providerSnapshotsWithHttpInfo(RoutingRequest routingRequest) throws RestClientException {
        Object localVarPostBody = routingRequest;
        
        // verify the required parameter 'routingRequest' is set
        if (routingRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingRequest' when calling providerSnapshots");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<DecisionInputSnapshotsResponse> localReturnType = new ParameterizedTypeReference<DecisionInputSnapshotsResponse>() {};
        return apiClient.invokeAPI("/v1/provider-snapshots", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Resolve the routing decision for a workload request
     * 
     * <p><b>200</b> - Routing decision
     * <p><b>400</b> - Invalid request payload
     * @param routingRequest  (required)
     * @param dryRun When true, collect facts and evaluate policy but skip the decision-audit write. (optional, default to false)
     * @return RoutingDecision
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingDecision resolveRoutingDecision(RoutingRequest routingRequest, Boolean dryRun) throws RestClientException {
        return resolveRoutingDecisionWithHttpInfo(routingRequest, dryRun).getBody();
    }

    /**
     * Resolve the routing decision for a workload request
     * 
     * <p><b>200</b> - Routing decision
     * <p><b>400</b> - Invalid request payload
     * @param routingRequest  (required)
     * @param dryRun When true, collect facts and evaluate policy but skip the decision-audit write. (optional, default to false)
     * @return ResponseEntity&lt;RoutingDecision&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingDecision> resolveRoutingDecisionWithHttpInfo(RoutingRequest routingRequest, Boolean dryRun) throws RestClientException {
        Object localVarPostBody = routingRequest;
        
        // verify the required parameter 'routingRequest' is set
        if (routingRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingRequest' when calling resolveRoutingDecision");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "dryRun", dryRun));
        

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<RoutingDecision> localReturnType = new ParameterizedTypeReference<RoutingDecision>() {};
        return apiClient.invokeAPI("/v1/resolve", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Liveness health endpoint
     * 
     * <p><b>200</b> - Service is alive
     * @return HealthResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public HealthResponse serviceHealth() throws RestClientException {
        return serviceHealthWithHttpInfo().getBody();
    }

    /**
     * Liveness health endpoint
     * 
     * <p><b>200</b> - Service is alive
     * @return ResponseEntity&lt;HealthResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<HealthResponse> serviceHealthWithHttpInfo() throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<HealthResponse> localReturnType = new ParameterizedTypeReference<HealthResponse>() {};
        return apiClient.invokeAPI("/v1/health", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Readiness endpoint with dependency checks
     * 
     * <p><b>200</b> - Service is ready to accept traffic
     * <p><b>503</b> - Service is not ready due to a critical dependency outage
     * @return ReadinessResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ReadinessResponse serviceReadiness() throws RestClientException {
        return serviceReadinessWithHttpInfo().getBody();
    }

    /**
     * Readiness endpoint with dependency checks
     * 
     * <p><b>200</b> - Service is ready to accept traffic
     * <p><b>503</b> - Service is not ready due to a critical dependency outage
     * @return ResponseEntity&lt;ReadinessResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ReadinessResponse> serviceReadinessWithHttpInfo() throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ReadinessResponse> localReturnType = new ParameterizedTypeReference<ReadinessResponse>() {};
        return apiClient.invokeAPI("/v1/ready", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }

    @Override
    public <T> ResponseEntity<T> invokeAPI(String url, HttpMethod method, Object request, ParameterizedTypeReference<T> returnType) throws RestClientException {
        String localVarPath = url.replace(apiClient.getBasePath(), "");
        Object localVarPostBody = request;

        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        return apiClient.invokeAPI(localVarPath, method, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, returnType);
    }
}
