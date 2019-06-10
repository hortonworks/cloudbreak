package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service family for HTTPS-based services.
 */
public class HttpsServiceFamily implements ServiceFamily<HttpsServiceEndpoint> {

    /**
     * The default port for the service.
     */
    private final int defaultPort;

    /**
     * The optional known service identifier for tunneling.
     */
    private final KnownServiceIdentifier knownServiceIdentifier;

    /**
     * Creates an HTTPS service family with the specified parameters.
     *
     * @param defaultPort the default port for the service
     */
    public HttpsServiceFamily(int defaultPort) {
        this(defaultPort, null);
    }

    /**
     * Creates an HTTPS service family with the specified parameters.
     *
     * @param defaultPort            the default port for the service
     * @param knownServiceIdentifier the optional known service identifier for tunneling
     */
    protected HttpsServiceFamily(int defaultPort, @Nullable KnownServiceIdentifier knownServiceIdentifier) {
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

    @Nonnull
    @Override
    public HttpsServiceEndpoint getServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, int port) {
        return new HttpsServiceEndpoint(hostEndpoint, port);
    }
}
