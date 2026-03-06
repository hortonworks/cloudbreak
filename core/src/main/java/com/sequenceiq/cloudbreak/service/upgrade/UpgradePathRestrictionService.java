package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.getAccountId;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class UpgradePathRestrictionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradePathRestrictionService.class);

    private final UpgradeVersionPatternMatcher patternMatcher = new UpgradeVersionPatternMatcher();

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private List<BlockedUpgradePath> blockedUpgradePaths;

    /**
     * Returns {@code true} if the upgrade from {@code current} to {@code target} is permitted.
     *
     * <p>The default is to permit. An upgrade is blocked only when at least one rule in
     * {@code upgrade-path-restrictions.json} matches both the source and target version
     * and is not overridden by an active entitlement or internal-account flag.
     */
    public boolean permitUpgrade(VersionComparisonContext current, VersionComparisonContext target) {
        boolean blocked = false;
        for (BlockedUpgradePath rule : blockedUpgradePaths) {
            if (patternMatcher.matches(rule.from(), current) && patternMatcher.matches(rule.to(), target) && !isOverridden(rule)) {
                LOGGER.debug("Upgrade from {} to {} blocked by rule [from={}, to={}]", current, target, rule.from(), rule.to());
                blocked = true;
            }
        }
        LOGGER.debug("Upgrade from {} to {}, permitted: {}", current, target, !blocked);
        return !blocked;
    }

    private boolean isOverridden(BlockedUpgradePath rule) {
        return isEntitlementOverrideActive(rule) || isInternalAccountOverrideActive(rule);
    }

    private boolean isEntitlementOverrideActive(BlockedUpgradePath rule) {
        if (rule.entitlementOverride() == null) {
            return false;
        }
        if ("mitigateReleaseFailure7218P1100".equals(rule.entitlementOverride())) {
            return entitlementService.isMitigateReleaseFailure7218P1100Enabled(getAccountId());
        }
        return false;
    }

    private boolean isInternalAccountOverrideActive(BlockedUpgradePath rule) {
        return rule.internalAccountOverride() && entitlementService.internalTenant(getAccountId());
    }
}
