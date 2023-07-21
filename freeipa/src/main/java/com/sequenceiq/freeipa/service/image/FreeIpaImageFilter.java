package com.sequenceiq.freeipa.service.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Inject
    private SupportedOsService supportedOsService;

    List<Image> filterImages(List<Image> candidateImages, FreeIpaImageFilterSettings imageFilterSettings) {
        LOGGER.debug("Filtering images with the following parameters: {}", imageFilterSettings);
        String platform = imageFilterSettings.platform();
        List<Image> filteredImages = candidateImages.stream()
                .filter(img -> filterPlatformAndRegion(imageFilterSettings, img))
                .filter(img -> filterOs(imageFilterSettings, img))
                .toList();
        if (!filteredImages.isEmpty()) {
            List<Image> notApplicableImages = new ArrayList<>(candidateImages);
            notApplicableImages.removeAll(filteredImages);
            LOGGER.debug("Used filter: {} | Images filtered: {}", imageFilterSettings, notApplicableImages);
            return providerSpecificImageFilter.filterImages(platform, filteredImages);
        } else {
            LOGGER.warn("Could not find any FreeIPA image matching {} in {}", imageFilterSettings, candidateImages);
            throw new ImageNotFoundException(String.format("Could not find any FreeIPA image on platform '%s' in region '%s' with os '%s' in catalog '%s'",
                    imageFilterSettings.platform(), imageFilterSettings.region(), imageFilterSettings.os(), imageFilterSettings.catalog()));
        }
    }

    boolean filterPlatformAndRegion(FreeIpaImageFilterSettings imageFilterSettings, Image image) {
        Map<String, Map<String, String>> imageSetsByProvider = image.getImageSetsByProvider();
        return imageSetsByProvider.containsKey(imageFilterSettings.platform())
                && CollectionUtils.containsAny(imageSetsByProvider.get(imageFilterSettings.platform()).keySet(), imageFilterSettings.region(), DEFAULT_REGION);
    }

    private boolean filterOs(FreeIpaImageFilterSettings imageFilterSettings, Image image) {
        String targetOs = image.getOs();
        return supportedOsService.isSupported(targetOs) &&
                (imageFilterSettings.os() == null
                        || majorOsUpgradeAllowed(imageFilterSettings, targetOs)
                        || targetOs.equalsIgnoreCase(imageFilterSettings.os()));
    }

    private boolean majorOsUpgradeAllowed(FreeIpaImageFilterSettings imageFilterSettings, String targetOs) {
        return imageFilterSettings.allowMajorOsUpgrade() && MAJOR_OS_UPGRADE_TARGET.equals(targetOs);
    }
}
