package com.sequenceiq.cloudbreak.ccm.cloudinit;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;

/**
 * Default {@link TunnelParameters} implementation.
 */
public class DefaultTunnelParameters implements TunnelParameters, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The known service identifier for the service.
     */
    private final KnownServiceIdentifier knownServiceIdentifier;

    /**
     * The port for which the tunnel is being registered.
     */
    private final int port;

    /**
     * Creates default tunnel parameters with the specified parameters.
     *
     * @param knownServiceIdentifier the known service identifier for the service
     * @param port                   the port for which the tunnel is being registered
     */
    public DefaultTunnelParameters(@Nonnull KnownServiceIdentifier knownServiceIdentifier, int port) {
        this.knownServiceIdentifier = Objects.requireNonNull(knownServiceIdentifier, "knownServiceIdentifier is null");
        this.port = port;
    }

    @Nonnull
    @Override
    public KnownServiceIdentifier getKnownServiceIdentifier() {
        return knownServiceIdentifier;
    }

    @Override
    public int getPort() {
        return port;
    }
}
