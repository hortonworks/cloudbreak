package com.sequenceiq.cloudbreak.core.flow2.chain;

public class FlowChainTriggers {
    public static final String FULL_PROVISION_TRIGGER_EVENT = "FULL_PROVISION_TRIGGER_EVENT";
    public static final String CLUSTER_RESET_CHAIN_TRIGGER_EVENT = "CLUSTER_RESET_CHAIN_TRIGGER_EVENT";
    public static final String FULL_UPSCALE_TRIGGER_EVENT = "FULL_UPSCALE_TRIGGER_EVENT";
    public static final String FULL_DOWNSCALE_TRIGGER_EVENT = "FULL_DOWNSCALE_TRIGGER_EVENT";
    public static final String FULL_START_TRIGGER_EVENT = "FULL_START_TRIGGER_EVENT";
    public static final String FULL_STOP_TRIGGER_EVENT = "FULL_STOP_TRIGGER_EVENT";
    public static final String FULL_SYNC_TRIGGER_EVENT = "FULL_SYNC_TRIGGER_EVENT";

    public static final String TEST_CHAIN_OF_CHAIN_TRIGGER_EVENT = "TEST_CHAIN_OF_CHAIN_TRIGGER_EVENT";
    public static final String TEST_CHAIN_OF_CHAIN_OF_CHAIN_TRIGGER_EVENT = "TEST_CHAIN_OF_CHAIN_OF_CHAIN_TRIGGER_EVENT";

    private FlowChainTriggers() {
    }
}
