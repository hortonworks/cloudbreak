package com.cloudera.smartrouter.client.api;

import com.cloudera.smartrouter.client.ApiClient;
import com.cloudera.smartrouter.client.BaseApi;

import com.cloudera.smartrouter.client.model.ApiErrorResponse;
import com.cloudera.smartrouter.client.model.GuardrailListResponse;
import com.cloudera.smartrouter.client.model.KillSwitchResponse;
import com.cloudera.smartrouter.client.model.KillSwitchUpdateRequest;
import com.cloudera.smartrouter.client.model.RevisionListResponse;
import com.cloudera.smartrouter.client.model.RoutingGuardrail;

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


public class GuardrailsApi extends BaseApi {

    public GuardrailsApi() {
        super(new ApiClient());
    }

    public GuardrailsApi(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * 
     * 
     * <p><b>201</b> - Created
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>409</b> - Name or display name taken, reserved or a reserved word
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param routingGuardrail  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return RoutingGuardrail
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingGuardrail createGuardrail(RoutingGuardrail routingGuardrail, String xChangeComment) throws RestClientException {
        return createGuardrailWithHttpInfo(routingGuardrail, xChangeComment).getBody();
    }

    /**
     * 
     * 
     * <p><b>201</b> - Created
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>409</b> - Name or display name taken, reserved or a reserved word
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param routingGuardrail  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;RoutingGuardrail&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingGuardrail> createGuardrailWithHttpInfo(RoutingGuardrail routingGuardrail, String xChangeComment) throws RestClientException {
        Object localVarPostBody = routingGuardrail;
        
        // verify the required parameter 'routingGuardrail' is set
        if (routingGuardrail == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingGuardrail' when calling createGuardrail");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

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

        ParameterizedTypeReference<RoutingGuardrail> localReturnType = new ParameterizedTypeReference<RoutingGuardrail>() {};
        return apiClient.invokeAPI("/v1/policies/guardrails", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Intrinsic guardrails cannot be deleted (403).
     * <p><b>204</b> - Deleted
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deleteGuardrail(String name, String ifMatch, String xChangeComment) throws RestClientException {
        deleteGuardrailWithHttpInfo(name, ifMatch, xChangeComment);
    }

    /**
     * 
     * Intrinsic guardrails cannot be deleted (403).
     * <p><b>204</b> - Deleted
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteGuardrailWithHttpInfo(String name, String ifMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling deleteGuardrail");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling deleteGuardrail");
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
        return apiClient.invokeAPI("/v1/policies/guardrails/{name}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Guardrail
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @return RoutingGuardrail
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingGuardrail getGuardrail(String name) throws RestClientException {
        return getGuardrailWithHttpInfo(name).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Guardrail
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @return ResponseEntity&lt;RoutingGuardrail&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingGuardrail> getGuardrailWithHttpInfo(String name) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling getGuardrail");
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

        ParameterizedTypeReference<RoutingGuardrail> localReturnType = new ParameterizedTypeReference<RoutingGuardrail>() {};
        return apiClient.invokeAPI("/v1/policies/guardrails/{name}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Existing endpoint; response shape unchanged.
     * <p><b>200</b> - Kill-switch state
     * @return KillSwitchResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public KillSwitchResponse getKillSwitch() throws RestClientException {
        return getKillSwitchWithHttpInfo().getBody();
    }

    /**
     * 
     * Existing endpoint; response shape unchanged.
     * <p><b>200</b> - Kill-switch state
     * @return ResponseEntity&lt;KillSwitchResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<KillSwitchResponse> getKillSwitchWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<KillSwitchResponse> localReturnType = new ParameterizedTypeReference<KillSwitchResponse>() {};
        return apiClient.invokeAPI("/v1/config/killswitch", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Revision summaries
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param limit  (optional, default to 50)
     * @param cursor  (optional)
     * @return RevisionListResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RevisionListResponse listGuardrailRevisions(String name, Integer limit, String cursor) throws RestClientException {
        return listGuardrailRevisionsWithHttpInfo(name, limit, cursor).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Revision summaries
     * <p><b>404</b> - Absent or tombstoned
     * @param name  (required)
     * @param limit  (optional, default to 50)
     * @param cursor  (optional)
     * @return ResponseEntity&lt;RevisionListResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RevisionListResponse> listGuardrailRevisionsWithHttpInfo(String name, Integer limit, String cursor) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling listGuardrailRevisions");
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
        return apiClient.invokeAPI("/v1/policies/guardrails/{name}/revisions", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Intrinsic (read-only) and discretionary guardrails plus kill-switch state
     * @return GuardrailListResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public GuardrailListResponse listGuardrails() throws RestClientException {
        return listGuardrailsWithHttpInfo().getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Intrinsic (read-only) and discretionary guardrails plus kill-switch state
     * @return ResponseEntity&lt;GuardrailListResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<GuardrailListResponse> listGuardrailsWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<GuardrailListResponse> localReturnType = new ParameterizedTypeReference<GuardrailListResponse>() {};
        return apiClient.invokeAPI("/v1/policies/guardrails", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Updated
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param routingGuardrail  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return RoutingGuardrail
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RoutingGuardrail replaceGuardrail(String name, String ifMatch, RoutingGuardrail routingGuardrail, String xChangeComment) throws RestClientException {
        return replaceGuardrailWithHttpInfo(name, ifMatch, routingGuardrail, xChangeComment).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Updated
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param name  (required)
     * @param ifMatch Policy or guardrail version ETag, e.g. \&quot;v3\&quot;. Missing returns 428; stale returns 412; \&quot;*\&quot; returns 400. (required)
     * @param routingGuardrail  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;RoutingGuardrail&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RoutingGuardrail> replaceGuardrailWithHttpInfo(String name, String ifMatch, RoutingGuardrail routingGuardrail, String xChangeComment) throws RestClientException {
        Object localVarPostBody = routingGuardrail;
        
        // verify the required parameter 'name' is set
        if (name == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'name' when calling replaceGuardrail");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling replaceGuardrail");
        }
        
        // verify the required parameter 'routingGuardrail' is set
        if (routingGuardrail == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingGuardrail' when calling replaceGuardrail");
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

        ParameterizedTypeReference<RoutingGuardrail> localReturnType = new ParameterizedTypeReference<RoutingGuardrail>() {};
        return apiClient.invokeAPI("/v1/policies/guardrails/{name}", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Existing endpoint; shape unchanged. New: persisted, audited, role-gated (403); X-Actor ignored; If-Match optional.
     * <p><b>200</b> - Updated state
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param killSwitchUpdateRequest  (required)
     * @param ifMatch  (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return KillSwitchResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public KillSwitchResponse updateKillSwitch(KillSwitchUpdateRequest killSwitchUpdateRequest, String ifMatch, String xChangeComment) throws RestClientException {
        return updateKillSwitchWithHttpInfo(killSwitchUpdateRequest, ifMatch, xChangeComment).getBody();
    }

    /**
     * 
     * Existing endpoint; shape unchanged. New: persisted, audited, role-gated (403); X-Actor ignored; If-Match optional.
     * <p><b>200</b> - Updated state
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param killSwitchUpdateRequest  (required)
     * @param ifMatch  (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;KillSwitchResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<KillSwitchResponse> updateKillSwitchWithHttpInfo(KillSwitchUpdateRequest killSwitchUpdateRequest, String ifMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = killSwitchUpdateRequest;
        
        // verify the required parameter 'killSwitchUpdateRequest' is set
        if (killSwitchUpdateRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'killSwitchUpdateRequest' when calling updateKillSwitch");
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

        ParameterizedTypeReference<KillSwitchResponse> localReturnType = new ParameterizedTypeReference<KillSwitchResponse>() {};
        return apiClient.invokeAPI("/v1/config/killswitch", HttpMethod.PUT, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
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
