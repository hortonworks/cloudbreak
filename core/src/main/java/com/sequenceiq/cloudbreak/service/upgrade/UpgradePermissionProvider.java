package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.service.upgrade.image.ClusterUpgradeImageFilter.CM_PACKAGE_KEY;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@Service
public class UpgradePermissionProvider {

    protected static final String STACK_PACKAGE_KEY = "stack";

    protected static final String CDH_BUILD_NUMBER_KEY = "cdh-build-number";

    protected static final String CM_BUILD_NUMBER_KEY = "cm-build-number";

    @Inject
    private ComponentBuildNumberComparator componentBuildNumberComparator;

    @Inject
    private UpgradeMatrixService upgradeMatrixService;

    @Inject
    private ComponentVersionComparator componentVersionComparator;

    public boolean permitCmUpgrade(ImageFilterParams imageFilterParams, Image image) {
        return permitUpgrade(imageFilterParams, image, CM_PACKAGE_KEY, CM_BUILD_NUMBER_KEY, false);
    }

    public boolean permitStackUpgrade(ImageFilterParams imageFilterParams, Image image) {
        return permitUpgrade(imageFilterParams, image, STACK_PACKAGE_KEY, CDH_BUILD_NUMBER_KEY, imageFilterParams.isCheckUpgradeMatrix());
    }

    private boolean permitUpgrade(ImageFilterParams imageFilterParams, Image image, String versionKey, String buildNumberKey, boolean checkUpgradeMatrix) {
        String currentVersion = getVersionFromImage(imageFilterParams.getCurrentImage(), versionKey);
        String newVersion = getVersionFromImage(image, versionKey);
        return versionsArePresent(currentVersion, newVersion)
                && currentVersion.equals(newVersion)
                        ? permitCmAndStackUpgradeByBuildNumber(imageFilterParams.getCurrentImage(), image, buildNumberKey)
                        : permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion, checkUpgradeMatrix);
    }

    private String getVersionFromImage(Image image, String key) {
        return Optional.ofNullable(image.getPackageVersions())
                .map(map -> map.get(key))
                .orElse(null);
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

    private boolean permitByComponentVersion(String currentVersion, String newVersion) {
        return componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion);
    }

    private boolean permitByUpgradeMatrix(String currentVersion, String newVersion) {
        return upgradeMatrixService.permitByUpgradeMatrix(currentVersion, newVersion);
    }
}
