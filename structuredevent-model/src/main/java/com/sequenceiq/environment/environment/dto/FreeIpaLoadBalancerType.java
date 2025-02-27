package com.sequenceiq.environment.environment.dto;

public enum FreeIpaLoadBalancerType {
    NONE, INTERNAL_NLB;

    public static FreeIpaLoadBalancerType getDefault() {
        return INTERNAL_NLB;
    }
}
