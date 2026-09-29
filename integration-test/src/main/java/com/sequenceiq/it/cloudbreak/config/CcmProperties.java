package com.sequenceiq.it.cloudbreak.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CcmProperties {

    @Value("${integrationtest.ccm.host:}")
    private String host;

    @Value("${integrationtest.ccm.port:443}")
    private int port;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
