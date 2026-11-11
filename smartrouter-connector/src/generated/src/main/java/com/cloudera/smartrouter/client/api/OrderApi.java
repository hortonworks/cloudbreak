package com.cloudera.smartrouter.client.api;

import com.cloudera.smartrouter.client.ApiClient;
import com.cloudera.smartrouter.client.BaseApi;

import com.cloudera.smartrouter.client.model.ApiErrorResponse;
import com.cloudera.smartrouter.client.model.MoveRequest;
import com.cloudera.smartrouter.client.model.PrioritySeed;
import com.cloudera.smartrouter.client.model.RoutingPolicyOrder;

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


public class OrderApi extends BaseApi {

    public OrderApi() {
        super(new ApiClient());
    }

    public OrderApi(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * 
     * 
     * <p><b>200</b> - Live scoped policies in evaluation order (System default excluded)
     * @return RoutingPolicyOrder
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicyOrder getPolicyOrder() throws RestClientException {
        return getPolicyOrderWithHttpInfo().getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Live scoped policies in evaluation order (System default excluded)
     * @return ResponseEntity&lt;RoutingPolicyOrder&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicyOrder> getPolicyOrderWithHttpInfo() throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json", "application/yaml"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<RoutingPolicyOrder> localReturnType = new ParameterizedTypeReference<RoutingPolicyOrder>() {};
        return apiClient.invokeAPI("/v1/policies/order", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Suggested insertion slot for the Prioritize step
     * @param scopeType For a policy with several scopes, pass its most specific scope type (service, then queue, then global). (required)
     * @return PrioritySeed
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public PrioritySeed getPrioritySeed(String scopeType) throws RestClientException {
        return getPrioritySeedWithHttpInfo(scopeType).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Suggested insertion slot for the Prioritize step
     * @param scopeType For a policy with several scopes, pass its most specific scope type (service, then queue, then global). (required)
     * @return ResponseEntity&lt;PrioritySeed&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PrioritySeed> getPrioritySeedWithHttpInfo(String scopeType) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'scopeType' is set
        if (scopeType == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'scopeType' when calling getPrioritySeed");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "scopeType", scopeType));
        

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PrioritySeed> localReturnType = new ParameterizedTypeReference<PrioritySeed>() {};
        return apiClient.invokeAPI("/v1/policies/priority-seed", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Moved
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Order version ETag, e.g. \&quot;o17\&quot;. (required)
     * @param moveRequest  (required)
     * @return RoutingPolicyOrder
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicyOrder movePolicy(String name, String ifMatch, MoveRequest moveRequest) throws RestClientException {
        return movePolicyWithHttpInfo(name, ifMatch, moveRequest).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Moved
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Order version ETag, e.g. \&quot;o17\&quot;. (required)
     * @param moveRequest  (required)
     * @return ResponseEntity&lt;RoutingPolicyOrder&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicyOrder> movePolicyWithHttpInfo(String name, String ifMatch, MoveRequest moveRequest) throws RestClientException {
        Object localVarPostBody = moveRequest;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling movePolicy");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling movePolicy");
        }
        
        // verify the required parameter 'moveRequest' is set
        if (moveRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'moveRequest' when calling movePolicy");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        if (ifMatch != null)
        localVarHeaderParams.add("If-Match", apiClient.parameterToString(ifMatch));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<RoutingPolicyOrder> localReturnType = new ParameterizedTypeReference<RoutingPolicyOrder>() {};
        return apiClient.invokeAPI("/v1/policies/{name}:move", HttpMethod.POST, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Order replaced
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param ifMatch Order version ETag, e.g. \&quot;o17\&quot;. (required)
     * @param routingPolicyOrder  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return RoutingPolicyOrder
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicyOrder replacePolicyOrder(String ifMatch, RoutingPolicyOrder routingPolicyOrder, String xChangeComment) throws RestClientException {
        return replacePolicyOrderWithHttpInfo(ifMatch, routingPolicyOrder, xChangeComment).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Order replaced
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param ifMatch Order version ETag, e.g. \&quot;o17\&quot;. (required)
     * @param routingPolicyOrder  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;RoutingPolicyOrder&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicyOrder> replacePolicyOrderWithHttpInfo(String ifMatch, RoutingPolicyOrder routingPolicyOrder, String xChangeComment) throws RestClientException {
        Object localVarPostBody = routingPolicyOrder;
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling replacePolicyOrder");
        }
        
        // verify the required parameter 'routingPolicyOrder' is set
        if (routingPolicyOrder == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingPolicyOrder' when calling replacePolicyOrder");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        if (ifMatch != null)
        localVarHeaderParams.add("If-Match", apiClient.parameterToString(ifMatch));
        if (xChangeComment != null)
        localVarHeaderParams.add("X-Change-Comment", apiClient.parameterToString(xChangeComment));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<RoutingPolicyOrder> localReturnType = new ParameterizedTypeReference<RoutingPolicyOrder>() {};
        return apiClient.invokeAPI("/v1/policies/order", HttpMethod.PUT, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
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
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        return apiClient.invokeAPI(localVarPath, method, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, returnType);
    }
}
