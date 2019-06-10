package com.sequenceiq.cloudbreak.ccm.endpoint;

import javax.annotation.Nonnull;

/**
 * Lookup service for service endpoints.
 */
public interface ServiceEndpointFinder {

    /**
     * Returns the service endpoint for the specified request.
     *
     * @param serviceEndpointRequest the service endpoint request
     * @return the service endpoint for the specified request
     * @throws ServiceEndpointLookupException if an exception occurs
     * @throws InterruptedException           if the lookup is interrupted
     */
    @Nonnull
    <T extends ServiceEndpoint> T getServiceEndpoint(@Nonnull ServiceEndpointRequest<T> serviceEndpointRequest)
            throws ServiceEndpointLookupException, InterruptedException;
}
