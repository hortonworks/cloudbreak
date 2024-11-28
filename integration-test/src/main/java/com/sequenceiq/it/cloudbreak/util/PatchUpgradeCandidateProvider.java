package com.sequenceiq.it.cloudbreak.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class PatchUpgradeCandidateProvider {

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    public Pair<String, String> getPatchUpgradeSourceAndCandidate(TestContext testContext) {
        String runtimeVersion = commonClusterManagerProperties.getUpgrade().getDistroXUpgradeCurrentVersion(testContext.getCloudProvider().getGovCloud());
        List<ImageV4Response> cdhImages = getCdhImages(testContext, runtimeVersion);

        for (int i = 0; i < cdhImages.size() - 1; i++) {
            ImageV4Response sourceImage = cdhImages.get(i);
            ImageV4Response targetImage = cdhImages.get(i + 1);
            if (hasDifferentBuildNumber(sourceImage, targetImage)) {
                return Pair.of(sourceImage.getUuid(), targetImage.getUuid());
            }
        }
        throw new TestFailException(String.format("There is no patch upgrade candidate found for runtime %s. Available images: %s", runtimeVersion, cdhImages));
    }

    private List<ImageV4Response> getCdhImages(TestContext testContext, String runtimeVersion) {
        List<ImageV4Response> cdhImages = new ArrayList<>();
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getV4WithAdvertisedImages())
                .then((context, entity, cloudbreakClient) -> {
                    List<ImageV4Response> sortedImages = getImagesByRuntimeAndCloudProvider(testContext, runtimeVersion, entity);
                    cdhImages.addAll(sortedImages);
                    return entity;
                })
                .validate();
        return cdhImages;
    }

    private List<ImageV4Response> getImagesByRuntimeAndCloudProvider(TestContext testContext, String currentUpgradeRuntimeVersion, ImageCatalogTestDto entity) {
        return entity.getResponse().getImages().getCdhImages().stream()
                .filter(im -> im.getVersion().equals(currentUpgradeRuntimeVersion)
                        && im.getImageSetsByProvider().containsKey(testContext.getCloudProvider().getCloudPlatform().name().toLowerCase()))
                .sorted(Comparator.comparing(ImageV4Response::getCreated))
                .toList();
    }

    private boolean hasDifferentBuildNumber(ImageV4Response current, ImageV4Response next) {
        String currentBuild = current.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        String nextBuild = next.getPackageVersions().get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey());
        return !Objects.equals(currentBuild, nextBuild);
    }
}
