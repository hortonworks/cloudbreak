package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage.MARKETPLACE_REGION;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.ImageFilter;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component("AzureImageFilter")
public class AzureImageFilter implements ImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageFilter.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Override
    public List<Image> filterImages(List<Image> imageList) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.azureMarketplaceImagesEnabled(accountId)) {
            return excludeMarketplaceImages(imageList);
        } else if (entitlementService.azureOnlyMarketplaceImagesEnabled(accountId)) {
            return includeOnlyMarketplaceImages(imageList);
        } else {
            LOGGER.debug("CDP_AZURE_IMAGE_MARKETPLACE_ONLY is not granted, skipping the filtering and returning all the {} images", imageList.size());
            return imageList;
        }
    }

    private List<Image> includeOnlyMarketplaceImages(List<Image> imageList) {
        LOGGER.debug("Only Azure Marketplace images are permitted, filtering for those..");
        List<Image> filteredImageList = imageList.stream()
                .filter(image -> image.getImageSetsByProvider().containsKey(AZURE.name().toLowerCase(Locale.ROOT)))
                .filter(this::checkIfMarketplaceImage)
                .map(this::removeNonMarketplaceRegions)
                .collect(Collectors.toList());
        LOGGER.debug("After filtering, the following image ids remained available for selection: {}", getImageIds(filteredImageList));
        return filteredImageList;
    }

    private List<Image> excludeMarketplaceImages(List<Image> imageList) {
        LOGGER.debug("Azure Marketplace images are not permitted, filtering those out..");
        List<Image> filteredImageList = imageList.stream()
                .filter(image -> image.getImageSetsByProvider().containsKey(AZURE.name().toLowerCase(Locale.ROOT)))
                .map(this::removeMarketplaceRegions)
                .collect(Collectors.toList());
        LOGGER.debug("After filtering, the following image ids remained available for selection: {}", getImageIds(filteredImageList));
        return filteredImageList;
    }

    private static List<String> getImageIds(List<Image> filteredImageList) {
        return filteredImageList.stream()
                .map(Image::getUuid)
                .collect(Collectors.toList());
    }

    private boolean checkIfMarketplaceImage(Image image) {
        return image.getImageSetsByProvider().values()
                .stream()
                .filter(section -> section.containsKey(MARKETPLACE_REGION))
                .map(section -> section.get(MARKETPLACE_REGION))
                .map(azureMarketplaceImageProviderService::hasMarketplaceFormat)
                .findFirst()
                .orElse(false);
    }

    private Image removeNonMarketplaceRegions(Image image) {
        Map<String, Map<String, String>> filteredMap = image.getImageSetsByProvider().entrySet()
                .stream()
                .filter(i -> i.getValue().containsKey(MARKETPLACE_REGION))
                .collect(Collectors.toMap(Map.Entry::getKey, i -> Map.of(MARKETPLACE_REGION, i.getValue().get(MARKETPLACE_REGION))));

        return new Image(image, filteredMap);
    }

    private Image removeMarketplaceRegions(Image image) {
        Map<String, Map<String, String>> filteredMap = image.getImageSetsByProvider().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> computeRegionMapWithoutMarketplace(entry.getValue())));

        return new Image(image, filteredMap);
    }

    private Map<String, String> computeRegionMapWithoutMarketplace(Map<String, String> regionToImageNameMap) {
        return regionToImageNameMap.entrySet().stream()
                .filter(entry -> !MARKETPLACE_REGION.equals(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
