package com.sequenceiq.periscope.service;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class EntitlementValidationService {

    private static final String ALLOWED = "ALLOWED_PLATFORM";

    private static final Map<String, String> PLATFORM_ENTITLEMENTS = Map.of(
            "AWS", "CDP_AWS_AUTOSCALING",
            "AZURE", "CDP_AZURE_AUTOSCALING",
            "YARN", ALLOWED);

    @Value("${periscope.entitlementCheckEnabled:true}")
    private Boolean entitlementCheckEnabled;

    @Inject
    private EntitlementService entitlementService;

    @Cacheable(cacheNames = "accountEntitlementCache", key = "{#accountId,#cloudPlatform}")
    public boolean autoscalingEntitlementEnabled(String userCrn, String accountId, String cloudPlatform) {
        boolean entitled;
        Optional<String> platformEntitlement = Optional.ofNullable(PLATFORM_ENTITLEMENTS.get(cloudPlatform));
        if (!entitlementCheckEnabled || platformEntitlement.map(entitlement -> ALLOWED.equals(entitlement)).orElse(false)) {
            entitled = true;
        } else if (platformEntitlement.isEmpty()) {
            entitled = false;
        } else {
            entitled = entitlementService.getEntitlements(userCrn, accountId)
                    .stream().anyMatch(e -> e.equalsIgnoreCase(platformEntitlement.get()));
        }
        return entitled;
    }
}
