package com.sequenceiq.redbeams.client;

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

@Configuration
public class RedbeamsApiClientConfig {

    private final UserCrnClientRequestFilter userCrnClientRequestFilter;

    public RedbeamsApiClientConfig(UserCrnClientRequestFilter userCrnClientRequestFilter) {
        this.userCrnClientRequestFilter = userCrnClientRequestFilter;
    }

    @Bean
    @ConditionalOnBean(RedbeamsApiClientParams.class)
    public WebTarget redbeamsApiClientWebTarget(RedbeamsApiClientParams redbeamsApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(redbeamsApiClientParams.getServiceUrl())
                .withCertificateValidation(redbeamsApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(redbeamsApiClientParams.isIgnorePreValidation())
                .withDebug(redbeamsApiClientParams.isRestDebug())
                .withClientRequestFilter(userCrnClientRequestFilter)
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
