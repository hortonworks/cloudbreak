package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixService;

@Service
public class UpgradePermissionProvider {

    @Inject
    private ComponentBuildNumberComparator componentBuildNumberComparator;

    @Inject
    private UpgradeMatrixService upgradeMatrixService;

    @Inject
    private ComponentVersionComparator componentVersionComparator;

    public boolean permitCmAndStackUpgrade(Image currentImage, Image image, String versionKey, String buildNumberKey) {
        String currentVersion = getVersionFromImage(currentImage, versionKey);
        String newVersion = getVersionFromImage(image, versionKey);
        return versionsArePresent(currentVersion, newVersion)
                && currentVersion.equals(newVersion)
                        ? permitCmAndStackUpgradeByBuildNumber(currentImage, image, buildNumberKey)
                        : permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion);
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

    boolean permitCmAndStackUpgradeByComponentVersion(String currentVersion, String newVersion) {
        return permitByComponentVersion(currentVersion, newVersion) && permitByUpgradeMatrix(currentVersion, newVersion);
    }

    private boolean permitByComponentVersion(String currentVersion, String newVersion) {
        return componentVersionComparator.permitCmAndStackUpgradeByComponentVersion(currentVersion, newVersion);
    }

    private boolean permitByUpgradeMatrix(String currentVersion, String newVersion) {
        return upgradeMatrixService.permitByUpgradeMatrix(currentVersion, newVersion);
    }
}
