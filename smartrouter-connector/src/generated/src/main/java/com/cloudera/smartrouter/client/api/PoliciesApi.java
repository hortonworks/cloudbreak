package com.cloudera.smartrouter.client.api;

import com.cloudera.smartrouter.client.ApiClient;
import com.cloudera.smartrouter.client.BaseApi;

import com.cloudera.smartrouter.client.model.ApiErrorResponse;
import com.cloudera.smartrouter.client.model.BulkDeleteRequest;
import com.cloudera.smartrouter.client.model.BulkDeleteResponse;
import com.cloudera.smartrouter.client.model.CloneRequest;
import com.cloudera.smartrouter.client.model.IdentityAvailability;
import com.cloudera.smartrouter.client.model.PolicyDiffResponse;
import com.cloudera.smartrouter.client.model.PolicyListResponse;
import com.cloudera.smartrouter.client.model.PolicyPatch;
import com.cloudera.smartrouter.client.model.PolicyPhase;
import com.cloudera.smartrouter.client.model.RevisionListResponse;
import com.cloudera.smartrouter.client.model.RollbackRequest;
import com.cloudera.smartrouter.client.model.RoutingPolicy;

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


public class PoliciesApi extends BaseApi {

    public PoliciesApi() {
        super(new ApiClient());
    }

    public PoliciesApi(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * 
     * Atomic; any version or order conflict aborts the whole request.
     * <p><b>200</b> - All deleted
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param bulkDeleteRequest  (required)
     * @return BulkDeleteResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public BulkDeleteResponse bulkDeletePolicies(BulkDeleteRequest bulkDeleteRequest) throws RestClientException {
        return bulkDeletePoliciesWithHttpInfo(bulkDeleteRequest).getBody();
    }

    /**
     * 
     * Atomic; any version or order conflict aborts the whole request.
     * <p><b>200</b> - All deleted
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param bulkDeleteRequest  (required)
     * @return ResponseEntity&lt;BulkDeleteResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<BulkDeleteResponse> bulkDeletePoliciesWithHttpInfo(BulkDeleteRequest bulkDeleteRequest) throws RestClientException {
        Object localVarPostBody = bulkDeleteRequest;
        
        // verify the required parameter 'bulkDeleteRequest' is set
        if (bulkDeleteRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'bulkDeleteRequest' when calling bulkDeletePolicies");
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

        ParameterizedTypeReference<BulkDeleteResponse> localReturnType = new ParameterizedTypeReference<BulkDeleteResponse>() {};
        return apiClient.invokeAPI("/v1/policies:bulk-delete", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Advisory only; create remains the atomic uniqueness authority.
     * <p><b>200</b> - Availability
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * @param name  (optional)
     * @param displayName  (optional)
     * @return IdentityAvailability
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public IdentityAvailability checkIdentityAvailability(String name, String displayName) throws RestClientException {
        return checkIdentityAvailabilityWithHttpInfo(name, displayName).getBody();
    }

    /**
     * 
     * Advisory only; create remains the atomic uniqueness authority.
     * <p><b>200</b> - Availability
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * @param name  (optional)
     * @param displayName  (optional)
     * @return ResponseEntity&lt;IdentityAvailability&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<IdentityAvailability> checkIdentityAvailabilityWithHttpInfo(String name, String displayName) throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "name", name));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "displayName", displayName));
        

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<IdentityAvailability> localReturnType = new ParameterizedTypeReference<IdentityAvailability>() {};
        return apiClient.invokeAPI("/v1/policies/identity-availability", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Creates a disabled clone; a scoped clone is inserted directly below its source.
     * <p><b>201</b> - Clone created
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>409</b> - Name or display name taken, reserved or a reserved word
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param cloneRequest  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @return RoutingPolicy
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicy clonePolicy(String name, CloneRequest cloneRequest, String ifPolicyOrderMatch) throws RestClientException {
        return clonePolicyWithHttpInfo(name, cloneRequest, ifPolicyOrderMatch).getBody();
    }

    /**
     * 
     * Creates a disabled clone; a scoped clone is inserted directly below its source.
     * <p><b>201</b> - Clone created
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>409</b> - Name or display name taken, reserved or a reserved word
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param cloneRequest  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @return ResponseEntity&lt;RoutingPolicy&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicy> clonePolicyWithHttpInfo(String name, CloneRequest cloneRequest, String ifPolicyOrderMatch) throws RestClientException {
        Object localVarPostBody = cloneRequest;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling clonePolicy");
        }
        
        // verify the required parameter 'cloneRequest' is set
        if (cloneRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'cloneRequest' when calling clonePolicy");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        if (ifPolicyOrderMatch != null)
        localVarHeaderParams.add("If-Policy-Order-Match", apiClient.parameterToString(ifPolicyOrderMatch));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<RoutingPolicy> localReturnType = new ParameterizedTypeReference<RoutingPolicy>() {};
        return apiClient.invokeAPI("/v1/policies/{name}:clone", HttpMethod.POST, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Create a policy (final wizard action)
     * 
     * <p><b>201</b> - Created. metadata.name is authoritative when it was generated.
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>409</b> - Name or display name taken, reserved or a reserved word
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>415</b> - Media type not accepted by this endpoint
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param routingPolicy  (required)
     * @param anchor  (optional)
     * @param position  (optional)
     * @param ifMatch Order version ETag; required (428) when anchor is present. (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return RoutingPolicy
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicy createPolicy(RoutingPolicy routingPolicy, String anchor, String position, String ifMatch, String xChangeComment) throws RestClientException {
        return createPolicyWithHttpInfo(routingPolicy, anchor, position, ifMatch, xChangeComment).getBody();
    }

    /**
     * Create a policy (final wizard action)
     * 
     * <p><b>201</b> - Created. metadata.name is authoritative when it was generated.
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>409</b> - Name or display name taken, reserved or a reserved word
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>415</b> - Media type not accepted by this endpoint
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param routingPolicy  (required)
     * @param anchor  (optional)
     * @param position  (optional)
     * @param ifMatch Order version ETag; required (428) when anchor is present. (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;RoutingPolicy&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicy> createPolicyWithHttpInfo(RoutingPolicy routingPolicy, String anchor, String position, String ifMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = routingPolicy;
        
        // verify the required parameter 'routingPolicy' is set
        if (routingPolicy == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingPolicy' when calling createPolicy");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "anchor", anchor));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "position", position));
        

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

        ParameterizedTypeReference<RoutingPolicy> localReturnType = new ParameterizedTypeReference<RoutingPolicy>() {};
        return apiClient.invokeAPI("/v1/policies", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Soft delete; name and display names stay reserved; order row removed in the same transaction.
     * <p><b>204</b> - Deleted
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deletePolicy(String name, String ifMatch, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        deletePolicyWithHttpInfo(name, ifMatch, ifPolicyOrderMatch, xChangeComment);
    }

    /**
     * 
     * Soft delete; name and display names stay reserved; order row removed in the same transaction.
     * <p><b>204</b> - Deleted
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deletePolicyWithHttpInfo(String name, String ifMatch, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling deletePolicy");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling deletePolicy");
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
        if (ifPolicyOrderMatch != null)
        localVarHeaderParams.add("If-Policy-Order-Match", apiClient.parameterToString(ifPolicyOrderMatch));
        if (xChangeComment != null)
        localVarHeaderParams.add("X-Change-Comment", apiClient.parameterToString(xChangeComment));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/v1/policies/{name}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Rule-ID-aware semantic diff
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param from  (required)
     * @param to  (required)
     * @return PolicyDiffResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public PolicyDiffResponse diffPolicyRevisions(String name, Integer from, Integer to) throws RestClientException {
        return diffPolicyRevisionsWithHttpInfo(name, from, to).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Rule-ID-aware semantic diff
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param from  (required)
     * @param to  (required)
     * @return ResponseEntity&lt;PolicyDiffResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PolicyDiffResponse> diffPolicyRevisionsWithHttpInfo(String name, Integer from, Integer to) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling diffPolicyRevisions");
        }
        
        // verify the required parameter 'from' is set
        if (from == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'from' when calling diffPolicyRevisions");
        }
        
        // verify the required parameter 'to' is set
        if (to == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'to' when calling diffPolicyRevisions");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "from", from));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "to", to));
        

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PolicyDiffResponse> localReturnType = new ParameterizedTypeReference<PolicyDiffResponse>() {};
        return apiClient.invokeAPI("/v1/policies/{name}/diff", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Policy document
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @return RoutingPolicy
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicy getPolicy(String name) throws RestClientException {
        return getPolicyWithHttpInfo(name).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Policy document
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @return ResponseEntity&lt;RoutingPolicy&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicy> getPolicyWithHttpInfo(String name) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling getPolicy");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("name", name);

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

        ParameterizedTypeReference<RoutingPolicy> localReturnType = new ParameterizedTypeReference<RoutingPolicy>() {};
        return apiClient.invokeAPI("/v1/policies/{name}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Historical document
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param version  (required)
     * @return RoutingPolicy
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicy getPolicyRevision(String name, Integer version) throws RestClientException {
        return getPolicyRevisionWithHttpInfo(name, version).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Historical document
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param version  (required)
     * @return ResponseEntity&lt;RoutingPolicy&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicy> getPolicyRevisionWithHttpInfo(String name, Integer version) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling getPolicyRevision");
        }
        
        // verify the required parameter 'version' is set
        if (version == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'version' when calling getPolicyRevision");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("name", name);
        uriVariables.put("version", version);

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

        ParameterizedTypeReference<RoutingPolicy> localReturnType = new ParameterizedTypeReference<RoutingPolicy>() {};
        return apiClient.invokeAPI("/v1/policies/{name}/revisions/{version}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * List policies with derived priority and pinned system layers
     * Filters combine with AND; a multi-valued filter matches any listed value. They also apply to the System default layer (systemLayers.bottom), which is returned only when it matches every given filter: it is Active, has a global scope and the objective default, and q and name see only its name (system-default) and display name (System default). With no filter it is always returned. limit, cursor, total and nextCursor count items only; systemLayers.bottom is never counted or paged. The application/yaml export contains policies only, never system layers.
     * <p><b>200</b> - Policy page. With Accept application/yaml, multi-document export (max 500, status omitted).
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE; the durable policy store is unreachable
     * @param q Case-insensitive substring of metadata.name, metadata.displayName, metadata.description, or any rule&#39;s reason or when expression. (optional)
     * @param phase  (optional)
     * @param objective A preset id matches policies with that preset; custom matches custom objectives; none matches policies without an objective. default is the objective of the System default layer: it matches no policy and keeps systemLayers.bottom, which any objective filter without default omits. default cannot be set on a policy. (optional)
     * @param scopeType Matches a policy when any of its scopes has a listed type; none matches policies without scopes. (optional)
     * @param name  (optional)
     * @param limit  (optional, default to 50)
     * @param cursor  (optional)
     * @return PolicyListResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public PolicyListResponse listPolicies(String q, List<PolicyPhase> phase, List<String> objective, List<String> scopeType, List<String> name, Integer limit, String cursor) throws RestClientException {
        return listPoliciesWithHttpInfo(q, phase, objective, scopeType, name, limit, cursor).getBody();
    }

    /**
     * List policies with derived priority and pinned system layers
     * Filters combine with AND; a multi-valued filter matches any listed value. They also apply to the System default layer (systemLayers.bottom), which is returned only when it matches every given filter: it is Active, has a global scope and the objective default, and q and name see only its name (system-default) and display name (System default). With no filter it is always returned. limit, cursor, total and nextCursor count items only; systemLayers.bottom is never counted or paged. The application/yaml export contains policies only, never system layers.
     * <p><b>200</b> - Policy page. With Accept application/yaml, multi-document export (max 500, status omitted).
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE; the durable policy store is unreachable
     * @param q Case-insensitive substring of metadata.name, metadata.displayName, metadata.description, or any rule&#39;s reason or when expression. (optional)
     * @param phase  (optional)
     * @param objective A preset id matches policies with that preset; custom matches custom objectives; none matches policies without an objective. default is the objective of the System default layer: it matches no policy and keeps systemLayers.bottom, which any objective filter without default omits. default cannot be set on a policy. (optional)
     * @param scopeType Matches a policy when any of its scopes has a listed type; none matches policies without scopes. (optional)
     * @param name  (optional)
     * @param limit  (optional, default to 50)
     * @param cursor  (optional)
     * @return ResponseEntity&lt;PolicyListResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PolicyListResponse> listPoliciesWithHttpInfo(String q, List<PolicyPhase> phase, List<String> objective, List<String> scopeType, List<String> name, Integer limit, String cursor) throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "q", q));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "phase", phase));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "objective", objective));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "scopeType", scopeType));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "name", name));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "cursor", cursor));
        

        final String[] localVarAccepts = { 
            "application/json", "application/yaml"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<PolicyListResponse> localReturnType = new ParameterizedTypeReference<PolicyListResponse>() {};
        return apiClient.invokeAPI("/v1/policies", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Revision summaries, newest first
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param limit  (optional, default to 50)
     * @param cursor  (optional)
     * @return RevisionListResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RevisionListResponse listPolicyRevisions(String name, Integer limit, String cursor) throws RestClientException {
        return listPolicyRevisionsWithHttpInfo(name, limit, cursor).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Revision summaries, newest first
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param limit  (optional, default to 50)
     * @param cursor  (optional)
     * @return ResponseEntity&lt;RevisionListResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RevisionListResponse> listPolicyRevisionsWithHttpInfo(String name, Integer limit, String cursor) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling listPolicyRevisions");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("name", name);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "limit", limit));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "cursor", cursor));
        

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<RevisionListResponse> localReturnType = new ParameterizedTypeReference<RevisionListResponse>() {};
        return apiClient.invokeAPI("/v1/policies/{name}/revisions", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Merge patch limited to metadata.displayName, metadata.description, spec.enabled, spec.scopes, spec.targets. Arrays (spec.scopes, spec.targets) are replaced as a whole.
     * <p><b>200</b> - Patched
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param policyPatch  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return RoutingPolicy
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicy patchPolicy(String name, String ifMatch, PolicyPatch policyPatch, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        return patchPolicyWithHttpInfo(name, ifMatch, policyPatch, ifPolicyOrderMatch, xChangeComment).getBody();
    }

    /**
     * 
     * Merge patch limited to metadata.displayName, metadata.description, spec.enabled, spec.scopes, spec.targets. Arrays (spec.scopes, spec.targets) are replaced as a whole.
     * <p><b>200</b> - Patched
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param policyPatch  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;RoutingPolicy&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicy> patchPolicyWithHttpInfo(String name, String ifMatch, PolicyPatch policyPatch, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = policyPatch;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling patchPolicy");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling patchPolicy");
        }
        
        // verify the required parameter 'policyPatch' is set
        if (policyPatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'policyPatch' when calling patchPolicy");
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
        if (ifPolicyOrderMatch != null)
        localVarHeaderParams.add("If-Policy-Order-Match", apiClient.parameterToString(ifPolicyOrderMatch));
        if (xChangeComment != null)
        localVarHeaderParams.add("X-Change-Comment", apiClient.parameterToString(xChangeComment));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/merge-patch+json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<RoutingPolicy> localReturnType = new ParameterizedTypeReference<RoutingPolicy>() {};
        return apiClient.invokeAPI("/v1/policies/{name}", HttpMethod.PATCH, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Updated
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param routingPolicy  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return RoutingPolicy
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicy replacePolicy(String name, String ifMatch, RoutingPolicy routingPolicy, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        return replacePolicyWithHttpInfo(name, ifMatch, routingPolicy, ifPolicyOrderMatch, xChangeComment).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Updated
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param routingPolicy  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;RoutingPolicy&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicy> replacePolicyWithHttpInfo(String name, String ifMatch, RoutingPolicy routingPolicy, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = routingPolicy;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling replacePolicy");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling replacePolicy");
        }
        
        // verify the required parameter 'routingPolicy' is set
        if (routingPolicy == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingPolicy' when calling replacePolicy");
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
        if (ifPolicyOrderMatch != null)
        localVarHeaderParams.add("If-Policy-Order-Match", apiClient.parameterToString(ifPolicyOrderMatch));
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

        ParameterizedTypeReference<RoutingPolicy> localReturnType = new ParameterizedTypeReference<RoutingPolicy>() {};
        return apiClient.invokeAPI("/v1/policies/{name}", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Forward-only; re-validated against the current catalog; creates a new version.
     * <p><b>200</b> - Rolled back
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param rollbackRequest  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return RoutingPolicy
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingPolicy rollbackPolicy(String name, String ifMatch, RollbackRequest rollbackRequest, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        return rollbackPolicyWithHttpInfo(name, ifMatch, rollbackRequest, ifPolicyOrderMatch, xChangeComment).getBody();
    }

    /**
     * 
     * Forward-only; re-validated against the current catalog; creates a new version.
     * <p><b>200</b> - Rolled back
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param rollbackRequest  (required)
     * @param ifPolicyOrderMatch Order version ETag; required (428) when the request changes order membership (ADR-001 v3 §6.2). (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;RoutingPolicy&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingPolicy> rollbackPolicyWithHttpInfo(String name, String ifMatch, RollbackRequest rollbackRequest, String ifPolicyOrderMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = rollbackRequest;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling rollbackPolicy");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling rollbackPolicy");
        }
        
        // verify the required parameter 'rollbackRequest' is set
        if (rollbackRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'rollbackRequest' when calling rollbackPolicy");
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
        if (ifPolicyOrderMatch != null)
        localVarHeaderParams.add("If-Policy-Order-Match", apiClient.parameterToString(ifPolicyOrderMatch));
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

        ParameterizedTypeReference<RoutingPolicy> localReturnType = new ParameterizedTypeReference<RoutingPolicy>() {};
        return apiClient.invokeAPI("/v1/policies/{name}:rollback", HttpMethod.POST, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
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
