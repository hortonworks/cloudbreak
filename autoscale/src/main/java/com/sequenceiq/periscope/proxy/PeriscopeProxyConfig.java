package com.sequenceiq.periscope.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PeriscopeProxyConfig {

    @Value("${https.proxyHost:}")
    private String httpsProxyHost;

    @Value("${https.proxyPort:}")
    private String httpsProxyPort;

    @Value("${periscope.useProxyForClusterConnection:false}")
    private boolean useProxyForClusterConnection;

    public String getHttpsProxyHost() {
        return httpsProxyHost;
    }

    public int getHttpsProxyPort() {
        return Integer.parseInt(httpsProxyPort);
    }

    public boolean isUseProxyForClusterConnection() {
        return useProxyForClusterConnection && isHttpsProxyConfigured();
    }

    public boolean isHttpsProxyConfigured() {
        return httpsProxyHost != null && httpsProxyPort != null;
    }
}
