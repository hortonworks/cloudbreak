package com.sequenceiq.cloudbreak.cloud.model.filesystem.efs;

import org.apache.commons.lang3.StringUtils;

public enum ThroughputMode {
    // Allowed values: bursting | provisioned
    BURSTING,
    PROVISIONED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    /**
     * Create instance from input string
     *
     * @param value the input in String to be converted to the enum
     * @return ThroughputMode corresponding state to the input value
     * @throws IllegalArgumentException If the specified value does not map to one of the known values in this enum.
     */
    public static ThroughputMode fromValue(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }

        return valueOf(value.toUpperCase());
    }
}
