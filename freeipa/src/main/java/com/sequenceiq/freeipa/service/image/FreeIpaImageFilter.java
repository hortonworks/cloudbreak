package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.RHEL8;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

@Component
public class FreeIpaImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaImageFilter.class);

    private static final String DEFAULT_REGION = "default";

    private static final String MAJOR_OS_UPGRADE_TARGET = RHEL8.getOs();

    @Inject
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Inject
    private SupportedOsService supportedOsService;

    @Inject
    private PreferredOsService preferredOsService;

    public List<Image> filterImages(List<Image> candidateImages, FreeIpaImageFilterSettings imageFilterSettings) {
        LOGGER.debug("Filtering images with the following parameters: {}", imageFilterSettings);
        if (StringUtils.isNotBlank(imageFilterSettings.currentImageId())) {
            return candidateImages.stream()
                    .filter(img -> filterPlatformAndRegion(imageFilterSettings, img))
                    .filter(img -> supportedOsService.isSupported(img.getOs()))
                    //It's not clear why we check the provider image reference (eg. the AMI in case of AWS) as imageId here.
                    //For safety and backward compatibility reasons the check remains here but should be checked if it really needed.
                    .filter(img -> hasSameUuid(imageFilterSettings.currentImageId(), img) || isMatchingImageIdInRegion(imageFilterSettings, img))
                    .collect(Collectors.toList());
        } else {
            String platform = imageFilterSettings.platform();
            List<Image> filteredImages = candidateImages.stream()
                    .filter(img -> filterPlatformAndRegion(imageFilterSettings, img))
                    .filter(img -> filterOs(imageFilterSettings, img))
                    .filter(image -> imageFilterSettings.architecture() == null || architectureMatches(image, imageFilterSettings))
                    .toList();
            if (!filteredImages.isEmpty()) {
                List<Image> notApplicableImages = new ArrayList<>(candidateImages);
                notApplicableImages.removeAll(filteredImages);
                LOGGER.debug("Used filter: {} | Images filtered: {}", imageFilterSettings, notApplicableImages);
                return providerSpecificImageFilter.filterImages(platform, filteredImages);
            } else {
                LOGGER.warn("Could not find any FreeIPA image matching {} in {}", imageFilterSettings, candidateImages);
                throw new ImageNotFoundException(String.format("Could not find any FreeIPA image on platform '%s' in region '%s' with os '%s' in catalog '%s'",
                        imageFilterSettings.platform(), imageFilterSettings.region(), imageFilterSettings.targetOs(), imageFilterSettings.catalog()));
            }
        }
    }

    private static boolean architectureMatches(Image image, FreeIpaImageFilterSettings imageFilterSettings) {
        if (image.getArchitecture() == null && Architecture.X86_64.equals(imageFilterSettings.architecture())) {
            return true;
        } else {
            return imageFilterSettings.architecture().getName().equalsIgnoreCase(image.getArchitecture());
        }
    }

    private boolean filterPlatformAndRegion(FreeIpaImageFilterSettings imageFilterSettings, Image image) {
        Map<String, Map<String, String>> imageSetsByProvider = image.getImageSetsByProvider();
        return imageSetsByProvider.containsKey(imageFilterSettings.platform())
                && CollectionUtils.containsAny(imageSetsByProvider.get(imageFilterSettings.platform()).keySet(), imageFilterSettings.region(), DEFAULT_REGION);
    }

    private boolean filterOs(FreeIpaImageFilterSettings imageFilterSettings, Image image) {
        String candidateImageOs = image.getOs();
        return supportedOsService.isSupported(candidateImageOs) &&
                (StringUtils.isBlank(imageFilterSettings.targetOs()) || imageFilterSettings.targetOs().equalsIgnoreCase(candidateImageOs))
                && (majorOsUpgradeAllowed(imageFilterSettings, candidateImageOs)
                || (candidateImageOs.equalsIgnoreCase(imageFilterSettings.currentOs()) || StringUtils.isBlank(imageFilterSettings.currentOs())));
    }

    private boolean majorOsUpgradeAllowed(FreeIpaImageFilterSettings imageFilterSettings, String targetOs) {
        return imageFilterSettings.allowMajorOsUpgrade() && MAJOR_OS_UPGRADE_TARGET.equals(targetOs);
    }

    private Boolean isMatchingImageIdInRegion(FreeIpaImageFilterSettings imageFilterSettings, Image img) {
        return Optional.ofNullable(img.getImageSetsByProvider().get(imageFilterSettings.platform()).get(imageFilterSettings.region()))
                .map(reg -> reg.equalsIgnoreCase(imageFilterSettings.currentImageId()))
                .orElse(false);
    }

    private boolean hasSameUuid(String imageId, Image img) {
        return img.getUuid().equalsIgnoreCase(imageId);
    }

    public Optional<Image> findMostRecentImage(List<Image> compatibleImages) {
        LOGGER.debug("Not found any image compatible with the application version. Falling back to the most recent image.");
        return compatibleImages.stream()
                .max(newestImageWithPreferredOs());
    }

    public Comparator<Image> newestImageWithPreferredOs() {
        String preferredOs = preferredOsService.getDefaultOs();
        return Comparator.<Image, Integer>comparing(img -> preferredOs.equalsIgnoreCase(img.getOs()) ? 1 : 0)
                .thenComparing(Image::getDate);
    }
}
