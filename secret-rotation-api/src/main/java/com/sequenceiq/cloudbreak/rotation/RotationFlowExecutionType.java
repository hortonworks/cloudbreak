package com.sequenceiq.cloudbreak.rotation;

public enum RotationFlowExecutionType {
    PREVALIDATE("Pre validate"),
    ROLLBACK("Rollback"),
    FINALIZE("Finalization"),
    ROTATE("Rotation");

    private final String displayName;

    RotationFlowExecutionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
