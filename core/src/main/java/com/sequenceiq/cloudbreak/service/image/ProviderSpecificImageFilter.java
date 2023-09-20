package com.sequenceiq.cloudbreak.service.image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public List<Image> filterImages(Collection<ImageCatalogPlatform> platforms, List<Image> imageList) {
        Set<Image> uniqueImages = platforms.stream()
                .map(platform -> filterImages(platform.name(), imageList))
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        return new ArrayList<>(uniqueImages);
    }

    private List<Image> filterImages(String cloudPlatform, List<Image> imageList) {
        Optional<ImageFilter> filterForProvider = getImageFilter(cloudPlatform);
        return filterForProvider
                .map(imageFilter -> imageFilter.filterImages(imageList))
                .orElse(imageList);
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
}
