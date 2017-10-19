package com.sequenceiq.cloudbreak.service.image;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;

@Component
public class ImageCatalogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    private static final String RELEASED_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+";

    private static final String UNRELEASED_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+-[d,r][c,e][v]?";

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    public Images getImages(String provider) throws CloudbreakImageCatalogException {
        return getImages(provider, cbVersion);
    }

    public List<Image> getBaseImages(String platform) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Images images = getImages(platform);
        List<Image> baseImages = images.getBaseImages();
        if (baseImages.isEmpty()) {
            String msg = String.format("Could not find any base image for platform '%s' and Cloudbreak version '%s'.", platform, cbVersion);
            throw new CloudbreakImageNotFoundException(msg);
        }
        return baseImages;
    }

    public Image getImage(String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Images images = imageCatalogProvider.getImageCatalogV2().getImages();
        Optional<? extends Image> image = getImage(imageId, images);
        if (!image.isPresent()) {
            images = imageCatalogProvider.getImageCatalogV2(true).getImages();
            image = getImage(imageId, images);
        }
        if (!image.isPresent()) {
            throw new CloudbreakImageNotFoundException(String.format("Could not find any image with id: '%s'.", imageId));
        }
        return image.get();
    }

    private Optional<? extends Image> getImage(String imageId, Images images) throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        Optional<? extends Image> image = findFirstWithImageId(imageId, images.getBaseImages());
        if (!image.isPresent()) {
            image = findFirstWithImageId(imageId, images.getHdpImages());
        }
        if (!image.isPresent()) {
            image = findFirstWithImageId(imageId, images.getHdfImages());
        }
        return image;
    }

    private Optional<? extends Image> findFirstWithImageId(String imageId, Collection<? extends Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
    }

    public Images getImages(String platform, String cbVersion) throws CloudbreakImageCatalogException {
        LOGGER.info("Determine images for platform: '{}' and Cloudbreak version: '{}'.", platform, cbVersion);
        Images images = new Images();
        CloudbreakImageCatalogV2 imageCatalog = imageCatalogProvider.getImageCatalogV2();
        if (imageCatalog != null) {
            Set<String> vMImageUUIDs = new HashSet<>();
            List<CloudbreakVersion> cloudbreakVersions = imageCatalog.getVersions().getCloudbreakVersions();
            List<CloudbreakVersion> exactMatchedImgs = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbVersion)).collect(Collectors.toList());

            if (!exactMatchedImgs.isEmpty()) {
                exactMatchedImgs.forEach(cloudbreakVersion -> vMImageUUIDs.addAll(cloudbreakVersion.getImageIds()));
            } else {
                vMImageUUIDs.addAll(prefixMatchForCBVersion(cbVersion, cloudbreakVersions));
            }

            List<Image> baseImages = imageCatalog.getImages().getBaseImages().stream()
                    .filter(img -> vMImageUUIDs.contains(img.getUuid()))
                    .filter(img -> img.getImageSetsByProvider().keySet().stream().anyMatch(p -> p.equalsIgnoreCase(platform)))
                    .collect(Collectors.toList());
            images.setBaseImages(baseImages);

            List<Image> hdpImages = imageCatalog.getImages().getHdpImages().stream()
                    .filter(img -> vMImageUUIDs.contains(img.getUuid()))
                    .filter(img -> img.getImageSetsByProvider().keySet().stream().anyMatch(p -> p.equalsIgnoreCase(platform)))
                    .collect(Collectors.toList());
            images.setHdpImages(hdpImages);

            List<Image> hdfImages = imageCatalog.getImages().getHdfImages().stream()
                    .filter(img -> vMImageUUIDs.contains(img.getUuid()))
                    .filter(img -> img.getImageSetsByProvider().keySet().stream().anyMatch(p -> p.equalsIgnoreCase(platform)))
                    .collect(Collectors.toList());
            images.setHdfImages(hdfImages);
        }
        return images;
    }

    private Set<String> prefixMatchForCBVersion(String cbVersion, List<CloudbreakVersion> cloudbreakVersions) {
        Set<String> vMImageUUIDs = new HashSet<>();
        String unReleasedVersion = extractCbVersion(UNRELEASED_VERSION_PATTERN, cbVersion);
        boolean versionIsReleased = unReleasedVersion.equals(cbVersion);

        if (!versionIsReleased) {
            Set<CloudbreakVersion> unReleasedCbVersions = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().stream().anyMatch(aVersion -> aVersion.startsWith(unReleasedVersion)))
                    .collect(Collectors.toSet());
            unReleasedCbVersions.stream().forEach(cloudbreakVersion -> vMImageUUIDs.addAll(cloudbreakVersion.getImageIds()));
        }

        if (versionIsReleased || vMImageUUIDs.isEmpty()) {
            String releasedVersion = extractCbVersion(RELEASED_VERSION_PATTERN, cbVersion);
            Set<CloudbreakVersion> releasedCbVersions = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(releasedVersion)).collect(Collectors.toSet());
            releasedCbVersions.stream().forEach(cloudbreakVersion -> vMImageUUIDs.addAll(cloudbreakVersion.getImageIds()));
        }
        return vMImageUUIDs;
    }

    private String extractCbVersion(String pattern, String cbVersion) {
        Matcher matcher = Pattern.compile(pattern).matcher(cbVersion);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return cbVersion;
    }
}
