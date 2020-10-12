package com.sequenceiq.freeipa.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

@Component
public class FreeIpaHealthCheckClientFactory {

    private static final String DEFAULT_BASE_PATH = "/freeipahealthcheck";

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${freeipa.healthcheck.connectionTimeoutMs}")
    private int connetionTimeoutMillis;

    @Value("${freeipa.healthcheck.readTimeoutMs}")
    private int readTimeoutMillis;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public FreeIpaHealthCheckClient getClient(Stack stack, InstanceMetaData instance)
            throws FreeIpaClientException, MalformedURLException {
        FreeIpaHealthCheckClient client;
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            client = buildFreeIpaHealthCheckClientForClusterProxy(stack, instance);
        } else {
            client = buildFreeIpaHealthCheckClientForDirectConnect(stack, instance);
        }
        return client;
    }

    private FreeIpaHealthCheckClient buildFreeIpaHealthCheckClientForClusterProxy(Stack stack, InstanceMetaData instanceMetaData)
            throws FreeIpaClientException, MalformedURLException {
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        String clusterProxyPath = toClusterProxyBasepath(stack, instanceMetaData.getDiscoveryFQDN());
        return buildFreeIpaHealthCheckClient(httpClientConfig, clusterProxyConfiguration.getClusterProxyPort(), clusterProxyPath, clusterProxyHeaders(),
                new FreeIpaHealthCheckClusterProxyErrorRpcListener());
    }

    private FreeIpaHealthCheckClient buildFreeIpaHealthCheckClientForDirectConnect(Stack stack, InstanceMetaData instanceMetaData)
            throws FreeIpaClientException, MalformedURLException {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(stack, instanceMetaData.getPublicIpWrapper(), instanceMetaData);
        int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        return buildFreeIpaHealthCheckClient(httpClientConfig, gatewayPort, DEFAULT_BASE_PATH, Map.of(), null);
    }

    private FreeIpaHealthCheckClient buildFreeIpaHealthCheckClient(HttpClientConfig clientConfig, int port, String basePath,
            Map<String, String> headers, FreeIpaHealthCheckRpcListener listener)
            throws FreeIpaClientException, MalformedURLException {

        Client restClient;
        try {
            restClient = RestClientUtil.createClient(clientConfig.getServerCert(), clientConfig.getClientCert(), clientConfig.getClientKey(),
                    connetionTimeoutMillis, readTimeoutMillis, restDebug);
        } catch (Exception e) {
            throw new FreeIpaClientException("Unable to create client for FreeIPA health checks", e);
        }
        URL freeIpaHealthCheckUrl = getIpaHealthCheckUrl(clientConfig, port, basePath);
        return new FreeIpaHealthCheckClient(restClient, freeIpaHealthCheckUrl, headers, listener);
    }

    private String toClusterProxyBasepath(Stack stack, String clusterProxyServiceName) {
        return String.format("%s%s", clusterProxyService.getProxyPath(stack, Optional.of(clusterProxyServiceName)), DEFAULT_BASE_PATH);
    }

    private URL getIpaHealthCheckUrl(HttpClientConfig clientConfig, int port, String basePath) throws MalformedURLException {
        String scheme = clientConfig.hasSSLConfigs() ? "https://" : "http://";
        String path = StringUtils.isBlank(basePath) ? "" : basePath;
        return new URL(scheme + clientConfig.getApiAddress() + ':' + port + path);
    }

    private Map<String, String> clusterProxyHeaders() {
        return Map.of(
                "Proxy-Ignore-Auth", "true",
                "Proxy-With-Timeout", Integer.toString(connetionTimeoutMillis)
        );
    }
}
