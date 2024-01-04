package com.sequenceiq.cloudbreak.ccm.endpoint;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * HTTPS endpoint.
 */
public class HttpsServiceEndpoint extends BaseServiceEndpoint {

    /**
     * The URI scheme.
     */
    public static final String SCHEME = "https";

    /**
     * The default HTTPS port.
     */
    public static final int DEFAULT_HTTPS_PORT = 443;

    private static final long serialVersionUID = 1L;

    /**
     * Creates an HTTPS endpoint with the specified parameters.
     *
     * @param hostEndpoint the host endpoint
     */
    public HttpsServiceEndpoint(@Nonnull HostEndpoint hostEndpoint) {
        this(hostEndpoint, (Integer) null);
    }

    /**
     * Creates an HTTPS endpoint with the specified parameters.
     *
     * @param hostEndpoint the host endpoint
     * @param portString   the port string
     */
    public HttpsServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, @Nullable String portString) {
        this(hostEndpoint, (portString == null) ? null : Integer.valueOf(portString));
    }

    /**
     * Creates an HTTPS endpoint with the specified parameters.
     *
     * @param hostEndpoint the host endpoint
     * @param port         the optional port
     */
    public HttpsServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, @Nullable Integer port) {
        super(hostEndpoint,
                (port != null) ? port : DEFAULT_HTTPS_PORT,
                getDefaultURI(SCHEME, hostEndpoint, port));
    }
}
