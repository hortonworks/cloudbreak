package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.util.Map;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;

/**
 * Holds tunnel parameters, each of which specifies how to register a tunnel
 * for a single service with CCM.
 */
public interface TunnelParameters {

    /**
     * Returns the known service identifier for the service. Only known services can be registered,
     * because CCM only recognizes a fixed set of known services.
     *
     * @return the known service identifier for the service
     */
    @Nonnull
    KnownServiceIdentifier getKnownServiceIdentifier();

    /**
     * Returns the port for which the tunnel is being registered.
     */
    int getPort();

    /**
     * Adds keys and values corresponding to the CCM parameters to the specified template model.
     *
     * @param model the template model map
     */
    default void addToTemplateModel(Map<String, Object> model) {
        String knownServiceName = getKnownServiceIdentifier().name().toLowerCase();
        String portKey = String.format(CcmParameterConstants.SERVICE_PORT_KEY_FORMAT, knownServiceName.charAt(0), knownServiceName.substring(1));
        model.put(portKey, getPort());
    }
}
