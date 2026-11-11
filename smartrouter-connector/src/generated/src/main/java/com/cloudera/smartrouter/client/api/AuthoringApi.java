package com.cloudera.smartrouter.client.api;

import com.cloudera.smartrouter.client.ApiClient;
import com.cloudera.smartrouter.client.BaseApi;

import com.cloudera.smartrouter.client.model.ApiErrorResponse;
import com.cloudera.smartrouter.client.model.ApplyResponse;
import com.cloudera.smartrouter.client.model.FieldCatalog;
import com.cloudera.smartrouter.client.model.MaterializeObjectiveRequest;
import com.cloudera.smartrouter.client.model.MaterializeObjectiveResponse;
import com.cloudera.smartrouter.client.model.PresetsResponse;
import com.cloudera.smartrouter.client.model.RoutingPolicy;
import com.cloudera.smartrouter.client.model.SimulationRequest;
import com.cloudera.smartrouter.client.model.SimulationResponse;
import com.cloudera.smartrouter.client.model.ValidationReport;

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


public class AuthoringApi extends BaseApi {

    public AuthoringApi() {
        super(new ApiClient());
    }

    public AuthoringApi(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * 
     * Atomic multi-document apply of RoutingPolicy, RoutingGuardrail and at most one RoutingPolicyOrder.
     * <p><b>200</b> - Plan (dryRun) or committed result
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>413</b> - More than 256 KiB or 100 documents
     * <p><b>415</b> - Media type not accepted by this endpoint
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param body  (required)
     * @param dryRun  (optional, default to false)
     * @param planHash When present, refuse with 412 if the computed plan differs. (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ApplyResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ApplyResponse applyPolicies(String body, Boolean dryRun, String planHash, String xChangeComment) throws RestClientException {
        return applyPoliciesWithHttpInfo(body, dryRun, planHash, xChangeComment).getBody();
    }

    /**
     * 
     * Atomic multi-document apply of RoutingPolicy, RoutingGuardrail and at most one RoutingPolicyOrder.
     * <p><b>200</b> - Plan (dryRun) or committed result
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale policy, order or plan precondition
     * <p><b>413</b> - More than 256 KiB or 100 documents
     * <p><b>415</b> - Media type not accepted by this endpoint
     * <p><b>422</b> - POLICY_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param body  (required)
     * @param dryRun  (optional, default to false)
     * @param planHash When present, refuse with 412 if the computed plan differs. (optional)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;ApplyResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ApplyResponse> applyPoliciesWithHttpInfo(String body, Boolean dryRun, String planHash, String xChangeComment) throws RestClientException {
        Object localVarPostBody = body;
        
        // verify the required parameter 'body' is set
        if (body == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'body' when calling applyPolicies");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "dryRun", dryRun));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "planHash", planHash));
        

        if (xChangeComment != null)
        localVarHeaderParams.add("X-Change-Comment", apiClient.parameterToString(xChangeComment));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/yaml"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ApplyResponse> localReturnType = new ParameterizedTypeReference<ApplyResponse>() {};
        return apiClient.invokeAPI("/v1/policies:apply", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Versioned CEL field catalog
     * @return FieldCatalog
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public FieldCatalog getFieldCatalog() throws RestClientException {
        return getFieldCatalogWithHttpInfo().getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Versioned CEL field catalog
     * @return ResponseEntity&lt;FieldCatalog&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FieldCatalog> getFieldCatalogWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<FieldCatalog> localReturnType = new ParameterizedTypeReference<FieldCatalog>() {};
        return apiClient.invokeAPI("/v1/policies/catalog/fields", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Versioned presets
     * @return PresetsResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public PresetsResponse getPresets() throws RestClientException {
        return getPresetsWithHttpInfo().getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Versioned presets
     * @return ResponseEntity&lt;PresetsResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PresetsResponse> getPresetsWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<PresetsResponse> localReturnType = new ParameterizedTypeReference<PresetsResponse>() {};
        return apiClient.invokeAPI("/v1/policies/presets", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Deterministic generated rules
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * @param materializeObjectiveRequest  (required)
     * @return MaterializeObjectiveResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public MaterializeObjectiveResponse materializeObjective(MaterializeObjectiveRequest materializeObjectiveRequest) throws RestClientException {
        return materializeObjectiveWithHttpInfo(materializeObjectiveRequest).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Deterministic generated rules
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * @param materializeObjectiveRequest  (required)
     * @return ResponseEntity&lt;MaterializeObjectiveResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<MaterializeObjectiveResponse> materializeObjectiveWithHttpInfo(MaterializeObjectiveRequest materializeObjectiveRequest) throws RestClientException {
        Object localVarPostBody = materializeObjectiveRequest;
        
        // verify the required parameter 'materializeObjectiveRequest' is set
        if (materializeObjectiveRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'materializeObjectiveRequest' when calling materializeObjective");
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

        ParameterizedTypeReference<MaterializeObjectiveResponse> localReturnType = new ParameterizedTypeReference<MaterializeObjectiveResponse>() {};
        return apiClient.invokeAPI("/v1/policies:materialize-objective", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Side-effect free; no providers, cache writes, EXPLAIN or audit.
     * <p><b>200</b> - Outcome and ordered trace
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>422</b> - SIMULATION_LIMIT
     * @param simulationRequest  (required)
     * @return SimulationResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public SimulationResponse simulatePolicy(SimulationRequest simulationRequest) throws RestClientException {
        return simulatePolicyWithHttpInfo(simulationRequest).getBody();
    }

    /**
     * 
     * Side-effect free; no providers, cache writes, EXPLAIN or audit.
     * <p><b>200</b> - Outcome and ordered trace
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>422</b> - SIMULATION_LIMIT
     * @param simulationRequest  (required)
     * @return ResponseEntity&lt;SimulationResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SimulationResponse> simulatePolicyWithHttpInfo(SimulationRequest simulationRequest) throws RestClientException {
        Object localVarPostBody = simulationRequest;
        
        // verify the required parameter 'simulationRequest' is set
        if (simulationRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'simulationRequest' when calling simulatePolicy");
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
            "application/json", "application/yaml"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<SimulationResponse> localReturnType = new ParameterizedTypeReference<SimulationResponse>() {};
        return apiClient.invokeAPI("/v1/policies:simulate", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * 
     * <p><b>200</b> - Report for any parseable document
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * @param routingPolicy  (required)
     * @param context  (optional, default to create)
     * @return ValidationReport
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ValidationReport validatePolicy(RoutingPolicy routingPolicy, String context) throws RestClientException {
        return validatePolicyWithHttpInfo(routingPolicy, context).getBody();
    }

    /**
     * 
     * 
     * <p><b>200</b> - Report for any parseable document
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * @param routingPolicy  (required)
     * @param context  (optional, default to create)
     * @return ResponseEntity&lt;ValidationReport&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ValidationReport> validatePolicyWithHttpInfo(RoutingPolicy routingPolicy, String context) throws RestClientException {
        Object localVarPostBody = routingPolicy;
        
        // verify the required parameter 'routingPolicy' is set
        if (routingPolicy == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'routingPolicy' when calling validatePolicy");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "context", context));
        

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json", "application/yaml"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ValidationReport> localReturnType = new ParameterizedTypeReference<ValidationReport>() {};
        return apiClient.invokeAPI("/v1/policies:validate", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
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
            "application/json", "application/yaml"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        return apiClient.invokeAPI(localVarPath, method, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, returnType);
    }
}
