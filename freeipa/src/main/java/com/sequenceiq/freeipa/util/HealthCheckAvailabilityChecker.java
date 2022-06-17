package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class HealthCheckAvailabilityChecker extends AvailabilityChecker {

    static final String HEALTH_CHECK_PACKAGE_NAME = "freeipa-health-agent";

    // only check if package is available, its version does not matter
    private static final Versioned HEALTH_CHECK_PACKAGE_MIN_VERSION = () -> "0.0.0";

    // feature supported from 2.32
    private static final Versioned CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION = () -> "2.31.0";

    public boolean isCdpFreeIpaHeathAgentAvailable(Stack stack) {
        return isAvailable(stack, CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION)
                && isPackageAvailable(stack, HEALTH_CHECK_PACKAGE_NAME, HEALTH_CHECK_PACKAGE_MIN_VERSION);
    }

}
