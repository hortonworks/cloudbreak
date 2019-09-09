package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.io.BaseEncoding;

/**
 * Holds instance parameters, which specify how an instance identifies itself to CCM.
 */
public interface InstanceParameters {

    /**
     * Returns the optional tunnel initiator ID, which uniquely identifies the instance to CCM.
     * If absent, the provider-specific instance ID will be used instead.
     *
     * @return the optional tunnel initiator ID, which uniquely identifies the instance to CCM
     */
    @Nonnull
    Optional<String> getTunnelInitiatorId();

    /**
     * Returns the key ID under which the private key was registered with CCM.
     *
     * @return the key ID under which the private key was registered with CCM
     */
    @Nonnull
    String getKeyId();

    /**
     * Returns the enciphered private key, which CCM uses to authenticate the instance.
     *
     * @return the enciphered private key, which CCM uses to authenticate the instance
     */
    @Nonnull
    String getEncipheredPrivateKey();

    /**
     * Adds keys and values corresponding to the CCM parameters to the specified template model.
     *
     * @param model the template model map
     */
    default void addToTemplateModel(Map<String, Object> model) {
        getTunnelInitiatorId().ifPresent(s -> model.put(CcmParameterConstants.TUNNEL_INITIATOR_ID_KEY, s));
        model.put(CcmParameterConstants.KEY_ID_KEY, getKeyId());
        model.put(CcmParameterConstants.ENCIPHERED_PRIVATE_KEY_KEY,
                BaseEncoding.base64().encode(getEncipheredPrivateKey().getBytes(StandardCharsets.UTF_8)));
    }
}
