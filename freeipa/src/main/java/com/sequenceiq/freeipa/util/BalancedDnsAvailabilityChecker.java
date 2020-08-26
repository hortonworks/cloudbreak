package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class BalancedDnsAvailabilityChecker extends AvailabilityChecker {

    // feature supported from 2.20
    private static final Versioned BALANCED_DNS_NAME_AFTER_VERSION = () -> "2.19.0";

    public boolean isBalancedDnsAvailable(Stack stack) {
        return isAvailable(stack, BALANCED_DNS_NAME_AFTER_VERSION);
    }

}
