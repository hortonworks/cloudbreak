package com.sequenceiq.environment.environment.flow.deletion.chain;

public enum FlowChainTriggers {

    ENV_DELETE_CLUSTERS_TRIGGER_EVENT("ENV_DELETE_CLUSTERS_TRIGGER_EVENT");

    private final String value;

    FlowChainTriggers(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
