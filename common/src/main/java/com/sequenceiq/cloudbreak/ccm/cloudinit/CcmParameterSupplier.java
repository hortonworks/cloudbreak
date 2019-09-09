package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;

/**
 * Supplier of CCM parameters.
 */
public interface CcmParameterSupplier {

    /**
     * Returns optional CCM parameters based on the specified context. The resulting CCM parameters may be empty
     * for a variety of reasons, including that CCM is disabled in the environment, or that the specified service
     * port map is {@code null} or empty.
     *
     *
     * @param actorCrn the actor CRN
     * @param accountId the account ID
     * @param keyId the key ID under which the private key was registered with CCM
     * @param tunneledServicePorts the map from known service identifiers to the ports to be tunneled
     * @return the CCM parameters
     */
    default Optional<CcmParameters> getCcmParameters(@Nonnull String actorCrn, @Nonnull String accountId, @Nonnull String keyId,
            @Nullable Map<KnownServiceIdentifier, Integer> tunneledServicePorts) {
        return getCcmParameters(getBaseCcmParameters(actorCrn, accountId, keyId).orElse(null), tunneledServicePorts);
    }

    /**
     * Returns optional CCM parameters based on the specified context. The CCM parameters may be empty
     * for a variety of reasons, including that CCM is disabled in the environment. The
     *
     *
     * @param actorCrn the actor CRN
     * @param accountId the account ID
     * @param keyId the key ID under which the private key was registered with CCM
     * @return the CCM parameters
     */
    Optional<CcmParameters> getBaseCcmParameters(@Nonnull String actorCrn, @Nonnull String accountId, @Nonnull String keyId);

    /**
     * Returns optional CCM parameters based on the specified context. The resulting CCM parameters will be empty
     * if the specified CCM parameters are {@code null} or the specified service port mapping is {@code null} or empty. Otherwise,
     * the resulting CCM parameters will have the same server and instance parameters as the specified CCM parameters, and
     * tunnel parameters based on the specified service port mapping.
     *
     * @param baseCcmParameters the CCM parameters
     * @param tunneledServicePorts the map from known service identifiers to the ports to be tunneled
     * @return the optional result CCM parameters
     */
    Optional<CcmParameters> getCcmParameters(@Nullable CcmParameters baseCcmParameters, @Nullable Map<KnownServiceIdentifier, Integer> tunneledServicePorts);
}
