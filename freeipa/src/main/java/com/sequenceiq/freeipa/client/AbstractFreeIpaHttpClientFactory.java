package com.sequenceiq.freeipa.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.client.RpcListener;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.service.sslcontext.SSLContextProvider;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

@Component
public abstract class AbstractFreeIpaHttpClientFactory<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFreeIpaHttpClientFactory.class);

    @Value("${rest.debug}")
    private boolean restDebug;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private SSLContextProvider sslContextProvider;

    public T getClient(Stack stack, InstanceMetaData instance)
            throws FreeIpaClientException, MalformedURLException {
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            return buildClientForClusterProxy(stack, instance, Optional.empty(), Optional.empty());
        } else {
            return buildClientForDirectConnect(stack, instance, Optional.empty(), Optional.empty());
        }
    }

    public T getClientWithBasicAuth(Stack stack, InstanceMetaData instance, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            return buildClientForClusterProxy(stack, instance, username, password);
        } else {
            return buildClientForDirectConnect(stack, instance, username, password);
        }
    }

    private T buildClientForClusterProxy(Stack stack, InstanceMetaData instanceMetaData, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {
        LOGGER.debug("Creating client with cluster proxy for instance: [{}]", instanceMetaData);
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        String clusterProxyPath = toClusterProxyBasepath(stack, instanceMetaData.getDiscoveryFQDN());
        LOGGER.debug("Using cluster proxy path: [{}]", clusterProxyPath);
        return buildFreeIpaClient(httpClientConfig, clusterProxyConfiguration.getClusterProxyPort(), clusterProxyPath, clusterProxyHeaders(),
                new FreeIpaClusterProxyErrorRpcListener(), username, password);
    }

    private T buildClientForDirectConnect(Stack stack, InstanceMetaData instanceMetaData, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {
        LOGGER.debug("Creating client with direct connection for instance: [{}]", instanceMetaData);
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(stack, instanceMetaData.getPublicIpWrapper(), instanceMetaData);
        int gatewayPort = Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        return buildFreeIpaClient(httpClientConfig, gatewayPort, getDefaultBasePath(), Map.of(), null, username, password);
    }

    protected abstract String getDefaultBasePath();

    private T buildFreeIpaClient(HttpClientConfig clientConfig, int port, String basePath,
            Map<String, String> headers, RpcListener listener, Optional<String> username, Optional<String> password)
            throws FreeIpaClientException, MalformedURLException {
        Client restClient = createRestClient(clientConfig);
        URL freeIpaUrl = getIpaUrl(clientConfig, port, basePath);
        if (username.isPresent() && password.isPresent()) {
            return instantiateClient(headers, listener, restClient, freeIpaUrl, username, password);
        } else {
            return instantiateClient(headers, listener, restClient, freeIpaUrl);
        }
    }

    private Client createRestClient(HttpClientConfig clientConfig) throws RetryableFreeIpaClientException {
        try {
            SSLContext sslContext = sslContextProvider.getSSLContext(clientConfig.getServerCert(), Optional.empty(),
                    clientConfig.getClientCert(), clientConfig.getClientKey());
            return RestClientUtil.createClient(sslContext, getConnectionTimeoutMillis(), getReadTimeoutMillis(), restDebug);
        } catch (Exception e) {
            throw new RetryableFreeIpaClientException("Unable to create client for FreeIPA health checks", e);
        }
    }

    protected abstract int getReadTimeoutMillis();

    protected abstract int getConnectionTimeoutMillis();

    protected abstract T instantiateClient(Map<String, String> headers, RpcListener listener, Client restClient, URL freeIpaUrl);

    protected abstract T instantiateClient(Map<String, String> headers, RpcListener listener, Client restClient, URL freeIpaUrl, Optional<String> username,
            Optional<String> password);

    private String toClusterProxyBasepath(Stack stack, String clusterProxyServiceName) {
        return String.format("%s%s", clusterProxyService.getProxyPathPgwAsFallBack(stack, clusterProxyServiceName), getDefaultBasePath());
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
