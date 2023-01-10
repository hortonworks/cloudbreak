package com.sequenceiq.consumption.client.internal;

import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.consumption.api.v1.ConsumptionApi;

@Configuration
public class ConsumptionApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    public ConsumptionApiClientConfig(ApiClientRequestFilter apiClientRequestFilter) {
        this.apiClientRequestFilter = apiClientRequestFilter;
    }

    @Bean
    @ConditionalOnBean(ConsumptionApiClientParams.class)
    public WebTarget consumptionApiClientWebTarget(ConsumptionApiClientParams consumptionApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(consumptionApiClientParams.getServiceUrl())
                .withCertificateValidation(consumptionApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(consumptionApiClientParams.isIgnorePreValidation())
                .withDebug(consumptionApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(ConsumptionApi.API_ROOT_CONTEXT)
                .build();
    }

}
