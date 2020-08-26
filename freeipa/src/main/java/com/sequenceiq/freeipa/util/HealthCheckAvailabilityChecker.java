package com.sequenceiq.freeipa.util;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class HealthCheckAvailabilityChecker extends AvailabilityChecker {

    // feature supported from 2.32
    private static final Versioned CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION = () -> "2.31.0";

    @Inject
    private CrnService crnService;

    @Inject
    private EntitlementService entitlementService;

    public boolean isCdpFreeIpaHeathAgentAvailable(Stack stack) {
        String accountId = crnService.getCurrentAccountId();
        return isAvailable(stack, CDP_FREEIPA_HEALTH_AGENT_AFTER_VERSION) &&
                entitlementService.freeIpaHealthCheckEnabled(INTERNAL_ACTOR_CRN, accountId);
    }

}
