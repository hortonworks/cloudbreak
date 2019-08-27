package com.sequenceiq.redbeams.client.internal;

import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.UserCrnClientRequestFilter;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.redbeams.api.RedbeamsApi;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

@Configuration
public class RedbeamsApiClientConfig {

    private final UserCrnClientRequestFilter userCrnClientRequestFilter;

    private final ClientTracingFeature clientTracingFeature;

    public RedbeamsApiClientConfig(UserCrnClientRequestFilter userCrnClientRequestFilter, ClientTracingFeature clientTracingFeature) {
        this.userCrnClientRequestFilter = userCrnClientRequestFilter;
        this.clientTracingFeature = clientTracingFeature;
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    public WebTarget redbeamsApiClientWebTarget(RedbeamsApiClientParams redbeamsApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(redbeamsApiClientParams.getServiceUrl())
                .withCertificateValidation(redbeamsApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(redbeamsApiClientParams.isIgnorePreValidation())
                .withDebug(redbeamsApiClientParams.isRestDebug())
                .withClientRequestFilter(userCrnClientRequestFilter)
                .withTracer(clientTracingFeature)
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
    DatabaseServerV4Endpoint databaseServerV4Endpoint(WebTarget redbeamsApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(redbeamsApiClientWebTarget, DatabaseServerV4Endpoint.class);
    }
}
