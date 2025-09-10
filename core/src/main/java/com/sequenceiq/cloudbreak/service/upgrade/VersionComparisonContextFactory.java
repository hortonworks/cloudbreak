package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.RELEASE_VERSION_TAG;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.util.CdhPatchVersionProvider;

@Component
public class VersionComparisonContextFactory {

    private static final int MAJOR_RELEASE_VERSION_LENGTH = 3;

    private static final int PATCH_VERSION_POSITION = 3;

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

    public VersionComparisonContext buildForStackBasedOnReleaseVersion(Image image) {
        String releaseVersion = image.getTags().get(RELEASE_VERSION_TAG.getKey());
        if (StringUtils.isNotEmpty(releaseVersion)) {
            String[] releaseVersionParts = releaseVersion.split("\\.");
            if (releaseVersionParts.length == MAJOR_RELEASE_VERSION_LENGTH || releaseVersionParts.length == MAJOR_RELEASE_VERSION_LENGTH + 1) {
                return new VersionComparisonContext.Builder()
                        .withMajorVersion(String.join(".", releaseVersionParts[0], releaseVersionParts[1], releaseVersionParts[2]))
                        .withPatchVersion(releaseVersionParts.length > MAJOR_RELEASE_VERSION_LENGTH ?
                                Integer.parseInt(releaseVersionParts[PATCH_VERSION_POSITION]) : null)
                        .withBuildNumber(Integer.parseInt(image.getPackageVersion(CDH_BUILD_NUMBER)))
                        .build();
            }
        }
        return buildForStack(image);
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
