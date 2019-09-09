package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default {@link InstanceParameters} implementation.
 */
public class DefaultInstanceParameters implements InstanceParameters, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The optional tunnel initiator ID, which uniquely identifies the instance to CCM.
     */
    private final String tunnelInitiatorId;

    /**
     * The key ID under which the private key was registered with CCM.
     */
    private final String keyId;

    /**
     * The enciphered private key, which CCM uses to authenticate the instance.
     */
    private final String encipheredPrivateKey;

    /**
     * Creates default CCM instance parameters with the specified parameters.
     *
     * @param tunnelInitiatorId    the optional tunnel initiator ID, which uniquely identifies the instance to CCM
     * @param keyId                the key ID under which the private key was registered with CCM
     * @param encipheredPrivateKey the enciphered private key, which CCM uses to authenticate the instance
     */
    public DefaultInstanceParameters(@Nullable String tunnelInitiatorId, String keyId, @Nonnull String encipheredPrivateKey) {
        this.tunnelInitiatorId = tunnelInitiatorId;
        this.keyId = Objects.requireNonNull(keyId, "keyId is null");
        this.encipheredPrivateKey = Objects.requireNonNull(encipheredPrivateKey, "encipheredPrivateKey is null");
    }

    @Nonnull
    @Override
    public Optional<String> getTunnelInitiatorId() {
        return Optional.ofNullable(tunnelInitiatorId);
    }

    @Nonnull
    @Override
    public String getKeyId() {
        return keyId;
    }

    @Nonnull
    @Override
    public String getEncipheredPrivateKey() {
        return encipheredPrivateKey;
    }
}
