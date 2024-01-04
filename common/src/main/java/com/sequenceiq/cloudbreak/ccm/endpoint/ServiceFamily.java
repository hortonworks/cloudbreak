package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.util.Optional;

import jakarta.annotation.Nonnull;

/**
 * Represents a family of services.
 *
 * @param <T> the type of service endpoint used by the family
 */
public interface ServiceFamily<T extends ServiceEndpoint> {

    /**
     * Returns the default port for the service.
     *
     * @return the default port for the service
     */
    int getDefaultPort();

    /**
     * Returns the optional known service identifier for tunneling.
     *
     * @return the optional known service identifier for tunneling
     */
    @Nonnull
    Optional<KnownServiceIdentifier> getKnownServiceIdentifier();

    /**
     * Returns a service endpoint for the service.
     *
     * @param hostEndpoint the host endpoint
     * @param port         the port
     * @return a service endpoint for the service
     */
    @Nonnull
    T getServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, int port);
}
