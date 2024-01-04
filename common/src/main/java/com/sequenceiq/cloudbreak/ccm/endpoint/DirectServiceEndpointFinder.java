package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.util.Optional;

import jakarta.annotation.Nonnull;

/**
 * An endpoint finder that returns an endpoint based on a direct connection to the target instance on the
 * default port for the requested service family.
 */
public class DirectServiceEndpointFinder implements ServiceEndpointFinder {

    @Nonnull
    @Override
    public <T extends ServiceEndpoint> T getServiceEndpoint(@Nonnull ServiceEndpointRequest<T> serviceEndpointRequest) {

        final Optional<HostEndpoint> hostEndpoint = serviceEndpointRequest.getTargetInstance().getHostEndpoint();
        if (!hostEndpoint.isPresent()) {
            throw new IllegalArgumentException("No host endpoint provided");
        }

        ServiceFamily<T> serviceFamily = serviceEndpointRequest.getServiceFamily();
        int port = serviceEndpointRequest.getPort().orElse(serviceFamily.getDefaultPort());

        return serviceFamily.getServiceEndpoint(hostEndpoint.get(), port);
    }
}
