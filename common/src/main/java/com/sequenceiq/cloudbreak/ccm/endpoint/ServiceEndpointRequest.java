package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Request for looking up service endpoints.
 */
public interface ServiceEndpointRequest<T extends ServiceEndpoint> {

    /**
     * The default amount of time to wait to fulfill a request, in seconds.
     */
    int DEFAULT_WAIT_DURATION_SEC = 30;

    /**
     * The default polling interval, in milliseconds.
     */
    long DEFAULT_POLLING_INTERVAL_MS = 1000L;

    /**
     * Returns a default service endpoint request for the specified target service.
     *
     * @param <T>              the type of endpoint
     * @param targetInstanceId the target instance ID
     * @param hostEndpoint     the optional host endpoint
     * @param port             the optional port
     * @param serviceFamily    the service family
     * @param directAccessRequired whether the lookup should only allow direct access to the target service
     * @return a default service endpoint request for the specified target service
     */
    @Nonnull
    static <T extends ServiceEndpoint> ServiceEndpointRequest<T> createDefaultServiceEndpointRequest(
            @Nonnull String targetInstanceId, @Nullable HostEndpoint hostEndpoint, @Nullable Integer port, @Nonnull ServiceFamily<T> serviceFamily,
            boolean directAccessRequired) {

        return new ServiceEndpointRequest<T>() {

            @Override
            public TargetInstance getTargetInstance() {
                return new TargetInstance() {
                    @Nonnull
                    @Override
                    public String getTargetInstanceId() {
                        return targetInstanceId;
                    }

                    @Nonnull
                    @Override
                    public Optional<HostEndpoint> getHostEndpoint() {
                        return Optional.ofNullable(hostEndpoint);
                    }
                };
            }

            @Override
            public ServiceFamily<T> getServiceFamily() {
                return serviceFamily;
            }

            @Override
            public Optional<Integer> getPort() {
                return Optional.ofNullable(port);
            }

            @Override
            public Optional<ZonedDateTime> getWaitUntilTime() {
                return Optional.of(ZonedDateTime.now().plusSeconds(DEFAULT_WAIT_DURATION_SEC));
            }

            @Override
            public long getPollingIntervalInMs() {
                return DEFAULT_POLLING_INTERVAL_MS;
            }

            @Override
            public boolean isDirectAccessRequired() {
                return directAccessRequired;
            }
        };
    }

    /**
     * Returns the target instance.
     *
     * @return the target instance
     */
    TargetInstance getTargetInstance();

    /**
     * Returns the service family.
     *
     * @return the service family
     */
    ServiceFamily<T> getServiceFamily();

    /**
     * Returns the optional port.
     *
     * @return the optional port
     */
    default Optional<Integer> getPort() {
        return Optional.empty();
    }

    /**
     * An optional datetime after which the lookup attempt should fail.
     *
     * @return the optional timeout
     */
    default Optional<ZonedDateTime> getWaitUntilTime() {
        return Optional.empty();
    }

    /**
     * Returns the default polling interval in milliseconds.
     *
     * @return the default polling interval in milliseconds
     */
    default long getPollingIntervalInMs() {
        return DEFAULT_POLLING_INTERVAL_MS;
    }

    /**
     * Returns whether the lookup should only allow direct access to the target service.
     *
     * @return whether the lookup should only allow direct access to the target service
     */
    default boolean isDirectAccessRequired() {
        return true;
    }
}
