package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ParcelType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Component
public class UpgradeOptionsResponseFactory {

    @Inject
    private ImageService imageService;

    public UpgradeOptionsV4Response createV4Response(Image currentImage, Images filteredImages, String cloudPlatform, String region, String imageCatalogName) {
        return new UpgradeOptionsV4Response(
                createImageInfoFromCurrentImage(currentImage, cloudPlatform, region, imageCatalogName),
                createImageInfoFromFilteredImages(filteredImages, imageCatalogName, cloudPlatform, region));
    }

    private ImageInfoV4Response createImageInfoFromCurrentImage(Image currentImage, String cloudPlatform, String region, String imageCatalogName) {
        return new ImageInfoV4Response(getImageName(currentImage, cloudPlatform, region), currentImage.getUuid(), imageCatalogName, currentImage.getCreated(),
                getComponentVersions(currentImage.getPackageVersions()));
    }

    private List<ImageInfoV4Response> createImageInfoFromFilteredImages(Images filteredImages, String imageCatalogName, String cloudPlatform, String region) {
        return filteredImages.getCdhImages().stream()
                .map(image -> createImageInfo(image, imageCatalogName, cloudPlatform, region))
                .collect(Collectors.toList());
    }

    private ImageInfoV4Response createImageInfo(Image image, String imageCatalogName, String cloudPlatform, String region) {
        return new ImageInfoV4Response(getImageName(image, cloudPlatform, region), image.getUuid(), imageCatalogName, image.getCreated(),
                getComponentVersions(image.getPackageVersions()));
    }

    private Map<String, String> getComponentVersions(Map<String, String> packageVersions) {
        return Map.of(
                ClusterManagerVariant.CLOUDERA_MANAGER.getName(), packageVersions.get("cm"),
                ParcelType.CLOUDERA_RUNTIME.getName(), packageVersions.get("stack"));
    }

    private String getImageName(Image image, String cloudPlatform, String region) {
        String imageName;
        try {
            imageName = imageService.determineImageName(cloudPlatform, region, image);
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image name not found", e);
        }
        return imageName;
    }
}
