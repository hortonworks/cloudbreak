package com.sequenceiq.smartrouter.client;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.cloudera.smartrouter.client.ApiClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClient;

/**
 * Generated {@link ApiClient} configured by a {@link ClusterProxyProxyClient}: it uses its {@link org.springframework.web.client.RestTemplate} and service URL,
 * and every call goes through {@link ClusterProxyProxyClient#callClusterProxy}, which measures it and translates failures to {@link ClusterProxyException}.
 */
public class SmartRouterApiClient extends ApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartRouterApiClient.class);

    private final ClusterProxyProxyClient clusterProxyProxyClient;

    public SmartRouterApiClient(ClusterProxyProxyClient clusterProxyProxyClient) {
        super(clusterProxyProxyClient.getRestTemplate());
        this.clusterProxyProxyClient = clusterProxyProxyClient;
        basePath = clusterProxyProxyClient.getServiceUrl();
    }

    @Override
    @SuppressWarnings("checkstyle:ParameterNumber")
    public <T> ResponseEntity<T> invokeAPI(String path, HttpMethod method, Map<String, Object> pathParams, MultiValueMap<String, String> queryParams,
            Object body, HttpHeaders headerParams, MultiValueMap<String, String> cookieParams, MultiValueMap<String, Object> formParams,
            List<MediaType> accept, MediaType contentType, String[] authNames, ParameterizedTypeReference<T> returnType) {
        String call = method + " " + basePath + path;
        LOGGER.debug("Going through cluster proxy to call Smart Router {}", call);
        return clusterProxyProxyClient.callClusterProxy(call, () -> super.invokeAPI(path, method, pathParams, queryParams, body, headerParams,
                cookieParams, formParams, accept, contentType, authNames, returnType));
    }
}
