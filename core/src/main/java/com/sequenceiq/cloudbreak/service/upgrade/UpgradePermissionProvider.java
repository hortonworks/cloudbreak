package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;

import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@Service
public class UpgradePermissionProvider {

    @Inject
    private UpgradeMatrixService upgradeMatrixService;

    @Inject
    private ComponentVersionComparator componentVersionComparator;

    @Inject
    private SupportedRuntimes supportedRuntimes;

    @Inject
    private VersionComparisonContextFactory versionComparisonContextFactory;

    @Inject
    private UpgradePathRestrictionService upgradePathRestrictionService;

    public boolean permitCmUpgrade(ImageFilterParams imageFilterParams, Image candiateImage) {
        VersionComparisonContext currentImageVersions = versionComparisonContextFactory.buildForCm(imageFilterParams.getCurrentImage().getPackageVersions());
        VersionComparisonContext candidateImageVersions = versionComparisonContextFactory.buildForCm(candiateImage.getPackageVersions());
        return permitByComponentVersion(currentImageVersions, candidateImageVersions, false);
    }

    public boolean permitStackUpgrade(ImageFilterParams filterParams, Image candidateImage) {
        VersionComparisonContext currentImageVersions = versionComparisonContextFactory.buildForStack(filterParams.getCurrentImage().getPackageVersions(),
                filterParams.getStackRelatedParcels());
        VersionComparisonContext candidateImageVersions = versionComparisonContextFactory.buildForStack(candidateImage);
        VersionComparisonContext candidateImageVersionsBasedOnReleaseVersion = versionComparisonContextFactory.buildForStackBasedOnReleaseVersion(
                candidateImage);
        return isRuntimeVersionSupported(candidateImage)
                && permitByComponentVersion(currentImageVersions, candidateImageVersions,
                checkUpgradeMatrix(currentImageVersions, candidateImageVersions, filterParams))
                && permitByUpgradePatchRestriction(currentImageVersions, candidateImageVersionsBasedOnReleaseVersion);
    }

    private boolean checkUpgradeMatrix(VersionComparisonContext currentImageVersionContext, VersionComparisonContext candidateImageVersionContext,
            ImageFilterParams imageFilterParams) {
        return !Objects.equals(currentImageVersionContext.getMajorVersion(), candidateImageVersionContext.getMajorVersion())
                && StackType.DATALAKE.equals(imageFilterParams.getStackType());
    }

    boolean permitByComponentVersion(VersionComparisonContext currentVersion, VersionComparisonContext newVersion, boolean checkUpgradeMatrix) {
        return permitByComponentVersion(currentVersion, newVersion)
                && (!checkUpgradeMatrix || permitByUpgradeMatrix(currentVersion, newVersion));
    }

    private boolean isRuntimeVersionSupported(Image image) {
        return supportedRuntimes.isSupported(image.getPackageVersion(STACK));
    }

    private boolean permitByComponentVersion(VersionComparisonContext currentVersion, VersionComparisonContext newVersion) {
        return componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion);
    }

    private boolean permitByUpgradePatchRestriction(VersionComparisonContext currentVersion, VersionComparisonContext newVersion) {
        return upgradePathRestrictionService.permitUpgrade(currentVersion, newVersion);
    }

    private boolean permitByUpgradeMatrix(VersionComparisonContext currentVersion, VersionComparisonContext newVersion) {
        return upgradeMatrixService.permitByUpgradeMatrix(currentVersion.getMajorVersion(), newVersion.getMajorVersion());
    }
}
