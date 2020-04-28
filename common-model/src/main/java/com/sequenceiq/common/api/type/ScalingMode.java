package com.sequenceiq.common.api.type;

/**
 * Specifies whether an instance group supports scaling option.
 */
public enum ScalingMode {

    // The instance group is not scalable.
    NONE,
    // The instance group scalability is left to implementation. The behaviour can change time to time.
    UNSPECIFIED;

    public boolean isScalable() {
        return this != NONE;
    }
}
