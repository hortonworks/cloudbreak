package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class HealthCheckAvailabilityChecker extends AvailabilityChecker {

    // feature supported from 2.32
    private static final Versioned CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION = () -> "2.31.0";

    public boolean isCdpFreeIpaHeathAgentAvailable(Stack stack) {
        return isAvailable(stack, CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION);
    }

}
