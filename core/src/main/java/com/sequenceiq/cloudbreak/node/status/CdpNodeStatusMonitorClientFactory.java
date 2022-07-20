package com.sequenceiq.cloudbreak.node.status;

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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClient;

@Component
public class CdpNodeStatusMonitorClientFactory {

    protected static final String DEFAULT_BASE_PATH = "/nodestatus";

    @Value("${rest.debug}")
    private boolean restDebug;

    @Value("${cb.nodestatus.connectionTimeoutMs}")
    private int connectionTimeoutMillis;

    @Value("${cb.nodestatus.readTimeoutMs}")
    private int readTimeoutMillis;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public CdpNodeStatusMonitorClient getClient(Stack stack, InstanceMetadataView instance) {
        final Optional<String> username;
        final Optional<String> password;
        if (stack.getCluster() != null
                && StringUtils.isNoneEmpty(stack.getCluster().getCdpNodeStatusMonitorUser(), stack.getCluster().getCdpNodeStatusMonitorPassword())) {
            username = Optional.of(stack.getCluster().getCdpNodeStatusMonitorUser());
            password = Optional.of(stack.getCluster().getCdpNodeStatusMonitorPassword());
        } else {
            username = Optional.empty();
            password = Optional.empty();
        }
        CdpNodeStatusMonitorClient client;
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            client = buildClientForClusterProxy(stack, instance, username, password);
        } else {
            client = buildClientForDirectConnect(stack, username, password);
        }
        return client;
    }

    private CdpNodeStatusMonitorClient buildClientForClusterProxy(Stack stack, InstanceMetadataView instanceMetaData, Optional<String> username,
            Optional<String> password) {
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        String clusterProxyPath = toClusterProxyBasepath(stack, instanceMetaData.getInstanceId());
        return buildNodeStatusMonitorClient(httpClientConfig, clusterProxyConfiguration.getClusterProxyPort(), clusterProxyPath, clusterProxyHeaders(),
                username, password);
    }

    private CdpNodeStatusMonitorClient buildClientForDirectConnect(Stack stack, Optional<String> username,
            Optional<String> password) {
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), stack.getClusterManagerIp(),
                stack.cloudPlatform());
        int gatewayPort = Optional.ofNullable(stack.getGatewayPort()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
        return buildNodeStatusMonitorClient(httpClientConfig, gatewayPort, DEFAULT_BASE_PATH, Map.of(), username, password);
    }

    private CdpNodeStatusMonitorClient buildNodeStatusMonitorClient(HttpClientConfig clientConfig, int port, String basePath,
            Map<String, String> headers, Optional<String> username, Optional<String> password) {

        Client restClient;
        try {
            restClient = RestClientUtil.createClient(clientConfig.getServerCert(), clientConfig.getClientCert(), clientConfig.getClientKey(),
                    connectionTimeoutMillis, readTimeoutMillis, restDebug);
        } catch (Exception e) {
            throw new CloudbreakServiceException("Unable to create client for node status checks", e);
        }
        URL url = null;
        try {
            url = getUrl(clientConfig, port, basePath);
        } catch (MalformedURLException e) {
            throw new CloudbreakServiceException("Unable to build url to check node status", e);
        }

        if (username.isPresent() && password.isPresent()) {
            return instantiateClient(headers, restClient, url, username, password);
        }
        return instantiateClient(headers, restClient, url);
    }

    protected CdpNodeStatusMonitorClient instantiateClient(Map<String, String> headers, Client restClient, URL url) {
        return new CdpNodeStatusMonitorClient(restClient, url, headers, null, Optional.empty(), Optional.empty());
    }

    protected CdpNodeStatusMonitorClient instantiateClient(Map<String, String> headers, Client restClient, URL url,
            Optional<String> username, Optional<String> password) {
        return new CdpNodeStatusMonitorClient(restClient, url, headers, null, username, password);
    }

    private String toClusterProxyBasepath(Stack stack, String clusterProxyServiceName) {
        return String.format("%s%s", clusterProxyService.getProxyPath(stack.getResourceCrn(), clusterProxyServiceName), DEFAULT_BASE_PATH);
    }

    private URL getUrl(HttpClientConfig clientConfig, int port, String basePath) throws MalformedURLException {
        String scheme = clientConfig.hasSSLConfigs() ? "https://" : "http://";
        String path = StringUtils.isBlank(basePath) ? "" : basePath;
        return new URL(scheme + clientConfig.getApiAddress() + ':' + port + path);
    }

    private Map<String, String> clusterProxyHeaders() {
        return Map.of(
                "Proxy-Ignore-Auth", "true",
                "Proxy-With-Timeout", Integer.toString(connectionTimeoutMillis)
        );
    }
}
