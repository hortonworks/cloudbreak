package com.sequenceiq.cloudbreak.ccm.termination;

import javax.annotation.Nonnull;

/**
 * Listener for CCM resource termination events.
 */
public interface CcmResourceTerminationListener {

    /**
     * Deregisters the CCM key associated with a resource when the resource is terminated.
     *
     * @param actorCrn the actor CRN
     * @param accountId the account ID
     * @param keyId the key ID associated with the resource
     */
    void deregisterCcmSshTunnelingKey(@Nonnull String actorCrn, @Nonnull String accountId, @Nonnull String keyId);
}
