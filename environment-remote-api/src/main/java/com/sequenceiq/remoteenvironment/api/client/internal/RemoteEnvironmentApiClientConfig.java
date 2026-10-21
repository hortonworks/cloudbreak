package com.sequenceiq.remoteenvironment.api.client.internal;

import jakarta.ws.rs.client.WebTarget;

import org.springframework.context.annotation.Bean;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.remoteenvironment.api.RemoteEnvironmentApi;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentV2Endpoint;

/**
 * Remote environment API client beans. Intentionally not a {@code @Configuration}, so component scanning does not pick it up:
 * import it with {@code @Import} from the configuration that defines the {@link RemoteEnvironmentApiClientParams} bean.
 * This keeps bean creation independent of configuration class processing order.
 */
public class RemoteEnvironmentApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    public RemoteEnvironmentApiClientConfig(ApiClientRequestFilter apiClientRequestFilter) {
        this.apiClientRequestFilter = apiClientRequestFilter;
    }

    @Bean
    public WebTarget remoteEnvironmentApiClientWebTarget(RemoteEnvironmentApiClientParams remoteEnvironmentApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(remoteEnvironmentApiClientParams.getServiceUrl())
                .withCertificateValidation(remoteEnvironmentApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(remoteEnvironmentApiClientParams.isIgnorePreValidation())
                .withDebug(remoteEnvironmentApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(RemoteEnvironmentApi.API_ROOT_CONTEXT)
                .build();
    }

    @Bean
    RemoteEnvironmentEndpoint remoteEnvironmentEndpoint(WebTarget remoteEnvironmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(remoteEnvironmentApiClientWebTarget, RemoteEnvironmentEndpoint.class);
    }

    @Bean
    RemoteEnvironmentV2Endpoint remoteEnvironmentV2Endpoint(WebTarget remoteEnvironmentApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(remoteEnvironmentApiClientWebTarget, RemoteEnvironmentV2Endpoint.class);
    }
}
