package com.sequenceiq.common.api.type;

public enum AdjustmentType {

    EXACT("Exact"), PERCENTAGE("Percentage"), BEST_EFFORT("Best Effort");

    private final String name;

    AdjustmentType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
