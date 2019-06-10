package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Represents a family of services.
 *
 * @param <T> the type of service endpoint used by the family
 */
public interface ServiceFamily<T extends ServiceEndpoint> {

    /**
     * The service family for nginx web servers on gateways.
     */
    ServiceFamily<HttpsServiceEndpoint> GATEWAY =
            new HttpsServiceFamily(9443, KnownServiceIdentifier.GATEWAY);

    /**
     * The service family for Apache Knox proxy servers.
     */
    ServiceFamily<HttpsServiceEndpoint> KNOX =
            new HttpsServiceFamily(8443, KnownServiceIdentifier.KNOX);

    /**
     * Returns a custom HTTPS service family.
     *
     * @param defaultPort the default port
     * @return the custom service family
     */
    static ServiceFamily<HttpsServiceEndpoint> customHttpsServiceFamily(int defaultPort) {
        return new HttpsServiceFamily(defaultPort);
    }

    /**
     * Returns the default port for the service.
     *
     * @return the default port for the service
     */
    int getDefaultPort();

    /**
     * Returns the optional known service identifier for tunneling.
     *
     * @return the optional known service identifier for tunneling
     */
    @Nonnull
    Optional<KnownServiceIdentifier> getKnownServiceIdentifier();

    /**
     * Returns a service endpoint for the service.
     *
     * @param hostEndpoint the host endpoint
     * @param port         the port
     * @return a service endpoint for the service
     */
    @Nonnull
    T getServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, int port);
}
