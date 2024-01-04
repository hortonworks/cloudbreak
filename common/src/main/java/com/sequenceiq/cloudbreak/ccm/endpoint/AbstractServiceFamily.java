package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Abstract service family implementation.
 *
 * @param <T> the type of service endpoint used by the family
 */
public abstract class AbstractServiceFamily<T extends ServiceEndpoint> implements ServiceFamily<T> {

    /**
     * The default port for the service.
     */
    private final int defaultPort;

    /**
     * The optional known service identifier for tunneling.
     */
    private final KnownServiceIdentifier knownServiceIdentifier;

    /**
     * Creates an abstract service family with the specified parameters.
     *
     * @param defaultPort the default port for the service
     */
    public AbstractServiceFamily(int defaultPort) {
        this(defaultPort, null);
    }

    /**
     * Creates an abstract service family with the specified parameters.
     *
     * @param defaultPort            the default port for the service
     * @param knownServiceIdentifier the optional known service identifier for tunneling
     */
    protected AbstractServiceFamily(int defaultPort, @Nullable KnownServiceIdentifier knownServiceIdentifier) {
        this.defaultPort = defaultPort;
        this.knownServiceIdentifier = knownServiceIdentifier;
    }

    @Override
    public int getDefaultPort() {
        return defaultPort;
    }

    @Nonnull
    @Override
    public Optional<KnownServiceIdentifier> getKnownServiceIdentifier() {
        return Optional.ofNullable(knownServiceIdentifier);
    }
}
