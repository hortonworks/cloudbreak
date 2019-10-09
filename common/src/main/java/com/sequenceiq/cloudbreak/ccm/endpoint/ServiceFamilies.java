package com.sequenceiq.cloudbreak.ccm.endpoint;

import javax.annotation.Nonnull;

/**
 * Provides utilities for dealing with service families.
 */
public class ServiceFamilies {

    /**
     * The service family for nginx web servers on gateways.
     */
    public static final ServiceFamily<HttpsServiceEndpoint> GATEWAY =
            new AbstractServiceFamily<>(9443, KnownServiceIdentifier.GATEWAY) {
                @Nonnull
                @Override
                public HttpsServiceEndpoint getServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, int port) {
                    return new HttpsServiceEndpoint(hostEndpoint, port);
                }
            };

    /**
     * The service family for Apache Knox proxy servers.
     */
    public static final ServiceFamily<HttpsServiceEndpoint> KNOX =
            new AbstractServiceFamily<>(443, KnownServiceIdentifier.KNOX) {
                @Nonnull
                @Override
                public HttpsServiceEndpoint getServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, int port) {
                    return new HttpsServiceEndpoint(hostEndpoint, port);
                }
            };

    /**
     * Private constructor to prevent instantiation.
     */
    private ServiceFamilies() {
    }
}
