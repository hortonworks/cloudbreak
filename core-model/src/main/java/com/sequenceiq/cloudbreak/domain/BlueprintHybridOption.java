package com.sequenceiq.cloudbreak.domain;

public enum BlueprintHybridOption {

    /**
     * The template is not specific to any hybrid setup
     */
    NONE,

    /**
     * The template is for a datahub that connects to an on-prem datalake
     */
    BURST_TO_CLOUD,

    /**
     * The template is for a datalake that simulates an on-prem base cluster
     */
    ON_PREM_BASE;

    public static boolean isNone(BlueprintHybridOption option) {
        return option == null || option == NONE;
    }
}
