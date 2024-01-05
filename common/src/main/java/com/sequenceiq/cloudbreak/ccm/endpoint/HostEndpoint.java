package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a host endpoint.
 */
public class HostEndpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The host address string.
     */
    private final String hostAddressString;

    /**
     * The optional resolved host address.
     */
    private final InetAddress hostAddress;

    /**
     * Creates a host endpoint with the specified parameters.
     *
     * @param hostAddressString the host address string
     */
    public HostEndpoint(@Nonnull String hostAddressString) {
        this(hostAddressString, null);
    }

    /**
     * Creates a host endpoint with the specified parameters.
     *
     * @param hostAddressString the host address string
     * @param hostAddress       the optional resolved host address
     */
    @JsonCreator
    public HostEndpoint(
            @Nonnull @JsonProperty("hostAddressString") String hostAddressString,
            @Nullable @JsonProperty("hostAddress") InetAddress hostAddress) {
        this.hostAddressString =
                Objects.requireNonNull(hostAddressString, "hostAddressString is null");
        this.hostAddress = hostAddress;
    }

    /**
     * Returns the host address string.
     *
     * @return the host address string
     */
    @Nonnull
    public String getHostAddressString() {
        return hostAddressString;
    }

    /**
     * Returns the optional resolved host address.
     *
     * @return the optional resolved host address
     */
    @Nonnull
    public Optional<InetAddress> getHostAddress() {
        return Optional.ofNullable(hostAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HostEndpoint that = (HostEndpoint) o;

        if (!hostAddressString.equals(that.hostAddressString)) {
            return false;
        }
        return Objects.equals(hostAddress, that.hostAddress);
    }

    @Override
    public int hashCode() {
        int result = hostAddressString.hashCode();
        result = 31 * result + (hostAddress == null ? 0 : hostAddress.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "HostEndpoint{"
                + "hostAddressString='" + hostAddressString + '\''
                + (hostAddress == null ? "" : ", hostAddress=" + hostAddress)
                + '}';
    }
}
