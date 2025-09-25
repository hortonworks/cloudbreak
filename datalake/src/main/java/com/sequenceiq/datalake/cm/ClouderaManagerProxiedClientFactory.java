package com.sequenceiq.datalake.cm;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.cm.client.tracing.CmRequestIdProviderInterceptor;
import com.sequenceiq.cloudbreak.cm.client.tracing.CmRequestLoggerInterceptor;

import okhttp3.OkHttpClient;

@Component
public class ClouderaManagerProxiedClientFactory {

    private static final String API_VERSION_31 = "v31";

    private static final int CM_READ_TIMEOUT_MS = 60 * 1000;

    private static final int CM_CONNECT_TIMEOUT_MS = 60 * 1000;

    @Value("${clusterProxy.url:}")
    private String clusterProxyUrl;

    @Inject
    private CmRequestLoggerInterceptor cmRequestLoggerInterceptor;

    @Inject
    private CmRequestIdProviderInterceptor cmRequestIdProviderInterceptor;

    private String getClusterProxyCloderaManagerBasePath(String clusterCrn) {
        return String.format("%s/proxy/%s/cloudera-manager/api/%s", clusterProxyUrl, clusterCrn, API_VERSION_31);
    }

    public ApiClient getProxiedClouderaManagerClient(String clouderaManagerStackCrn) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(getClusterProxyCloderaManagerBasePath(clouderaManagerStackCrn));
        OkHttpClient.Builder builder = apiClient.getHttpClient().newBuilder();
        builder.interceptors().add(cmRequestIdProviderInterceptor);
        builder.interceptors().add(cmRequestLoggerInterceptor);
        builder.connectTimeout(CM_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        builder.readTimeout(CM_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        apiClient.setHttpClient(builder.build());
        return apiClient;
    }

}
