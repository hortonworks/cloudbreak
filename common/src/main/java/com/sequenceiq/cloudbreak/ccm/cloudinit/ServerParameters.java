package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;

/**
 * Holds CCM server parameters, which specify how to connect to CCM.
 */
public interface ServerParameters {

    /**
     * Returns the SSH service endpoint for connecting to CCM.
     *
     * @return the SSH service endpoint for connecting to CCM
     */
    @Nonnull
    ServiceEndpoint getCcmSshServiceEndpoint();

    /**
     * Returns the public key for CCM, which allows the client to verify that it is
     * talking to a valid CCM.
     *
     * @return the public key for CCM, which allows the client to verify that it is
     * talking to a valid CCM
     */
    @Nonnull
    String getCcmPublicKey();

    @Nonnull
    String getMinaSshdServiceId();

    /**
     * Adds keys and values corresponding to the CCM parameters to the specified template model.
     *
     * @param model the template model map
     */
    default void addToTemplateModel(Map<String, Object> model) {
        ServiceEndpoint ccmSshServiceEndpoint = getCcmSshServiceEndpoint();
        String ccmHostAddressString = ccmSshServiceEndpoint.getHostEndpoint().getHostAddressString();
        model.put(CcmParameterConstants.CCM_HOST_KEY, ccmHostAddressString);
        int ccmSshPort = ccmSshServiceEndpoint.getPort().orElse(CcmParameterConstants.DEFAULT_CCM_SSH_PORT);
        model.put(CcmParameterConstants.CCM_SSH_PORT_KEY, ccmSshPort);
        // Put public key into known_hosts format
        String formattedPublicKey = String.format(
                CcmParameterConstants.CCM_PUBLIC_KEY_FORMAT,
                ccmHostAddressString, ccmSshPort, getCcmPublicKey());
        model.put(CcmParameterConstants.CCM_PUBLIC_KEY_KEY,
                BaseEncoding.base64().encode(formattedPublicKey.getBytes(StandardCharsets.UTF_8)));
    }
}
