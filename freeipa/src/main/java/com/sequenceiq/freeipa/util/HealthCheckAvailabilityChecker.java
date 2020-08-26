package com.sequenceiq.freeipa.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class HealthCheckAvailabilityChecker {

    // feature supported from 2.29
    private static final Versioned CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION = () -> "2.28.0";

    public boolean isCdpFreeIpaHeathAgentAvailable(Stack stack) {
        if (StringUtils.isNotBlank(stack.getAppVersion())) {
            Versioned currentVersion = () -> stack.getAppVersion();
            return new VersionComparator().compare(currentVersion, CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION) > 0;
        } else {
            return false;
        }
    }

}
