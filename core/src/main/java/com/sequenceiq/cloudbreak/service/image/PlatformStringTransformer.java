package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogPlatform.imageCatalogPlatform;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogPlatform;

@Service
public class PlatformStringTransformer {

    private static final String GOV = "_gov";

    public ImageCatalogPlatform getPlatformStringForImageCatalog(String cloudPlatform, String variant) {
        String platformVariant = variant;
        String platform = cloudPlatform.toLowerCase();
        if (Strings.isNullOrEmpty(platformVariant)) {
            return imageCatalogPlatform(platform);
        } else if (platformVariant.toLowerCase().endsWith(GOV)) {
            return imageCatalogPlatform(platformVariant);
        } else {
            return imageCatalogPlatform(platform);
        }
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalog(ImageCatalogPlatform imageCatalogPlatform, String variant) {
        return getPlatformStringForImageCatalog(imageCatalogPlatform.nameToLowerCase(), variant);
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalog(String cloudPlatform, boolean govCloud) {
        return imageCatalogPlatform(govCloudSegmentRequired(cloudPlatform) && govCloud ? cloudPlatform.concat(GOV).toUpperCase() : cloudPlatform.toUpperCase());
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalog(ImageCatalogPlatform imageCatalogPlatform, boolean govCloud) {
        return getPlatformStringForImageCatalog(imageCatalogPlatform.nameToLowerCase(), govCloud);
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalogByRegion(String cloudPlatform, String region) {
        if (region.toLowerCase().contains("-gov-")) {
            return imageCatalogPlatform(govCloudSegmentRequired(cloudPlatform) ? cloudPlatform.concat(GOV).toUpperCase() : cloudPlatform.toUpperCase());
        }
        return imageCatalogPlatform(cloudPlatform);
    }

    public Set<ImageCatalogPlatform> getPlatformStringForImageCatalogSet(String cloudPlatform, String variant) {
        return Set.of(getPlatformStringForImageCatalog(cloudPlatform, variant));
    }

    private boolean govCloudSegmentRequired(String cloudPlatform) {
        return !cloudPlatform.toLowerCase().endsWith(GOV);
    }
}
