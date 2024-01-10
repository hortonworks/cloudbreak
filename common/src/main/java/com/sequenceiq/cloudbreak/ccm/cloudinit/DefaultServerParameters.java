package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.io.Serializable;
import java.util.Objects;

import jakarta.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;

/**
 * Default {@link ServerParameters} implementation.
 */
public class DefaultServerParameters implements ServerParameters, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The SSH service endpoint for connecting to CCM.
     */
    private final ServiceEndpoint ccmSshServiceEndpoint;

    /**
     * Tthe public key for CCM, which allows the client to verify that it is talking to a valid CCM.
     */
    private final String ccmPublicKey;

    private final String minaSshdServiceId;

    /**
     * Creates default server parameters with the specified parameters.
     *  @param ccmSshServiceEndpoint the SSH service endpoint for connecting to CCM
     * @param ccmPublicKey          the public key for CCM, which allows the client to verify that it is talking to a valid CCM
     * @param minaSshdServiceId     minaSshdServiceId
     */
    public DefaultServerParameters(ServiceEndpoint ccmSshServiceEndpoint, String ccmPublicKey, String minaSshdServiceId) {
        this.ccmSshServiceEndpoint = Objects.requireNonNull(ccmSshServiceEndpoint, "ccmSshServiceEndpoint is null");
        this.ccmPublicKey = Objects.requireNonNull(ccmPublicKey, "ccmPublicKey is null");
        this.minaSshdServiceId = minaSshdServiceId;
    }

    @Nonnull
    @Override
    public ServiceEndpoint getCcmSshServiceEndpoint() {
        return ccmSshServiceEndpoint;
    }

    @Nonnull
    @Override
    public String getCcmPublicKey() {
        return ccmPublicKey;
    }

    @Nonnull
    @Override
    public String getMinaSshdServiceId() {
        return minaSshdServiceId;
    }
}
