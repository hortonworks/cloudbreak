package com.sequenceiq.cloudbreak.ccmimpl.endpoint;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.exception.CcmException;

/**
 * Supplier of minasshd gRPC service endpoints.
 */
public interface MinaSshdGrpcEndpointSupplier {

    /**
     * Returns the minasshd gRPC service endpoint.
     *
     * @param actorCrn the actor CRN
     * @param accountId the account ID
     * @return the minasshd gRPC service endpoint
     */
    @Nonnull
    ServiceEndpoint getMinaSshdGrpcServiceEndpoint(@Nonnull String actorCrn, @Nonnull String accountId) throws CcmException;
}
