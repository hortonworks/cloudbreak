package com.sequenceiq.cloudbreak.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudbreakProxyConfig {

    @Value("${https.proxyHost:}")
    private String httpsProxyHost;

    @Value("${https.proxyPort:}")
    private String httpsProxyPort;

    @Value("${cb.useProxyForClusterConnection:}")
    private boolean useProxyForClusterConnection;

    public String getHttpsProxyHost() {
        return httpsProxyHost;
    }

    public int getHttpsProxyPort() {
        return Integer.valueOf(httpsProxyPort);
    }

    public boolean isUseProxyForClusterConnection() {
        return useProxyForClusterConnection && isHttpsProxyConfigured();
    }

    public boolean isHttpsProxyConfigured() {
        return httpsProxyHost != null && httpsProxyPort != null;
    }
}
