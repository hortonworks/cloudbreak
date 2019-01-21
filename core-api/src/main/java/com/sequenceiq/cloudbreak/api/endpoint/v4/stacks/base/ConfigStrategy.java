package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

public enum ConfigStrategy {
    NEVER_APPLY, ONLY_STACK_DEFAULTS_APPLY, ALWAYS_APPLY, ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES
}
