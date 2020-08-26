package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class ClusterProxyServiceAvailabilityChecker extends AvailabilityChecker {

    // feature supported from 2.21
    private static final Versioned DNS_BASED_SERVICE_NAME_AFTER_VERSION = () -> "2.20.0";

    public boolean isDnsBasedServiceNameAvailable(Stack stack) {
        return isAvailable(stack, DNS_BASED_SERVICE_NAME_AFTER_VERSION);
    }

}
