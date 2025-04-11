package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionEqualToLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class UpgradePathRestrictionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradePathRestrictionService.class);

    @Inject
    private EntitlementService entitlementService;

    public boolean permitUpgrade(VersionComparisonContext current, VersionComparisonContext target) {
        boolean result = permitUpgradeByVersion(current, target);
        LOGGER.debug("Upgrade from {} to {}, permitted: {}", current, target, result);
        return result;
    }

    @SuppressWarnings({ "checkstyle:CyclomaticComplexity", "checkstyle:MagicNumber" })
    private boolean permitUpgradeByVersion(VersionComparisonContext current, VersionComparisonContext target) {
        if (skipValidation(current, target)) {
            return true;
        }

        int targetPatch = target.getPatchVersion().orElse(0);
        int currentPatch = current.getPatchVersion().orElse(0);
        String currentMajor = current.getMajorVersion();

        if (targetPatch == 0 || targetPatch == 100) {
            boolean from7217 = majorVersionEquals(currentMajor, "7.2.17") && currentPatch > 100 && currentPatch < 600;
            boolean from7218 = majorVersionEquals(currentMajor, "7.2.18") && currentPatch < 300;
            return isVersionNewerOrEqualThanLimited(currentMajor, () -> "7.2.17") && (from7217 || from7218);
        }

        if (targetPatch >= 200) {
            boolean from7217OrNewer = majorVersionEquals(currentMajor, "7.2.17") && currentPatch >= 200;
            boolean from7218OrNewer = isVersionNewerOrEqualThanLimited(currentMajor, () -> "7.2.18");
            return from7217OrNewer || from7218OrNewer;
        }

        return true;
    }

    private boolean majorVersionEquals(String currentMajor, String limited) {
        return isVersionEqualToLimited(currentMajor, () -> limited);
    }

    private boolean skipValidation(VersionComparisonContext current, VersionComparisonContext target) {
        return !isVersionEqualToLimited(target.getMajorVersion(), CLOUDERA_STACK_VERSION_7_3_1) ||
                isVersionNewerOrEqualThanLimited(current.getMajorVersion(), CLOUDERA_STACK_VERSION_7_3_1) || isInternalAccount();
    }

    private boolean isInternalAccount() {
        return entitlementService.internalTenant(ThreadBasedUserCrnProvider.getAccountId());
    }

}
