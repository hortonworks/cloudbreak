package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageBasicInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
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
        String runtimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion(testContext.getCloudProvider().getGovCloud());
        return getUpgradeSourceAndCandidateByCondition(testContext, this::hasDifferentBuildNumber, runtimeVersion, Architecture.X86_64, false);
    }

    public Pair<String, String> getPatchUpgradeSourceAndCandidate(TestContext testContext, String runtimeVersion, Architecture architecture) {
        return getUpgradeSourceAndCandidateByCondition(testContext, this::hasDifferentBuildNumber, runtimeVersion, architecture, false);
    }

    public Pair<String, String> getOsUpgradeSourceAndCandidate(TestContext testContext) {
        String runtimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion(testContext.getCloudProvider().getGovCloud());
        return getUpgradeSourceAndCandidateByCondition(testContext, this::hasSameBuildNumber, runtimeVersion, Architecture.X86_64, false);
    }

    public Pair<String, String> getOsUpgradeSourceAndCandidate(TestContext testContext, String runtimeVersion, Architecture architecture) {
        return getUpgradeSourceAndCandidateByCondition(testContext, this::hasSameBuildNumber, runtimeVersion, architecture, false);
    }

    public Pair<String, String> getDistroUpgradeSourceAndCandidate(TestContext testContext, String runtimeVersion, Architecture architecture, OsType sourceOs,
            OsType targetOs, boolean defaultOnly) {
        return getUpgradeSourceAndCandidateByCondition(testContext, (current, target) ->
                sourceOs.matches(current.getOs(), current.getOsType())
                        && targetOs.matches(target.getOs(), target.getOsType())
                        && hasSameBuildNumber(current, target), runtimeVersion, architecture, defaultOnly);
    }

    private Pair<String, String> getUpgradeSourceAndCandidateByCondition(TestContext testContext, BiPredicate<ImageV4Response, ImageV4Response> matchCondition,
            String runtimeVersion, Architecture architecture, boolean defaultOnly) {
        List<ImageV4Response> allCdhImage = getCdhImagesByRuntime(testContext, runtimeVersion, imageCatalogTestClient.getV4WithAllImages())
                .stream()
                .filter(hasArchitecture(architecture))
                .sorted(Comparator.comparing(ImageV4Response::getCreated).reversed())
                .toList();
        List<ImageV4Response> advertisedCdhImages = getDefaultCdhImagesByRuntime(testContext, runtimeVersion, imageCatalogTestClient
                .getImagesByNameV4(testContext.getCloudPlatform(), defaultOnly))
                .stream()
                .filter(hasArchitecture(architecture))
                .sorted(Comparator.comparing(ImageV4Response::getCreated).reversed())
                .toList();

        for (int i = 0; i < advertisedCdhImages.size(); i++) {
            ImageV4Response targetImage = advertisedCdhImages.get(i);
            for (int j = 0; j < allCdhImage.size(); j++) {
                ImageV4Response sourceImage = allCdhImage.get(j);
                if (matchCondition.test(sourceImage, targetImage)) {
                    Pair<String, String> pair = Pair.of(sourceImage.getUuid(), targetImage.getUuid());
                    LOGGER.info("Upgrade candidates found. Source image: {}, target image: {}", pair.getLeft(), pair.getRight());
                    return pair;
                }
            }
        }
        throw new TestFailException(String.format("There is no upgrade candidate found for runtime %s. Available images: %s", runtimeVersion, allCdhImage));
    }

    private static Predicate<ImageV4Response> hasArchitecture(Architecture architecture) {
        return image -> architecture.equals(Architecture.fromStringWithFallback(image.getArchitecture()));
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
        LOGGER.info("Found images for runtime {}: {}", runtimeVersion, cdhImages.stream().map(ImageBasicInfoV4Response::getUuid).toList());
        return cdhImages;
    }

    private List<ImageV4Response> getDefaultCdhImagesByRuntime(TestContext testContext, String runtimeVersion, Action<ImageCatalogTestDto,
            CloudbreakClient> action) {
        List<ImageV4Response> cdhImages = new ArrayList<>();
        testContext
                .given(ImageCatalogTestDto.class)
                .when(action)
                .then((context, entity, cloudbreakClient) -> {
                    List<ImageV4Response> sortedImages = getDefaultImagesByRuntimeAndCloudProvider(testContext, runtimeVersion, entity);
                    cdhImages.addAll(sortedImages);
                    return entity;
                })
                .validate();
        LOGGER.info("Found images for runtime {}: {}", runtimeVersion, cdhImages.stream().map(ImageBasicInfoV4Response::getUuid).toList());
        return cdhImages;
    }

    private List<ImageV4Response> getImagesByRuntimeAndCloudProvider(TestContext testContext, String currentUpgradeRuntimeVersion, ImageCatalogTestDto entity) {
        ImagesV4Response images = entity.getResponse().getImages();
        return filterByCloudProviderAndRuntime(testContext, currentUpgradeRuntimeVersion, images);
    }

    private List<ImageV4Response> getDefaultImagesByRuntimeAndCloudProvider(TestContext testContext, String currentUpgradeRuntimeVersion,
            ImageCatalogTestDto entity) {
        ImagesV4Response images = entity.getResponseByProvider();
        return filterByCloudProviderAndRuntime(testContext, currentUpgradeRuntimeVersion, images);
    }

    private List<ImageV4Response> filterByCloudProviderAndRuntime(TestContext testContext, String currentUpgradeRuntimeVersion, ImagesV4Response images) {
        String provider = testContext.getCloudProvider().getCloudPlatform().name().toLowerCase();
        String region = testContext.getCloudProvider().region();
        return images.getCdhImages().stream()
                .filter(image -> image.getVersion().equals(currentUpgradeRuntimeVersion))
                .filter(image -> image.getImageSetsByProvider().containsKey(provider))
                .filter(image -> image.getImageSetsByProvider().get(provider).containsKey(region) || !CloudPlatform.azureOrAws(provider))
                .sorted(Comparator.comparing(ImageV4Response::getCreated))
                .toList();
    }

    private boolean hasDifferentBuildNumber(ImageV4Response current, ImageV4Response target) {
        String currentCdhBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String targetCdhBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String currentCmVersion = current.getPackageVersions().get(ImagePackageVersion.CM.getKey());
        String targetCmVersion = target.getPackageVersions().get(ImagePackageVersion.CM.getKey());
        return new VersionComparator().compare(() -> targetCmVersion, () -> currentCmVersion) > 0 &&
                !Objects.equals(currentCdhBuildNumber, targetCdhBuildNumber) && current.getCreated() < target.getCreated();
    }

    private boolean hasSameBuildNumber(ImageV4Response current, ImageV4Response target) {
        String currentCdhBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String targetCdhBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String currentCmBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        String targetCmBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        return Objects.equals(currentCdhBuildNumber, targetCdhBuildNumber) && Objects.equals(currentCmBuildNumber, targetCmBuildNumber)
                && !Objects.equals(current.getUuid(), target.getUuid())
                && (!Objects.equals(current.getOsType(), target.getOsType()) || current.getCreated() < target.getCreated());
    }
}
