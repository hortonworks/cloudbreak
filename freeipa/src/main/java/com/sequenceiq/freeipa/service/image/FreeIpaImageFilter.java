package com.sequenceiq.freeipa.service.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

@Component
public class FreeIpaImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaImageFilter.class);

    private static final String DEFAULT_REGION = "default";

    private static final String MAJOR_OS_UPGRADE_TARGET = "redhat8";

    @Inject
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    List<Image> filterImages(List<Image> candidateImages, FreeIpaImageFilterSettings imageFilterSettings) {
        LOGGER.debug("Filtering images with the following parameters: {}", imageFilterSettings);
        String platform = imageFilterSettings.platform();
        List<Image> filteredImages = candidateImages.stream().filter(filterImagesPredicate(imageFilterSettings)).toList();
        if (!filteredImages.isEmpty()) {
            List<Image> notApplicableImages = new ArrayList<>(candidateImages);
            notApplicableImages.removeAll(filteredImages);
            LOGGER.debug("Used filter for: | {} | Images filtered: {}", imageFilterSettings.os(),
                    notApplicableImages.stream().map(Image::toString).collect(Collectors.joining(", ")));
            return providerSpecificImageFilter.filterImages(platform, filteredImages);
        } else {
            LOGGER.warn("No FreeIPA image found with OS {}, falling back to the latest available one if such exists!", imageFilterSettings.os());
            return candidateImages;
        }
    }

    private Predicate<Image> filterImagesPredicate(FreeIpaImageFilterSettings imageFilterSettings) {
        return image -> {
            String targetOs = image.getOs();
            return (majorOsUpgradeAllowed(imageFilterSettings, targetOs) || targetOs.equalsIgnoreCase(imageFilterSettings.os())) &&
                    image.getImageSetsByProvider().containsKey(imageFilterSettings.platform()) &&
                    filterRegion(imageFilterSettings, image);
        };
    }

    private boolean majorOsUpgradeAllowed(FreeIpaImageFilterSettings imageFilterSettings, String targetOs) {
        return imageFilterSettings.allowMajorOsUpgrade() && MAJOR_OS_UPGRADE_TARGET.equals(targetOs);
    }

    boolean filterRegion(FreeIpaImageFilterSettings imageFilterSettings, Image img) {
        Set<String> regionSet = img.getImageSetsByProvider().get(imageFilterSettings.platform()).keySet();
        return CollectionUtils.containsAny(regionSet, imageFilterSettings.region(), DEFAULT_REGION);
    }
}
