package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.api.swagger.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ClouderaManagerProxiedClientFactory {

    private static final String API_VERSION_32 = "v32";

    private static final int CM_READ_TIMEOUT_MS =  60 * 1000;

    private static final int CM_CONNECT_TIMEOUT_MS = 20 * 1000;

    @Value("${clusterProxy.url:}")
    private String clusterProxyUrl;

    private String getClusterProxyCloderaManagerBasePath(String clusterCrn) {
        return String.format("%s/proxy/%s/cloudera-manager/api/%s", clusterProxyUrl, clusterCrn, API_VERSION_32);
    }

    public ApiClient getProxiedClouderaManagerClient(String clouderaManagerStackCrn) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(getClusterProxyCloderaManagerBasePath(clouderaManagerStackCrn));
        apiClient.setConnectTimeout(CM_READ_TIMEOUT_MS);
        apiClient.getHttpClient().setReadTimeout(CM_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        return apiClient;
    }
}
