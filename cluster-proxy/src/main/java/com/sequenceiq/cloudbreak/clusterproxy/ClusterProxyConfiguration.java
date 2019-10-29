package com.sequenceiq.cloudbreak.clusterproxy;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClusterProxyConfiguration {

    public static final String FREEIPA_SERVICE_NAME = "freeipa";

    private static final int DEFAULT_CLUSTER_PROXY_PORT = 10080;

    @Value("${clusterProxy.enabled:false}")
    private boolean clusterProxyIntegrationEnabled;

    @Value("${clusterProxy.url:localhost:10080/cluster-proxy}")
    private String clusterProxyUrl;

    @Value("${clusterProxy.registerConfigPath:/rpc/forceRegisterConfig}")
    private String registerConfigPath;

    @Value("${clusterProxy.updateConfigPath:/rpc/updateConfig}")
    private String updateConfigPath;

    @Value("${clusterProxy.removeConfigPath:/rpc/removeConfig}")
    private String removeConfigPath;

    private String clusterProxyHost = "localhost";

    private int clusterProxyPort = DEFAULT_CLUSTER_PROXY_PORT;

    private String clusterProxyBasePath = "cluster-proxy";

    @PostConstruct
    private void init() throws IllegalArgumentException {
        if (clusterProxyIntegrationEnabled) {
            try {
                URL url = new URL(clusterProxyUrl);
                clusterProxyHost = url.getHost();
                clusterProxyPort = url.getPort();
                clusterProxyBasePath = url.getPath();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Configuration `clusterProxy.url` is not a URL.", e);
            }
        }
    }

    public boolean isClusterProxyIntegrationEnabled() {
        return clusterProxyIntegrationEnabled;
    }

    public void setClusterProxyIntegrationEnabled(boolean clusterProxyIntegrationEnabled) {
        this.clusterProxyIntegrationEnabled = clusterProxyIntegrationEnabled;
    }

    public String getClusterProxyUrl() {
        return clusterProxyUrl;
    }

    public void setClusterProxyUrl(String clusterProxyUrl) {
        this.clusterProxyUrl = clusterProxyUrl;
    }

    public String getRegisterConfigPath() {
        return registerConfigPath;
    }

    public void setRegisterConfigPath(String registerConfigPath) {
        this.registerConfigPath = registerConfigPath;
    }

    public String getUpdateConfigPath() {
        return updateConfigPath;
    }

    public void setUpdateConfigPath(String updateConfigPath) {
        this.updateConfigPath = updateConfigPath;
    }

    public String getRemoveConfigPath() {
        return removeConfigPath;
    }

    public void setRemoveConfigPath(String removeConfigPath) {
        this.removeConfigPath = removeConfigPath;
    }

    public String getClusterProxyHost() {
        return clusterProxyHost;
    }

    public int getClusterProxyPort() {
        return clusterProxyPort;
    }

    public String getClusterProxyBasePath() {
        return clusterProxyBasePath;
    }

    public String getRegisterConfigUrl() {
        return clusterProxyUrl + registerConfigPath;
    }

    public String getUpdateConfigUrl() {
        return clusterProxyUrl + updateConfigPath;
    }

    public String getRemoveConfigUrl() {
        return clusterProxyUrl + removeConfigPath;
    }
}
