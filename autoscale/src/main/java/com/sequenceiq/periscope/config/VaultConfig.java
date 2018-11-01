package com.sequenceiq.periscope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

@Configuration
public class VaultConfig extends AbstractVaultConfiguration {

    @Value("${periscope.vault.addr:vault.service.consul}")
    private String address;

    @Value("${periscope.vault.port:8200}")
    private int port;

    @Value("${periscope.vault.root.token:}")
    private String rootToken;

    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = VaultEndpoint.create(address, port);
        endpoint.setScheme("http");
        return endpoint;
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(rootToken);
    }
}
