package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
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

    @Inject
    private BaseImagePackageVersionsEnricher baseImagePackageVersionsEnricher;

    public void validateRequestedComponentVersions(ClusterUpgradeProperties properties) {
        validateRequestedComponentVersions(properties.currentImage().packageVersions(), properties.currentImage().products(),
                properties.targetImage().packageVersions(), properties.targetImage().getAllProducts());
    }

    public void validateRequestedComponentVersions(Map<String, String> currentPackages,
            Set<ClouderaManagerProduct> currentProducts, UpgradeComponents components) {
        Map<String, String> targetPackages = baseImagePackageVersionsEnricher.getPackageVersionsFromComponents(
                components.getNormalizedRepository(), new HashSet<>(components.products()));
        validateRequestedComponentVersions(currentPackages, currentProducts, targetPackages, components.products());
    }

    private void validateRequestedComponentVersions(Map<String, String> currentPackages, Collection<ClouderaManagerProduct> currentProducts,
            Map<String, String> targetPackages, Collection<ClouderaManagerProduct> targetProducts) {
        validateComponentUpgrade("Cloudera Manager", versionComparisonContextFactory.buildForCm(currentPackages),
                versionComparisonContextFactory.buildForCm(targetPackages));
        validateComponentUpgrade("CDH", versionComparisonContextFactory.buildForStack(currentPackages, getCdhParcels(currentProducts)),
                versionComparisonContextFactory.buildForStack(targetPackages, getCdhParcels(targetProducts)));
    }

    private Map<String, String> getCdhParcels(Collection<ClouderaManagerProduct> products) {
        return Optional.ofNullable(products).orElse(Set.of()).stream()
                .filter(product -> CDH.name().equals(product.getName()))
                .collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
    }

    private void validateComponentUpgrade(String component, VersionComparisonContext current, VersionComparisonContext target) {
        if (!permitByComponentVersion(current, target)) {
            throw new BadRequestException(String.format("Cannot upgrade %s from %s to %s.", component,
                    describeVersion(current), describeVersion(target)));
        }
    }

    private String describeVersion(VersionComparisonContext version) {
        return String.format("%s%s (build: %s)", version.getMajorVersion(), version.getPatchVersion().map(patch -> "." + patch).orElse(""),
                Objects.toString(version.getBuildNumber(), "unspecified"));
    }

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
