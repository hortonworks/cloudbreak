package com.sequenceiq.maintenance.client.internal;

import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.maintenance.api.MaintenanceApi;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.v1.task.endpoint.MaintenanceWindowTaskEndpoint;

@Configuration
public class MaintenanceApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    public MaintenanceApiClientConfig(ApiClientRequestFilter apiClientRequestFilter) {
        this.apiClientRequestFilter = apiClientRequestFilter;
    }

    @Bean
    @ConditionalOnBean(MaintenanceApiClientParams.class)
    public WebTarget maintenanceApiClientWebTarget(MaintenanceApiClientParams maintenanceApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(maintenanceApiClientParams.getServiceUrl())
                .withCertificateValidation(maintenanceApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(maintenanceApiClientParams.isIgnorePreValidation())
                .withDebug(maintenanceApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(MaintenanceApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "maintenanceApiClientWebTarget")
    MaintenanceWindowTaskEndpoint maintenanceWindowTaskEndpoint(WebTarget maintenanceApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(maintenanceApiClientWebTarget, MaintenanceWindowTaskEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "maintenanceApiClientWebTarget")
    MaintenanceWindowScheduleInternalEndpoint maintenanceWindowScheduleInternalEndpoint(WebTarget maintenanceApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(maintenanceApiClientWebTarget, MaintenanceWindowScheduleInternalEndpoint.class);
    }
}
