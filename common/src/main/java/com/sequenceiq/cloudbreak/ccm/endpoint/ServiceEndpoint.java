package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a service endpoint.
 */
public interface ServiceEndpoint {

    /**
     * Returns the host endpoint.
     *
     * @return the host endpoint
     */
    @Nonnull
    HostEndpoint getHostEndpoint();

    /**
     * Returns the optional port.
     *
     * @return the optional port
     */
    @Nonnull
    Optional<Integer> getPort();

    /**
     * Returns the optional URI.
     *
     * @return the optional URI
     */
    @Nonnull
    Optional<URI> getURI();

    /**
     * Returns a string representation of this service endpoint consisting of
     * the host address or address string optionally followed by a colon and the
     * the service port.
     *
     * @return a string representation of this service endpoint consisting of the
     * host address or address string optionally followed by a colon and the
     * service port
     */
    @Nonnull
    default String asHostWithOptionalPort() {
        return asHostWithOptionalPort(null);
    }

    /**
     * Returns a string representation of this service endpoint consisting of
     * the host address or address string optionally followed by a colon and the
     * service port or the specified default port.
     *
     * @param defaultPort the default port if no port is specified in the endpoint
     * @return a string representation of this service endpoint consisting of the
     * host address or address string optionally followed by a colon and the
     * service port
     */
    @Nonnull
    default String asHostWithOptionalPort(@Nullable Integer defaultPort) {
        StringBuilder buf = new StringBuilder();
        HostEndpoint hostEndpoint = getHostEndpoint();
        Optional<InetAddress> optionalHostAddress = hostEndpoint.getHostAddress();
        if (optionalHostAddress.isPresent()) {
            buf.append(optionalHostAddress.get().getHostAddress());
        } else {
            buf.append(hostEndpoint.getHostAddressString());
        }
        Optional<Integer> optionalPort = getPort();
        if (optionalPort.isPresent()) {
            buf.append(':');
            buf.append(optionalPort.get());
        } else if (defaultPort != null) {
            buf.append(':');
            buf.append(defaultPort);
        }
        return buf.toString();
    }
}
