package com.sequenceiq.maintenance.configuration;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.cloudbreak.client.internal.CloudbreakApiClientConfig;
import com.sequenceiq.cloudbreak.client.internal.CloudbreakApiClientParams;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;
import com.sequenceiq.freeipa.api.client.internal.FreeIpaApiClientConfig;
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;
import com.sequenceiq.sdx.client.internal.SdxApiClientConfig;
import com.sequenceiq.sdx.client.internal.SdxApiClientParams;

@Configuration(proxyBeanMethods = false)
@Import({CloudbreakApiClientConfig.class, SdxApiClientConfig.class, FreeIpaApiClientConfig.class})
public class MaintenanceResourceOwnerClientConfig {

    private final MaintenancePeerServiceUrlResolver peerServiceUrlResolver;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Inject
    public MaintenanceResourceOwnerClientConfig(MaintenancePeerServiceUrlResolver peerServiceUrlResolver) {
        this.peerServiceUrlResolver = peerServiceUrlResolver;
    }

    @Bean
    public CloudbreakApiClientParams cloudbreakApiClientParams() throws ServiceAddressResolvingException {
        return new CloudbreakApiClientParams(
                restDebug,
                certificateValidation,
                ignorePreValidation,
                peerServiceUrlResolver.resolveBaseUrl(MaintenanceSubmitterService.CLOUDBREAK));
    }

    @Bean
    public SdxApiClientParams sdxApiClientParams() throws ServiceAddressResolvingException {
        return new SdxApiClientParams(
                restDebug,
                certificateValidation,
                ignorePreValidation,
                peerServiceUrlResolver.resolveBaseUrl(MaintenanceSubmitterService.DATALAKE));
    }
}
