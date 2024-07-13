package com.sequenceiq.redbeams.client.internal;

import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.redbeams.api.RedbeamsApi;
import com.sequenceiq.redbeams.api.endpoint.v1.RedBeamsFlowEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.progress.ProgressV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;

@Configuration
public class RedbeamsApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    public RedbeamsApiClientConfig(ApiClientRequestFilter apiClientRequestFilter) {
        this.apiClientRequestFilter = apiClientRequestFilter;
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    public WebTarget redbeamsApiClientWebTarget(RedbeamsApiClientParams redbeamsApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(redbeamsApiClientParams.getServiceUrl())
                .withCertificateValidation(redbeamsApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(redbeamsApiClientParams.isIgnorePreValidation())
                .withDebug(redbeamsApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(RedbeamsApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    DatabaseV4Endpoint databaseV4Endpoint(WebTarget redbeamsApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(redbeamsApiClientWebTarget, DatabaseV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    SupportV4Endpoint supportV4Endpoint(WebTarget redbeamsApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(redbeamsApiClientWebTarget, SupportV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    DatabaseServerV4Endpoint databaseServerV4Endpoint(WebTarget redbeamsApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(redbeamsApiClientWebTarget, DatabaseServerV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    ProgressV4Endpoint progressDatabaseServerV4Endpoint(WebTarget redbeamsApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(redbeamsApiClientWebTarget, ProgressV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    OperationV4Endpoint operationDatabaseServerV4Endpoint(WebTarget redbeamsApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(redbeamsApiClientWebTarget, OperationV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    RedBeamsFlowEndpoint redBeamsV1FlowEndpoint(WebTarget redbeamsApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(redbeamsApiClientWebTarget, RedBeamsFlowEndpoint.class);
    }
}
