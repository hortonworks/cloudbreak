package com.sequenceiq.common.model;

public enum AwsDiskType {
    Standard("standard", "Magnetic"),
    Ephemeral("ephemeral", "Ephemeral"),
    Gp2("gp2", "General Purpose (SSD)"),
    St1("st1", "Throughput Optimized HDD");

    private final String value;

    private final String displayName;

    AwsDiskType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String value() {
        return value;
    }

    public String displayName() {
        return displayName;
    }
}
