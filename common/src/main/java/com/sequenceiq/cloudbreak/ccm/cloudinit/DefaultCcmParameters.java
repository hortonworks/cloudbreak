package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

/**
 * Default {@link CcmParameters} implementation.
 */
public class DefaultCcmParameters implements CcmParameters, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The CCM server parameters, which specify how to connect to CCM.
     */
    private final ServerParameters serverParameters;

    /**
     * The instance parameters, which specify how the instance identifies itself to CCM.
     */
    private final InstanceParameters instanceParameters;

    /**
     * The list of CCM tunnel parameters, each of which specifies how to register a tunnel for a single service with CCM.
     */
    private final List<TunnelParameters> tunnelParameters;

    /**
     * Creates default CCM parameters with the specified parameters.
     *
     * @param serverParameters   the CCM server parameters, which specify how to connect to CCM
     * @param instanceParameters the instance parameters, which specify how the instance identifies itself to CCM
     * @param tunnelParameters   the list of CCM tunnel parameters, each of which specifies how to register a tunnel for a single service with CCM
     */
    public DefaultCcmParameters(@Nonnull ServerParameters serverParameters, @Nonnull InstanceParameters instanceParameters,
            @Nonnull List<TunnelParameters> tunnelParameters) {
        this.serverParameters = Objects.requireNonNull(serverParameters, "serverParameters is null");
        this.instanceParameters = Objects.requireNonNull(instanceParameters, "instanceParameters is null");
        this.tunnelParameters = ImmutableList.copyOf(Objects.requireNonNull(tunnelParameters, "tunnelParameters is null"));
    }

    @Override
    public ServerParameters getServerParameters() {
        return serverParameters;
    }

    @Override
    public InstanceParameters getInstanceParameters() {
        return instanceParameters;
    }

    @Override
    public List<TunnelParameters> getTunnelParameters() {
        return tunnelParameters;
    }
}
