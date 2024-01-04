package com.sequenceiq.cloudbreak.ccm.endpoint;

import jakarta.annotation.Nonnull;

/**
 * Lookup service for service endpoints.
 */
public interface ServiceEndpointFinder {

    /**
     * Returns the service endpoint for the specified request.
     *
     * @param serviceEndpointRequest the service endpoint request
     * @return the service endpoint for the specified request, which contains a host, port, and optional URI for reaching
     * the service, possibly through an intermediary such as an SSH tunnel
     * @throws ServiceEndpointLookupException if an exception occurs
     * @throws InterruptedException           if the lookup is interrupted
     */
    @Nonnull
    <T extends ServiceEndpoint> T getServiceEndpoint(@Nonnull ServiceEndpointRequest<T> serviceEndpointRequest)
            throws ServiceEndpointLookupException, InterruptedException;
}
