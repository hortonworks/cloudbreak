package com.sequenceiq.periscope.policies.scaling.rule;

public final class RuleProperties {

    public static final String LIMIT = "limit";
    public static final String SCALING_ADJUSTMENT = "scaling-adjustment";

    private RuleProperties() {
        throw new IllegalStateException();
    }
}
