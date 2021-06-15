package com.sequenceiq.cloudbreak.service.upgrade;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@Service
public class UpgradePermissionProvider {

    @Inject
    private ComponentBuildNumberComparator componentBuildNumberComparator;

    @Inject
    private UpgradeMatrixService upgradeMatrixService;

    @Inject
    private ComponentVersionComparator componentVersionComparator;

    @Inject
    private SupportedRuntimes supportedRuntimes;

    public boolean permitCmUpgrade(ImageFilterParams imageFilterParams, Image image) {
        return permitUpgrade(imageFilterParams, image, ImagePackageVersion.CM, ImagePackageVersion.CM_BUILD_NUMBER, false);
    }

    public boolean permitStackUpgrade(ImageFilterParams filterParams, Image image) {
        return isRuntimeVersionSupported(image)
                && permitUpgrade(filterParams, image, ImagePackageVersion.STACK, ImagePackageVersion.CDH_BUILD_NUMBER, checkUpgradeMatrix(filterParams));
    }

    private boolean checkUpgradeMatrix(ImageFilterParams imageFilterParams) {
        return StackType.DATALAKE.equals(imageFilterParams.getStackType());
    }

    private boolean permitUpgrade(ImageFilterParams imageFilterParams, Image image, ImagePackageVersion version, ImagePackageVersion buildNumber,
            boolean checkUpgradeMatrix) {
        String currentVersion = imageFilterParams.getCurrentImage().getPackageVersion(version);
        String newVersion = image.getPackageVersion(version);
        return versionsArePresent(currentVersion, newVersion)
                && currentVersion.equals(newVersion)
                ? permitCmAndStackUpgradeByBuildNumber(imageFilterParams.getCurrentImage(), image, buildNumber.getKey())
                : permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion, checkUpgradeMatrix);
    }

    private boolean versionsArePresent(String currentVersion, String newVersion) {
        return currentVersion != null && newVersion != null;
    }

    private boolean permitCmAndStackUpgradeByBuildNumber(Image currentImage, Image image, String buildNumberKey) {
        return componentBuildNumberComparator.compare(currentImage, image, buildNumberKey);
    }

    boolean permitCmAndStackUpgradeByComponentVersion(String currentVersion, String newVersion, boolean checkUpgradeMatrix) {
        return permitByComponentVersion(currentVersion, newVersion)
                && (!checkUpgradeMatrix || permitByUpgradeMatrix(currentVersion, newVersion));
    }

    private boolean isRuntimeVersionSupported(Image image) {
        return supportedRuntimes.isSupported(image.getPackageVersion(ImagePackageVersion.STACK));
    }

    private boolean permitByComponentVersion(String currentVersion, String newVersion) {
        return componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion);
    }

    private boolean permitByUpgradeMatrix(String currentVersion, String newVersion) {
        return upgradeMatrixService.permitByUpgradeMatrix(currentVersion, newVersion);
    }
}
