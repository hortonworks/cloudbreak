package com.sequenceiq.cloudbreak.clusterproxy;

import java.net.MalformedURLException;
import java.net.URL;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClusterProxyConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyConfiguration.class);

    @Value("${clusterProxy.enabled:}")
    private boolean clusterProxyIntegrationEnabled;

    @Value("${clusterProxy.url:}")
    private String clusterProxyUrl;

    @Value("${clusterProxy.registerConfigPath}")
    private String registerConfigPath;

    @Value("${clusterProxy.updateConfigPath}")
    private String updateConfigPath;

    @Value("${clusterProxy.removeConfigPath}")
    private String removeConfigPath;

    @Value("${clusterProxy.readConfigPath}")
    private String readConfigPath;

    @Value("${clusterProxy.httpProxyPath:/proxy}")
    private String httpProxyPath;

    private String clusterProxyHost;

    private int clusterProxyPort;

    private String clusterProxyBasePath;

    private String clusterProxyProtocol;

    @PostConstruct
    private void init() throws IllegalArgumentException {
        if (clusterProxyIntegrationEnabled) {
            LOGGER.info("Cluster proxy integration enabled, initializing config from URL: [{}]", clusterProxyUrl);
            try {
                URL url = new URL(clusterProxyUrl);
                clusterProxyHost = url.getHost();
                clusterProxyPort = url.getPort();
                clusterProxyBasePath = url.getPath();
                clusterProxyProtocol = url.getProtocol();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Configuration `clusterProxy.url` is not a URL.", e);
            }
        } else {
            LOGGER.info("Cluster proxy integration is disabled");
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

    public String getReadConfigPath() {
        return readConfigPath;
    }

    public void setReadConfigPath(String readConfigPath) {
        this.readConfigPath = readConfigPath;
    }

    public String getHttpProxyPath() {
        return httpProxyPath;
    }

    public void setHttpProxyPath(String httpProxyPath) {
        this.httpProxyPath = httpProxyPath;
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

    public String getClusterProxyProtocol() {
        return clusterProxyProtocol;
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

    public String getReadConfigUrl() {
        return clusterProxyUrl + readConfigPath;
    }

}
