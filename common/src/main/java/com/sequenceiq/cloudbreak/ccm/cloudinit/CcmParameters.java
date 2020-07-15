package com.sequenceiq.cloudbreak.ccm.cloudinit;

import static com.sequenceiq.common.api.type.InstanceGroupType.isGateway;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sequenceiq.common.api.type.InstanceGroupType;

/**
 * Holds CCM cloud-init parameters, which encapsulate the user data required to configure an instance to
 * connect to CCM, identify itself to CCM, and register tunnels with CCM.
 */
public interface CcmParameters {

    /**
     * Adds keys and values corresponding to the CCM parameters to the specified template model.
     *
     * @param type          instance group type, CCM is only enabled to GATEWAY type
     * @param ccmParameters paramters for CCM
     * @param model         the template model map
     */
    static void addToTemplateModel(InstanceGroupType type, @Nullable CcmParameters ccmParameters, @Nonnull Map<String, Object> model) {
        if (ccmParameters == null || !isGateway(type)) {
            model.put(CcmParameterConstants.CCM_ENABLED_KEY, Boolean.FALSE);
        } else {
            model.put(CcmParameterConstants.CCM_ENABLED_KEY, Boolean.TRUE);
            ccmParameters.addToTemplateModel(model);
        }
    }

    /**
     * Returns the CCM server parameters, which specify how to connect to CCM.
     *
     * @return the CCM server parameters, which specify how to connect to CCM
     */
    ServerParameters getServerParameters();

    /**
     * Returns the instance parameters, which specify how the instance identifies itself to CCM.
     *
     * @return the instance parameters, which specify how the instance identifies itself to CCM
     */
    InstanceParameters getInstanceParameters();

    /**
     * Returns the list of CCM tunnel parameters, each of which specifies how to register a tunnel
     * for a single service with CCM.
     *
     * @return the list of CCM tunnel parameters, each of which specifies how to register a tunnel
     * for a single service with CCM
     */
    List<TunnelParameters> getTunnelParameters();

    /**
     * Adds keys and values corresponding to the CCM parameters to the specified template model.
     *
     * @param model the template model map
     */
    default void addToTemplateModel(@Nonnull Map<String, Object> model) {
        getServerParameters().addToTemplateModel(model);
        getInstanceParameters().addToTemplateModel(model);
        for (TunnelParameters tunnelParameters : getTunnelParameters()) {
            tunnelParameters.addToTemplateModel(model);
        }
    }
}
