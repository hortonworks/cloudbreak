package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base class for service endpoint implementations.
 */
public class BaseServiceEndpoint implements ServiceEndpoint, Serializable {

    /**
     * The dummy port for URI construction.
     */
    private static final int DUMMY_PORT = -1;

    private static final long serialVersionUID = 1L;

    /**
     * The host endpoint.
     */
    private final HostEndpoint hostEndpoint;

    /**
     * The optional port.
     */
    private final Integer port;

    /**
     * The optional URI for connecting to the endpoint.
     */
    private final URI uri;

    /**
     * Creates a base service endpoint with the specified parameters.
     *
     * @param hostEndpoint the host endpoint
     */
    public BaseServiceEndpoint(@Nonnull HostEndpoint hostEndpoint) {
        this(hostEndpoint, null, null);
    }

    /**
     * Creates a base service endpoint with the specified parameters.
     *
     * @param hostEndpoint the host endpoint
     * @param port         the optional port
     * @param uri          the optional URI
     */
    public BaseServiceEndpoint(@Nonnull HostEndpoint hostEndpoint, @Nullable Integer port, @Nullable URI uri) {
        this.hostEndpoint =
                Objects.requireNonNull(hostEndpoint, "hostEndpoint is null");
        this.port = port;
        this.uri = uri;
    }

    /**
     * Returns a default URI for the specified parameters.
     *
     * @param scheme       the scheme
     * @param hostEndpoint the host endpoint
     * @param port         the optional port
     * @return a default URI for the specified parameters
     */
    protected static URI getDefaultURI(@Nonnull String scheme, @Nonnull HostEndpoint hostEndpoint, @Nullable Integer port) {
        try {
            return new URI(Objects.requireNonNull(scheme, "scheme is null"), null,
                    Objects.requireNonNull(hostEndpoint, "hostEndpoint is null").getHostAddressString(),
                    (port == null) ? DUMMY_PORT : port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot build URI for scheme: " + scheme + ", hostEndpoint: " + hostEndpoint + ", port: " + port, e);
        }
    }

    @Override
    public HostEndpoint getHostEndpoint() {
        return hostEndpoint;
    }

    @Override
    public Optional<Integer> getPort() {
        return Optional.ofNullable(port);
    }

    @Override
    public Optional<URI> getURI() {
        return Optional.ofNullable(uri);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BaseServiceEndpoint that = (BaseServiceEndpoint) o;

        if (!hostEndpoint.equals(that.hostEndpoint)) {
            return false;
        }
        if (!Objects.equals(port, that.port)) {
            return false;
        }
        return Objects.equals(uri, that.uri);

    }

    @Override
    public int hashCode() {
        return Objects.hash(hostEndpoint, port, uri);
    }

    @Override
    public String toString() {
        return "BaseServiceEndpoint{"
                + "hostEndpoint=" + hostEndpoint
                + ((port == null) ? "" : ", port=" + port)
                + ((uri == null) ? "" : ", uri=" + uri)
                + '}';
    }
}
