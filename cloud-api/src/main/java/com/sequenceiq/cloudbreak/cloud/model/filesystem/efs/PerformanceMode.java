package com.sequenceiq.cloudbreak.cloud.model.filesystem.efs;

import org.apache.commons.lang3.StringUtils;

public enum PerformanceMode {
    // Allowed values: generalPurpose | maxIO
    GENERALPURPOSE("generalPurpose"),
    MAXIO("maxIO");

    private String value;

    PerformanceMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Create instance from input string
     *
     * @param value the input in String to be converted to the enum
     * @return PerformanceMode corresponding state to the input value
     * @throws IllegalArgumentException If the specified value does not map to one of the known values in this enum.
     */
    public static PerformanceMode fromValue(String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }

        return PerformanceMode.valueOf(value.toUpperCase());
    }
}
