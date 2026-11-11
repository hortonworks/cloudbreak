package com.cloudera.smartrouter.client.api;

import com.cloudera.smartrouter.client.ApiClient;
import com.cloudera.smartrouter.client.BaseApi;

import com.cloudera.smartrouter.client.model.ApiErrorResponse;
import com.cloudera.smartrouter.client.model.QueueListResponse;
import com.cloudera.smartrouter.client.model.QueueRegistry;
import com.cloudera.smartrouter.client.model.QueueSyncResponse;
import com.cloudera.smartrouter.client.model.QueueUpdateRequest;
import com.cloudera.smartrouter.client.model.QueueUpdateResponse;

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


public class QueuesApi extends BaseApi {

    public QueuesApi() {
        super(new ApiClient());
    }

    public QueuesApi(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * 
     * Removes one declaration (the queue then inherits from its nearest declared ancestor, if any). Requires a security or platform administrator and the current registry version.
     * <p><b>204</b> - Removed
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale queue registry version; the ETag header carries the current version
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param queuePath Full YARN queue path such as root.etl (required)
     * @param ifMatch Current queue registry version; missing returns 428, stale returns 412 with the current ETag (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deleteQueue(String queuePath, String ifMatch, String xChangeComment) throws RestClientException {
        deleteQueueWithHttpInfo(queuePath, ifMatch, xChangeComment);
    }

    /**
     * 
     * Removes one declaration (the queue then inherits from its nearest declared ancestor, if any). Requires a security or platform administrator and the current registry version.
     * <p><b>204</b> - Removed
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>404</b> - Absent or tombstoned
     * <p><b>412</b> - Stale queue registry version; the ETag header carries the current version
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param queuePath Full YARN queue path such as root.etl (required)
     * @param ifMatch Current queue registry version; missing returns 428, stale returns 412 with the current ETag (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteQueueWithHttpInfo(String queuePath, String ifMatch, String xChangeComment) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'queuePath' is set
        if (queuePath == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'queuePath' when calling deleteQueue");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling deleteQueue");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("queuePath", queuePath);

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
        return apiClient.invokeAPI("/v1/config/queues/{queuePath}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Queue registry: which YARN queues may burst to the cloud. A declaration covers the queue and its descendants; the nearest declared ancestor wins and an explicit false overrides a burstable parent. Queues without a declared ancestor are not burstable. With Accept application/yaml the registry is returned as a QueueRegistry document (export, edit, PUT).
     * <p><b>200</b> - Registered queues with their YARN status
     * @return QueueListResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public QueueListResponse listQueues() throws RestClientException {
        return listQueuesWithHttpInfo().getBody();
    }

    /**
     * 
     * Queue registry: which YARN queues may burst to the cloud. A declaration covers the queue and its descendants; the nearest declared ancestor wins and an explicit false overrides a burstable parent. Queues without a declared ancestor are not burstable. With Accept application/yaml the registry is returned as a QueueRegistry document (export, edit, PUT).
     * <p><b>200</b> - Registered queues with their YARN status
     * @return ResponseEntity&lt;QueueListResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QueueListResponse> listQueuesWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<QueueListResponse> localReturnType = new ParameterizedTypeReference<QueueListResponse>() {};
        return apiClient.invokeAPI("/v1/config/queues", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Replaces every declaration. Requires a security or platform administrator and the current registry version. A queue new to the registry must exist in YARN when the inventory is loaded. metadata is ignored.
     * <p><b>200</b> - The registry after the change
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale queue registry version; the ETag header carries the current version
     * <p><b>422</b> - QUEUE_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id (queue.path, queue.unique, queue.known, queue.burstable, queue.count, queue.document)
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param ifMatch Current queue registry version; missing returns 428, stale returns 412 with the current ETag (required)
     * @param queueRegistry  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return QueueRegistry
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public QueueRegistry replaceQueues(String ifMatch, QueueRegistry queueRegistry, String xChangeComment) throws RestClientException {
        return replaceQueuesWithHttpInfo(ifMatch, queueRegistry, xChangeComment).getBody();
    }

    /**
     * 
     * Replaces every declaration. Requires a security or platform administrator and the current registry version. A queue new to the registry must exist in YARN when the inventory is loaded. metadata is ignored.
     * <p><b>200</b> - The registry after the change
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale queue registry version; the ETag header carries the current version
     * <p><b>422</b> - QUEUE_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id (queue.path, queue.unique, queue.known, queue.burstable, queue.count, queue.document)
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param ifMatch Current queue registry version; missing returns 428, stale returns 412 with the current ETag (required)
     * @param queueRegistry  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;QueueRegistry&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QueueRegistry> replaceQueuesWithHttpInfo(String ifMatch, QueueRegistry queueRegistry, String xChangeComment) throws RestClientException {
        Object localVarPostBody = queueRegistry;
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling replaceQueues");
        }
        
        // verify the required parameter 'queueRegistry' is set
        if (queueRegistry == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'queueRegistry' when calling replaceQueues");
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
            "application/json", "application/yaml"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json", "application/yaml"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<QueueRegistry> localReturnType = new ParameterizedTypeReference<QueueRegistry>() {};
        return apiClient.invokeAPI("/v1/config/queues", HttpMethod.PUT, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Re-reads the YARN queue list now and reports how the registered queues&#39; status changed. Never deletes declarations. Requires a security or platform administrator.
     * <p><b>200</b> - Sync result
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @return QueueSyncResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public QueueSyncResponse syncQueues() throws RestClientException {
        return syncQueuesWithHttpInfo().getBody();
    }

    /**
     * 
     * Re-reads the YARN queue list now and reports how the registered queues&#39; status changed. Never deletes declarations. Requires a security or platform administrator.
     * <p><b>200</b> - Sync result
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @return ResponseEntity&lt;QueueSyncResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QueueSyncResponse> syncQueuesWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<QueueSyncResponse> localReturnType = new ParameterizedTypeReference<QueueSyncResponse>() {};
        return apiClient.invokeAPI("/v1/config/queues/sync", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * 
     * Declares one queue burstable or not burstable. Requires a security or platform administrator and the current registry version. A queue new to the registry must exist in YARN when the inventory is loaded.
     * <p><b>200</b> - The declaration; updated is false when it already had this value
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale queue registry version; the ETag header carries the current version
     * <p><b>422</b> - QUEUE_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id (queue.path, queue.unique, queue.known, queue.burstable, queue.count, queue.document)
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param queuePath Full YARN queue path such as root.etl (required)
     * @param ifMatch Current queue registry version; missing returns 428, stale returns 412 with the current ETag (required)
     * @param queueUpdateRequest  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return QueueUpdateResponse
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public QueueUpdateResponse updateQueue(String queuePath, String ifMatch, QueueUpdateRequest queueUpdateRequest, String xChangeComment) throws RestClientException {
        return updateQueueWithHttpInfo(queuePath, ifMatch, queueUpdateRequest, xChangeComment).getBody();
    }

    /**
     * 
     * Declares one queue burstable or not burstable. Requires a security or platform administrator and the current registry version. A queue new to the registry must exist in YARN when the inventory is loaded.
     * <p><b>200</b> - The declaration; updated is false when it already had this value
     * <p><b>400</b> - Malformed body, bad parameter or schema violation
     * <p><b>403</b> - Missing role or intrinsic resource
     * <p><b>412</b> - Stale queue registry version; the ETag header carries the current version
     * <p><b>422</b> - QUEUE_INVALID; errors[].field is a JSON pointer, errors[].rejectedValue the check id (queue.path, queue.unique, queue.known, queue.burstable, queue.count, queue.document)
     * <p><b>428</b> - Required precondition header missing
     * <p><b>503</b> - POLICY_STORE_UNAVAILABLE when the durable policy store is unreachable, or IDENTITY_UNAVAILABLE when no trusted caller identity is configured (SPNEGO or mTLS off) and unauthenticated writes are disabled
     * @param queuePath Full YARN queue path such as root.etl (required)
     * @param ifMatch Current queue registry version; missing returns 428, stale returns 412 with the current ETag (required)
     * @param queueUpdateRequest  (required)
     * @param xChangeComment Stored in the revision/event; required for lock and unlock. (optional)
     * @return ResponseEntity&lt;QueueUpdateResponse&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<QueueUpdateResponse> updateQueueWithHttpInfo(String queuePath, String ifMatch, QueueUpdateRequest queueUpdateRequest, String xChangeComment) throws RestClientException {
        Object localVarPostBody = queueUpdateRequest;
        
        // verify the required parameter 'queuePath' is set
        if (queuePath == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'queuePath' when calling updateQueue");
        }
        
        // verify the required parameter 'ifMatch' is set
        if (ifMatch == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'ifMatch' when calling updateQueue");
        }
        
        // verify the required parameter 'queueUpdateRequest' is set
        if (queueUpdateRequest == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'queueUpdateRequest' when calling updateQueue");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("queuePath", queuePath);

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

        ParameterizedTypeReference<QueueUpdateResponse> localReturnType = new ParameterizedTypeReference<QueueUpdateResponse>() {};
        return apiClient.invokeAPI("/v1/config/queues/{queuePath}", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
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
