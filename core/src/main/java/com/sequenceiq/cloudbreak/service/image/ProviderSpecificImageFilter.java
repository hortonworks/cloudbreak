package com.sequenceiq.cloudbreak.service.image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ImageFilter;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class ProviderSpecificImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSpecificImageFilter.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public List<Image> filterImages(Collection<ImageCatalogPlatform> platforms, List<Image> imageList) {
        Set<Image> uniqueImages = platforms.stream()
                .map(platform -> filterImages(platform.name(), imageList))
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        LOGGER.debug("Provider specific image filtering is done, there are {} images out of {}.", uniqueImages.size(), imageList.size());

        return new ArrayList<>(uniqueImages);
    }

    private List<Image> filterImages(String cloudPlatform, List<Image> imageList) {
        List<Image> cloudPlatformImages = getCloudPlatformImages(cloudPlatform, imageList);
        Optional<ImageFilter> filterForProvider = getImageFilter(cloudPlatform);
        List<Image> cloudPlatformFilteredImages = filterForProvider
                .map(imageFilter -> imageFilter.filterImages(cloudPlatformImages))
                .orElse(cloudPlatformImages);

        LOGGER.debug("{} specific image filtering is done, there are {} images out of {}.",
                cloudPlatform, cloudPlatformFilteredImages.size(), cloudPlatformImages.size());

        return cloudPlatformFilteredImages;
    }

    private Optional<ImageFilter> getImageFilter(String cloudPlatform) {
        String platform = cloudPlatform.toUpperCase(Locale.ROOT);
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(platform),
                Variant.variant(platform));

        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant))
                .map(CloudConnector::parameters)
                .flatMap(PlatformParameters::imageFilter);
    }

    private List<Image> getCloudPlatformImages(String cloudPlatform, List<Image> imageList) {
        List<Image> cloudPlatformImages = imageList.stream()
                .filter(image -> image.getImageSetsByProvider().containsKey(cloudPlatform.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        LOGGER.debug("{} image filtering is done, there are {} images out of {}.", cloudPlatform, cloudPlatformImages.size(), imageList.size());

        return cloudPlatformImages;
    }
}
