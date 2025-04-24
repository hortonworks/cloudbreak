package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageBasicInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

@Component
public class TestUpgradeCandidateProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUpgradeCandidateProvider.class);

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    public Pair<String, String> getPatchUpgradeSourceAndCandidate(TestContext testContext) {
        return getUpgradeSourceAndCandidateByCondition(testContext, this::hasDifferentBuildNumber);
    }

    public Pair<String, String> getOsUpgradeSourceAndCandidate(TestContext testContext) {
        return getUpgradeSourceAndCandidateByCondition(testContext, this::hasSameBuildNumber);
    }

    private Pair<String, String> getUpgradeSourceAndCandidateByCondition(TestContext testContext, BiPredicate<ImageV4Response, ImageV4Response> matchCondition) {
        String runtimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion(testContext.getCloudProvider().getGovCloud());
        List<ImageV4Response> allCdhImage = getCdhImagesByRuntime(testContext, runtimeVersion, imageCatalogTestClient.getV4WithAllImages());
        List<ImageV4Response> advertisedCdhImages = getCdhImagesByRuntime(testContext, runtimeVersion, imageCatalogTestClient.getV4WithAdvertisedImages());

        for (int i = 0; i < advertisedCdhImages.size(); i++) {
            ImageV4Response targetImage = advertisedCdhImages.get(advertisedCdhImages.size() - i - 1);
            for (ImageV4Response sourceImage : allCdhImage) {
                if (matchCondition.test(sourceImage, targetImage)) {
                    Pair<String, String> pair = Pair.of(sourceImage.getUuid(), targetImage.getUuid());
                    LOGGER.info("Upgrade candidates found. Source image: {}, target image: {}", pair.getLeft(), pair.getRight());
                    return pair;
                }
            }
        }
        throw new TestFailException(String.format("There is no upgrade candidate found for runtime %s. Available images: %s", runtimeVersion, allCdhImage));
    }

    private List<ImageV4Response> getCdhImagesByRuntime(TestContext testContext, String runtimeVersion, Action<ImageCatalogTestDto, CloudbreakClient> action) {
        List<ImageV4Response> cdhImages = new ArrayList<>();
        testContext
                .given(ImageCatalogTestDto.class)
                .when(action)
                .then((context, entity, cloudbreakClient) -> {
                    List<ImageV4Response> sortedImages = getImagesByRuntimeAndCloudProvider(testContext, runtimeVersion, entity);
                    cdhImages.addAll(sortedImages);
                    return entity;
                })
                .validate();
        LOGGER.info("Found images: {}", cdhImages.stream().map(ImageBasicInfoV4Response::getUuid).toList());
        return cdhImages;
    }

    private List<ImageV4Response> getImagesByRuntimeAndCloudProvider(TestContext testContext, String currentUpgradeRuntimeVersion, ImageCatalogTestDto entity) {
        return entity.getResponse().getImages().getCdhImages().stream()
                .filter(image -> image.getVersion().equals(currentUpgradeRuntimeVersion)
                        && image.getImageSetsByProvider().containsKey(testContext.getCloudProvider().getCloudPlatform().name().toLowerCase()))
                .sorted(Comparator.comparing(ImageV4Response::getCreated))
                .toList();
    }

    private boolean hasDifferentBuildNumber(ImageV4Response current, ImageV4Response target) {
        String currentCdhBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String targetCdhBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        return !Objects.equals(currentCdhBuildNumber, targetCdhBuildNumber) && current.getCreated() < target.getCreated();
    }

    private boolean hasSameBuildNumber(ImageV4Response current, ImageV4Response target) {
        String currentCdhBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String targetCdhBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String currentCmBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        String targetCmBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        return Objects.equals(currentCdhBuildNumber, targetCdhBuildNumber) && Objects.equals(currentCmBuildNumber, targetCmBuildNumber)
                && !Objects.equals(current.getUuid(), target.getUuid()) && current.getCreated() < target.getCreated();
    }
}
