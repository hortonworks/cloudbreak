package com.sequenceiq.cloudbreak.conf;


import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

@Configuration
class VaultConf extends AbstractVaultConfiguration {


    //TODO: these variables need to come from Profile file fordev environment
    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = VaultEndpoint.create("192.168.64.2", 8200);
        endpoint.setScheme("http");
        return endpoint;
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication("4uZQovesNGpco8z4n9s6DKNi");
    }
}
