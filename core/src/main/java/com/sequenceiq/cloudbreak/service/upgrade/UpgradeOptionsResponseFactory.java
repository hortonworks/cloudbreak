package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class UpgradeOptionsResponseFactory {

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentVersionProvider componentVersionProvider;

    public UpgradeV4Response createV4Response(Image currentImage, ImageFilterResult filteredImages, String cloudPlatform, String region,
            String imageCatalogName) {
        return new UpgradeV4Response(
                createImageInfoFromCurrentImage(currentImage, cloudPlatform, region, imageCatalogName),
                createImageInfoFromFilteredImages(filteredImages.getImages(), imageCatalogName, cloudPlatform, region),
                filteredImages.getReason());
    }

    private ImageInfoV4Response createImageInfoFromCurrentImage(Image currentImage, String cloudPlatform, String region, String imageCatalogName) {
        return new ImageInfoV4Response(getImageName(currentImage, cloudPlatform, region), currentImage.getUuid(), imageCatalogName, currentImage.getCreated(),
                currentImage.getDate(), getComponentVersions(currentImage.getPackageVersions(), currentImage.getOs(), currentImage.getDate()));
    }

    private List<ImageInfoV4Response> createImageInfoFromFilteredImages(List<Image> filteredImages, String imageCatalogName, String cloudPlatform,
            String region) {
        return filteredImages.stream()
                .map(image -> createImageInfo(image, imageCatalogName, cloudPlatform, region)).sorted().collect(Collectors.toList());
    }

    private ImageInfoV4Response createImageInfo(Image image, String imageCatalogName, String cloudPlatform, String region) {
        return new ImageInfoV4Response(getImageName(image, cloudPlatform, region), image.getUuid(), imageCatalogName, image.getCreated(), image.getDate(),
                getComponentVersions(image.getPackageVersions(), image.getOs(), image.getDate()));
    }

    private ImageComponentVersions getComponentVersions(Map<String, String> packageVersions, String os, String osPatchLevel) {
        return componentVersionProvider.getComponentVersions(packageVersions, os, osPatchLevel);
    }

    private String getImageName(Image image, String cloudPlatform, String region) {
        String imageName;
        try {
            imageName = imageService.determineImageName(cloudPlatform, region, image);
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException(String.format("Image (%s) name not found", image.getUuid()), e);
        }
        return imageName;
    }
}
