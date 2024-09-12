package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.util.CdhPatchVersionProvider;

@Component
public class VersionComparisonContextFactory {

    @Inject
    private CdhPatchVersionProvider cdhPatchVersionProvider;

    public VersionComparisonContext buildForCm(Map<String, String> packageVersions) {
        String currentCmVersion = packageVersions.get(CM.getKey());
        return new VersionComparisonContext.Builder()
                .withMajorVersion(currentCmVersion)
                .withBuildNumber(Integer.parseInt(packageVersions.get(CM_BUILD_NUMBER.getKey())))
                .build();
    }

    public VersionComparisonContext buildForStack(Image image) {
        return new VersionComparisonContext.Builder()
                .withMajorVersion(image.getVersion())
                .withPatchVersion(cdhPatchVersionProvider.getPatchFromVersionString(
                        image.getStackDetails().getRepo().getStack().get(StackRepoDetails.REPOSITORY_VERSION)).orElse(null))
                .withBuildNumber(Integer.parseInt(image.getPackageVersion(CDH_BUILD_NUMBER)))
                .build();
    }

    public VersionComparisonContext buildForStack(Map<String, String> packageVersions, Map<String, String> stackRelatedParcels) {
        return new VersionComparisonContext.Builder()
                .withMajorVersion(packageVersions.get(STACK.getKey()))
                .withPatchVersion(getPatchVersion(stackRelatedParcels))
                .withBuildNumber(Integer.parseInt(packageVersions.get(CDH_BUILD_NUMBER.getKey())))
                .build();
    }

    private Integer getPatchVersion(Map<String, String> stackRelatedParcels) {
        Optional<String> detailedStackVersion = Optional.ofNullable(stackRelatedParcels.get(CDH.name()));
        return detailedStackVersion
                .flatMap(cdhVersion -> cdhPatchVersionProvider.getPatchFromVersionString(cdhVersion))
                .orElse(null);
    }
}
