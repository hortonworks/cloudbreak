package com.sequenceiq.freeipa.service.image;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ImageFilter;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.converter.image.CoreImageToImageConverter;
import com.sequenceiq.freeipa.converter.image.ImageToCoreImageConverter;

@Component
public class ProviderSpecificImageFilter {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CoreImageToImageConverter coreImageToImageConverter;

    @Inject
    private ImageToCoreImageConverter imageToCoreImageConverter;

    public List<Image> filterImages(String cloudPlatform, List<Image> imageList) {
        Optional<ImageFilter> filterForProvider = getImageFilter(cloudPlatform);
        if (filterForProvider.isEmpty()) {
            return imageList;
        }

        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> coreImageList =
                imageList.stream().
                        map(imageToCoreImageConverter::convert).
                        collect(Collectors.toList());
        List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> filteredImageList = filterForProvider.get().filterImages(coreImageList);
        return filteredImageList.stream().
                map(coreImageToImageConverter::convert).
                collect(Collectors.toList());
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
