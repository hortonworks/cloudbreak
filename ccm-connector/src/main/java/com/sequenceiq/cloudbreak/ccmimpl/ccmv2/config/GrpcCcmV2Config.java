package com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GrpcCcmV2Config {

    @Value("${altus.ccmv2mgmt.host:thunderhead-clusterconnectivitymanagementv2.thunderhead-clusterconnectivitymanagementv2.svc.cluster.local}")
    private String host;

    @Value("${altus.ccmv2mgmt.port:80}")
    private int port;

    @Value("${altus.ccmv2mgmt.client.polling_interval_ms:5000}")
    private int pollingIntervalMs;

    @Value("${altus.ccmv2mgmt.client.timeout_ms:300000}")
    private int timeoutMs;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
