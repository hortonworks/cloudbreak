package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageBasicInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
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

        return findUpgradePair(allCdhImage, advertisedCdhImages, matchCondition, runtimeVersion);
    }

    Pair<String, String> findUpgradePair(List<ImageV4Response> sourceImages, List<ImageV4Response> targetImages,
            BiPredicate<ImageV4Response, ImageV4Response> matchCondition, String runtimeVersion) {
        for (int i = 0; i < targetImages.size(); i++) {
            ImageV4Response targetImage = targetImages.get(i);
            for (int j = 0; j < sourceImages.size(); j++) {
                ImageV4Response sourceImage = sourceImages.get(j);
                if (matchCondition.test(sourceImage, targetImage)) {
                    Pair<String, String> pair = Pair.of(sourceImage.getUuid(), targetImage.getUuid());
                    LOGGER.info("Upgrade candidates found. Source image: {}, target image: {}", pair.getLeft(), pair.getRight());
                    return pair;
                }
            }
        }
        LOGGER.warn("No strict upgrade candidate found for runtime {} with the given condition. Falling back to oldest source and newest target.",
                runtimeVersion);
        return fallbackToOldestSourceAndNewestTarget(sourceImages, targetImages, runtimeVersion);
    }

    private Pair<String, String> fallbackToOldestSourceAndNewestTarget(List<ImageV4Response> sourceImages, List<ImageV4Response> targetImages,
            String runtimeVersion) {
        List<ImageV4Response> candidates = targetImages.isEmpty() ? sourceImages : targetImages;
        if (candidates.size() < 2) {
            throw new TestFailException(String.format("There is no upgrade candidate found for runtime %s. Available images: %s",
                    runtimeVersion, sourceImages));
        }
        List<ImageV4Response> sorted = candidates.stream()
                .sorted(Comparator.comparing(this::getCdhBuildNumber))
                .toList();
        ImageV4Response source = sorted.get(0);
        ImageV4Response target = sorted.get(sorted.size() - 1);
        if (source.getUuid().equals(target.getUuid()) || getCdhBuildNumber(source) == getCdhBuildNumber(target)) {
            throw new TestFailException(String.format("There is no upgrade candidate found for runtime %s. Available images: %s",
                    runtimeVersion, sourceImages));
        }
        Pair<String, String> pair = Pair.of(source.getUuid(), target.getUuid());
        LOGGER.info("Fallback upgrade candidates found. Source image: {}, target image: {}", pair.getLeft(), pair.getRight());
        return pair;
    }

    private long getCdhBuildNumber(ImageV4Response image) {
        return Long.parseLong(image.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey()));
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
                .filter(image -> !excludedByRuntimeRule(currentUpgradeRuntimeVersion, image))
                .filter(image -> image.getImageSetsByProvider().containsKey(provider))
                .filter(image -> image.getImageSetsByProvider().get(provider).containsKey(region) || !CloudPlatform.AWS.equalsIgnoreCase(provider))
                .sorted(Comparator.comparing(ImageV4Response::getCreated))
                .toList();
    }

    boolean excludedByRuntimeRule(String runtimeVersion, ImageV4Response image) {
        String repositoryVersion = getRepositoryVersion(image);
        if (repositoryVersion == null) {
            return false;
        }
        List<String> repositoryVersionExclusions = commonClusterManagerProperties.getUpgrade().getRuntimeImageRepositoryVersionExclusions()
                .getOrDefault(runtimeVersion, List.of());
        return repositoryVersionExclusions.stream().anyMatch(repositoryVersion::contains);
    }

    private String getRepositoryVersion(ImageV4Response image) {
        return Optional.ofNullable(image)
                .map(ImageV4Response::getStackDetails)
                .map(BaseStackDetailsV4Response::getRepository)
                .map(BaseStackRepoDetailsV4Response::getStack)
                .map(stackRepo -> stackRepo.get(StackRepoDetails.REPOSITORY_VERSION))
                .orElse(null);
    }

    boolean hasDifferentBuildNumber(ImageV4Response current, ImageV4Response target) {
        String currentCdhBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String targetCdhBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String currentCmVersion = current.getPackageVersions().get(ImagePackageVersion.CM.getKey());
        String targetCmVersion = target.getPackageVersions().get(ImagePackageVersion.CM.getKey());
        return new VersionComparator().compare(() -> targetCmVersion, () -> currentCmVersion) >= 0 &&
                Long.parseLong(targetCdhBuildNumber) > Long.parseLong(currentCdhBuildNumber);
    }

    boolean hasSameBuildNumber(ImageV4Response current, ImageV4Response target) {
        String currentCdhBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String targetCdhBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String currentCmBuildNumber = current.getPackageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        String targetCmBuildNumber = target.getPackageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        return Objects.equals(currentCdhBuildNumber, targetCdhBuildNumber) && Objects.equals(currentCmBuildNumber, targetCmBuildNumber)
                && !Objects.equals(current.getUuid(), target.getUuid())
                && (!Objects.equals(current.getOsType(), target.getOsType()) || current.getCreated() < target.getCreated());
    }
}
