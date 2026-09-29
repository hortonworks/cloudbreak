package com.sequenceiq.it.cloudbreak.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VaultProperties {

    @Value("${integrationtest.vault.address:vault}")
    private String address;

    @Value("${integrationtest.vault.port:8200}")
    private int port;

    @Value("${integrationtest.vault.scheme:http}")
    private String scheme;

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getHealthUrl() {
        return scheme + "://" + address + ":" + port + "/v1/sys/health";
    }
}
