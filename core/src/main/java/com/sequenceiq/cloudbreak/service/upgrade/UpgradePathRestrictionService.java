package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionEqualToLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class UpgradePathRestrictionService {

    @Inject
    private EntitlementService entitlementService;

    public boolean permitUpgrade(VersionComparisonContext currentVersion, VersionComparisonContext newVersion) {
        return !targetRuntimeIs731p0(newVersion) || shouldAllowTheUpgrade(currentVersion) || isInternalAccount();
    }

    @SuppressWarnings("magicnumber")
    private boolean shouldAllowTheUpgrade(VersionComparisonContext currentVersion) {
        return !(isCurrentVersionOlderThanLimited(currentVersion, "7.2.17", 100)
                || isCurrentVersionNewerThanLimited(currentVersion, "7.2.17", 600)
                || isCurrentVersionNewerThanLimited(currentVersion, "7.2.18", 300));
    }

    private boolean targetRuntimeIs731p0(VersionComparisonContext newVersion) {
        return isVersionEqualToLimited(newVersion.getMajorVersion(), CLOUDERA_STACK_VERSION_7_3_1)
                && newVersion.getPatchVersion().orElse(0) == 0;
    }

    private boolean isCurrentVersionNewerThanLimited(VersionComparisonContext currentVersion, String runtimeVersion, int patchVersion) {
        return currentVersion.getMajorVersion().equals(runtimeVersion)
                && currentVersion.getPatchVersion().isPresent() && currentVersion.getPatchVersion().get() >= patchVersion;
    }

    private boolean isCurrentVersionOlderThanLimited(VersionComparisonContext currentVersion, String runtimeVersion, int patchVersion) {
        return isVersionOlderThanLimited(currentVersion.getMajorVersion(), () -> runtimeVersion)
                || (currentVersion.getMajorVersion().equals(runtimeVersion)
                && currentVersion.getPatchVersion().orElse(0) <= patchVersion);
    }

    private boolean isInternalAccount() {
        return entitlementService.internalTenant(ThreadBasedUserCrnProvider.getAccountId());
    }

}
