package com.sequenceiq.environment.configuration.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaNetworkProvider;
import com.sequenceiq.freeipa.api.client.internal.FreeIpaApiClientParams;

@Configuration
public class FreeIpaApiConfig {

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:true}")
    private boolean ignorePreValidation;

    @Inject
    private List<FreeIpaNetworkProvider> freeIpaNetworkProviders;

    @Inject
    @Named("freeIpaServerUrl")
    private String freeIpaServerUrl;

    @Bean
    public FreeIpaApiClientParams freeIpaApiClientParams() {
        return new FreeIpaApiClientParams(restDebug, certificateValidation, ignorePreValidation, freeIpaServerUrl);
    }

    @Bean
    public Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform() {
        Map<CloudPlatform, FreeIpaNetworkProvider> map = new HashMap<>();
        for (FreeIpaNetworkProvider freeIpaNetworkProvider : freeIpaNetworkProviders) {
            map.put(freeIpaNetworkProvider.cloudPlatform(), freeIpaNetworkProvider);
        }
        return map;
    }

}
