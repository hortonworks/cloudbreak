package com.sequenceiq.cloudbreak;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.OsType;

public class ImageCatalogMock {

    public static final String DEFAULT_IMAGE_CATALOG_PATH = "/upgrade-test-image-catalog.json";

    private final CloudbreakImageCatalogV3 imageCatalog = getImageCatalog();

    public Image getLatestImageByRuntimeAndPlatformAndOs(String runtime, String cloudPlatform, OsType osType) {
        return getAllCdhImages(cloudPlatform).stream()
                .filter(image -> runtime.equals(image.getVersion()) && osType.getOs().equalsIgnoreCase(image.getOs()))
                .max(Comparator.comparing(Image::getCreated))
                .orElseThrow(() -> new CloudbreakRuntimeException(
                        String.format("There is no image found in the catalog with runtime %s and platform %s", runtime, cloudPlatform)));
    }

    public Image getImage(String imageId) {
        return imageCatalog.getImages().getCdhImages().stream()
                .filter(image -> image.getUuid().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new CloudbreakRuntimeException(String.format("Image not found with id: %s", imageId)));
    }

    private CloudbreakImageCatalogV3 getImageCatalog() {
        String imageCatalogJson = FileReaderUtils.readFileFromClasspathQuietly(DEFAULT_IMAGE_CATALOG_PATH);
        return JsonUtil.readValueUnchecked(imageCatalogJson, CloudbreakImageCatalogV3.class);
    }

    public ImageFilterResult getAvailableImages(String currentImageId, String cloudPlatform) {
        Set<String> defaultImages = imageCatalog.getVersions().getCloudbreakVersions().stream()
                .map(CloudbreakVersion::getDefaults)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        List<Image> images = getAllCdhImages(cloudPlatform).stream()
                .filter(image -> defaultImages.contains(image.getUuid()) || image.getUuid().equals(currentImageId))
                .toList();
        return new ImageFilterResult(images);
    }

    public List<Image> getAllCdhImages(String cloudPlatform) {
        return imageCatalog.getImages().getCdhImages().stream()
                .filter(image -> image.getImageSetsByProvider().containsKey(cloudPlatform.toLowerCase()))
                .toList();
    }

}
