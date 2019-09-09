package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

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

    /**
     * Creates default server parameters with the specified parameters.
     *
     * @param ccmSshServiceEndpoint the SSH service endpoint for connecting to CCM
     * @param ccmPublicKey          tthe public key for CCM, which allows the client to verify that it is talking to a valid CCM
     */
    public DefaultServerParameters(ServiceEndpoint ccmSshServiceEndpoint, String ccmPublicKey) {
        this.ccmSshServiceEndpoint = Objects.requireNonNull(ccmSshServiceEndpoint, "ccmSshServiceEndpoint is null");
        this.ccmPublicKey = Objects.requireNonNull(ccmPublicKey, "ccmPublicKey is null");
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
}
