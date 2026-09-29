package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.common.model.OsType;

public final class ClusterUpgradePropertiesTestUtils {

    private ClusterUpgradePropertiesTestUtils() {
    }

    public static ClusterUpgradeProperties withRuntimeVersion(String runtimeVersion) {
        return withRuntimeVersionAndFlags(runtimeVersion, false, true, false);
    }

    public static ClusterUpgradeProperties withFlags(boolean lockComponents, boolean rollingUpgradeEnabled, boolean replaceVms) {
        return withRuntimeVersionAndFlags("7.2.18", lockComponents, rollingUpgradeEnabled, replaceVms);
    }

    public static ClusterUpgradeProperties withRuntimeVersionAndFlags(String runtimeVersion, boolean lockComponents, boolean rollingUpgradeEnabled,
            boolean replaceVms) {
        return withCurrentAndTargetRuntime("7.2.17", runtimeVersion, lockComponents, rollingUpgradeEnabled, replaceVms);
    }

    public static ClusterUpgradeProperties withTargetRuntimeOnly(String targetRuntimeVersion) {
        return withCurrentAndTargetRuntime(null, targetRuntimeVersion, false, true, false);
    }

    public static ClusterUpgradeProperties withTargetRuntimeAndCurrentPackages(String targetRuntimeVersion, Map<String, String> currentPackageVersions) {
        ClusterUpgradeProperties base = withTargetRuntimeOnly(targetRuntimeVersion);
        ClusterUpgradeProperties.CurrentImageUpgradeContext currentImage = new ClusterUpgradeProperties.CurrentImageUpgradeContext(
                "currentImageId",
                "currentImageCatalogName",
                "imageCatalogUrl",
                null,
                new HashMap<>(currentPackageVersions),
                new HashMap<>(),
                OsType.RHEL8,
                "redhat8",
                "x86_64",
                "2024-01-01",
                1L,
                "currentImageName");
        return new ClusterUpgradeProperties(base.getOptions(), currentImage, base.getTargetImage());
    }

    public static ClusterUpgradeProperties withTargetParcelAndCurrentPackages(String targetRuntimeVersion, String cdhParcelVersion,
            Map<String, String> currentPackageVersions) {
        ClusterUpgradeProperties base = withTargetRuntimeAndCurrentPackages(targetRuntimeVersion, currentPackageVersions);
        ClusterUpgradeProperties.TargetImageUpgradeContext target = base.getTargetImage();
        ClouderaManagerProduct cdhParcel = new ClouderaManagerProduct().withName("CDH").withVersion(cdhParcelVersion);
        ClusterUpgradeProperties.TargetImageUpgradeContext targetWithParcel = new ClusterUpgradeProperties.TargetImageUpgradeContext(
                target.imageId(), target.catalogName(), target.catalogUrl(), target.runtimeVersion(), target.imageVersion(),
                target.cdhBuildNumber(), target.packageVersions(), target.tags(), target.osType(), target.os(),
                target.architecture(), target.date(), target.created(), target.imageName(), target.stackDetails(),
                target.repo(), target.preWarmParcelEntries(), target.preWarmCsd(), cdhParcel, target.preWarmParcels(),
                target.clouderaManagerRepo());
        return new ClusterUpgradeProperties(base.getOptions(), base.getCurrentImage(), targetWithParcel);
    }

    public static ClusterUpgradeProperties withCurrentAndTargetRuntime(String currentRuntimeVersion, String targetRuntimeVersion,
            boolean lockComponents, boolean rollingUpgradeEnabled, boolean replaceVms) {
        return withCurrentAndTargetRuntime(currentRuntimeVersion, targetRuntimeVersion, "currentImageCatalogName", lockComponents,
                rollingUpgradeEnabled, replaceVms);
    }

    public static ClusterUpgradeProperties withCurrentAndTargetRuntime(String currentRuntimeVersion, String targetRuntimeVersion,
            String currentImageCatalogName, boolean lockComponents, boolean rollingUpgradeEnabled, boolean replaceVms) {
        ClusterUpgradeProperties.UpgradeRequestOptions options =
                new ClusterUpgradeProperties.UpgradeRequestOptions(replaceVms, lockComponents, rollingUpgradeEnabled);
        ClusterUpgradeProperties.CurrentImageUpgradeContext currentImage = new ClusterUpgradeProperties.CurrentImageUpgradeContext(
                "currentImageId",
                currentImageCatalogName,
                "imageCatalogUrl",
                currentRuntimeVersion,
                new HashMap<>(),
                new HashMap<>(),
                OsType.RHEL8,
                "redhat8",
                "x86_64",
                "2024-01-01",
                1L,
                "currentImageName");
        ClusterUpgradeProperties.TargetImageUpgradeContext targetImage = new ClusterUpgradeProperties.TargetImageUpgradeContext(
                "targetImageId",
                "imageCatalogName",
                "imageCatalogUrl",
                targetRuntimeVersion,
                targetRuntimeVersion,
                null,
                new HashMap<>(),
                new HashMap<>(),
                OsType.RHEL8,
                "redhat8",
                "x86_64",
                "2024-01-01",
                1L,
                "targetImageName",
                null,
                new HashMap<>(),
                null,
                null,
                null,
                null,
                null);
        return new ClusterUpgradeProperties(options, currentImage, targetImage);
    }
}
