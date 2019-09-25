package com.sequenceiq.cloudbreak.common.type;

public class HostMetadataExtendedState {

    private HostMetadataState hostMetadataState;

    private String explanation;

    public HostMetadataExtendedState(HostMetadataState hostMetadataState, String explanation) {
        this.hostMetadataState = hostMetadataState;
        this.explanation = explanation;
    }

    public HostMetadataState getHostMetadataState() {
        return hostMetadataState;
    }

    public String getExplanation() {
        return explanation;
    }
}
