package com.sequenceiq.cloudbreak.cluster.model.stopstart;

public enum HostCommissionState {

    COMMISSIONED("COMMISSIONED"),

    DECOMMISSIONED("DECOMMISSIONED"),

    UNKNOWN("UNKNOWN"),

    OFFLINING("OFFLINING"),

    OFFLINED("OFFLINED");

    private String value;

    HostCommissionState(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "HostCommissionState{" +
                "value='" + value + '\'' +
                '}';
    }
}
