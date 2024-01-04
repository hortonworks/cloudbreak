package com.sequenceiq.cloudbreak.ccm.cloudinit;

import jakarta.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.exception.CcmException;

/**
 * Enables remapping of CCMV1 keys to new IDs.
 */
public interface CCMV1KeyRemapper {
    /**
     * Remaps the CCMV1 key mapped to the given ID to the new ID.
     *
     *
     * @param actorCrn the actor CRN
     * @param accountId the account ID
     * @param originalKeyId the key ID under which the private key was registered with CCM
     * @param newKeyId      the new key ID to map the key to with CCM
     */
    void remapKey(@Nonnull String actorCrn, @Nonnull String accountId, @Nonnull String originalKeyId, @Nonnull String newKeyId)
            throws CcmException, InterruptedException;
}
