package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;

@Component
public class ComponentVersionComparator {

    public boolean permitCmAndStackUpgradeByComponentVersion(VersionComparisonContext currentVersion, VersionComparisonContext candidateVersion) {
        return candidateVersionNewer(currentVersion, candidateVersion)
                || candidatePatchVersionIsNewer(currentVersion, candidateVersion)
                || buildNumberIsNewerOrEqual(currentVersion, candidateVersion);
    }

    private boolean candidateVersionNewer(VersionComparisonContext currentVersion, VersionComparisonContext candidateVersion) {
        return CMRepositoryVersionUtil.isVersionOlderThanLimited(currentVersion.getMajorVersion(), candidateVersion::getMajorVersion);
    }

    private boolean candidatePatchVersionIsNewer(VersionComparisonContext currentVersion, VersionComparisonContext candidateVersion) {
        return majorVersionsAreEquals(currentVersion, candidateVersion) && candidatePatchVersionIsNewerIfPresent(currentVersion, candidateVersion);
    }

    private boolean candidatePatchVersionIsNewerIfPresent(VersionComparisonContext currentVersion, VersionComparisonContext candidateVersion) {
        return candidateVersion.getPatchVersion()
                .map(candidatePatch -> currentVersion.getPatchVersion().map(currentPatch -> candidatePatch > currentPatch).orElse(true))
                .orElse(false);
    }

    private boolean patchVersionAreEquals(VersionComparisonContext currentVersion, VersionComparisonContext candidateVersion) {
        Optional<Integer> currentPatchVersion = currentVersion.getPatchVersion();
        Optional<Integer> candidatePatchVersion = candidateVersion.getPatchVersion();
        return currentPatchVersion.equals(candidatePatchVersion);
    }

    private boolean majorVersionsAreEquals(VersionComparisonContext currentVersion, VersionComparisonContext candidateVersion) {
        return CMRepositoryVersionUtil.isVersionEqualToLimited(currentVersion::getMajorVersion, candidateVersion::getMajorVersion);
    }

    private boolean buildNumberIsNewerOrEqual(VersionComparisonContext currentVersion, VersionComparisonContext candidateVersion) {
        return majorVersionsAreEquals(currentVersion, candidateVersion)
                && patchVersionAreEquals(currentVersion, candidateVersion)
                && Objects.nonNull(candidateVersion.getBuildNumber())
                && Objects.nonNull(currentVersion.getBuildNumber())
                && candidateVersion.getBuildNumber() >= currentVersion.getBuildNumber();
    }

}
