package com.sequenceiq.cloudbreak.ccm.endpoint;

import java.util.Optional;

import jakarta.annotation.Nonnull;

/**
 * Represents a target instance where a service is running.
 */
public interface TargetInstance {

    /**
     * Returns the unique identifier for the target instance.
     *
     * @return the unique identifier for the target instance
     */
    @Nonnull
    String getTargetInstanceId();

    /**
     * The optional host endpoint for the target.
     *
     * @return the optional host endpoint for the target
     */
    @Nonnull
    default Optional<HostEndpoint> getHostEndpoint() {
        return Optional.empty();
    }
}
