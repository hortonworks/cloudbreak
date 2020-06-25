package com.sequenceiq.freeipa.configuration;

import com.sequenceiq.sdx.client.internal.SdxApiClientParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import javax.inject.Named;

@Configuration
public class SdxApiConfig {
    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cert.ignorePreValidation:false}")
    private boolean ignorePreValidation;

    @Inject
    @Named("sdxServerUrl")
    private String sdxServerUrl;

    @Bean
    public SdxApiClientParams sdxApiClientParams() {
        return new SdxApiClientParams(restDebug, certificateValidation, ignorePreValidation, sdxServerUrl);
    }
}
