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
import com.sequenceiq.cloudbreak.client.RpcListener;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

@Component
public abstract class FreeIpaClientFactory<T> {

    @Value("${rest.debug}")
    private boolean restDebug;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public T getClient(Stack stack, InstanceMetaData instance)
            throws FreeIpaClientException, MalformedURLException {
        T client;
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            client = buildClientForClusterProxy(stack, instance, Optional.empty(), Optional.empty());
        } else {
            client = buildClientForDirectConnect(stack, instance, Optional.empty(), Optional.empty());
        }
        return client;
    }

    public T getClientWithBasicAuth(Stack stack, InstanceMetaData instance, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {
        T client;
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            client = buildClientForClusterProxy(stack, instance, username, password);
        } else {
            client = buildClientForDirectConnect(stack, instance, username, password);
        }
        return client;
    }

    private T buildClientForClusterProxy(Stack stack, InstanceMetaData instanceMetaData, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        String clusterProxyPath = toClusterProxyBasepath(stack, instanceMetaData.getDiscoveryFQDN());
        return buildFreeIpaClient(httpClientConfig, clusterProxyConfiguration.getClusterProxyPort(), clusterProxyPath, clusterProxyHeaders(),
                new FreeIpaClusterProxyErrorRpcListener(), username, password);
    }

    private T buildClientForDirectConnect(Stack stack, InstanceMetaData instanceMetaData, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(stack, instanceMetaData.getPublicIpWrapper(), instanceMetaData);
        int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        return buildFreeIpaClient(httpClientConfig, gatewayPort, getDefaultBasePath(), Map.of(), null, username, password);
    }

    protected abstract String getDefaultBasePath();

    private T buildFreeIpaClient(HttpClientConfig clientConfig, int port, String basePath,
            Map<String, String> headers, RpcListener listener, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {

        Client restClient;
        try {
            restClient = RestClientUtil.createClient(clientConfig.getServerCert(), clientConfig.getClientCert(), clientConfig.getClientKey(),
                    getConnectionTimeoutMillis(), getReadTimeoutMillis(), restDebug);
        } catch (Exception e) {
            throw new RetryableFreeIpaClientException("Unable to create client for FreeIPA health checks", e);
        }
        URL freeIpaUrl = getIpaUrl(clientConfig, port, basePath);

        if (username.isPresent() && password.isPresent()) {
            return instantiateClient(headers, listener, restClient, freeIpaUrl, username, password);
        }
        return instantiateClient(headers, listener, restClient, freeIpaUrl);
    }

    protected abstract int getReadTimeoutMillis();

    protected abstract int getConnectionTimeoutMillis();

    protected abstract T instantiateClient(Map<String, String> headers, RpcListener listener, Client restClient, URL freeIpaUrl);

    protected abstract T instantiateClient(Map<String, String> headers, RpcListener listener, Client restClient, URL freeIpaUrl, Optional<String> username,
            Optional<String> password);

    private String toClusterProxyBasepath(Stack stack, String clusterProxyServiceName) {
        return String.format("%s%s", clusterProxyService.getProxyPath(stack, Optional.of(clusterProxyServiceName)), getDefaultBasePath());
    }

    private URL getIpaUrl(HttpClientConfig clientConfig, int port, String basePath) throws MalformedURLException {
        String scheme = clientConfig.hasSSLConfigs() ? "https://" : "http://";
        String path = StringUtils.isBlank(basePath) ? "" : basePath;
        return new URL(scheme + clientConfig.getApiAddress() + ':' + port + path);
    }

    private Map<String, String> clusterProxyHeaders() {
        return Map.of(
                "Proxy-Ignore-Auth", "true",
                "Proxy-With-Timeout", Integer.toString(getConnectionTimeoutMillis())
        );
    }
}
