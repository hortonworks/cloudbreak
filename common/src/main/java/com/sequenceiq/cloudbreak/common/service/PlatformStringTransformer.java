package com.sequenceiq.cloudbreak.common.service;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Service
public class PlatformStringTransformer {

    public ImageCatalogPlatform getPlatformStringForImageCatalog(String cloudPlatform, String variant) {
        String lowerCasePlatform = cloudPlatform.toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(variant)) {
            return imageCatalogPlatform(lowerCasePlatform);
        } else if (variant.toLowerCase(Locale.ROOT).endsWith(CommonGovService.GOV)) {
            return getPlatformStringForImageCatalog(lowerCasePlatform, true);
        } else {
            return imageCatalogPlatform(lowerCasePlatform);
        }
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalog(ImageCatalogPlatform imageCatalogPlatform, String variant) {
        return getPlatformStringForImageCatalog(imageCatalogPlatform.nameToLowerCase(), variant);
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalog(String cloudPlatform, boolean govCloud) {
        return imageCatalogPlatform(govCloudSegmentRequired(cloudPlatform) && govCloud ?
                cloudPlatform.concat(CommonGovService.GOV).toUpperCase(Locale.ROOT) : cloudPlatform.toUpperCase(Locale.ROOT));
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalog(ImageCatalogPlatform imageCatalogPlatform, boolean govCloud) {
        return getPlatformStringForImageCatalog(imageCatalogPlatform.nameToLowerCase(), govCloud);
    }

    public ImageCatalogPlatform getPlatformStringForImageCatalogByRegion(String cloudPlatform, String region) {
        if (StringUtils.isNotBlank(region) && region.toLowerCase(Locale.ROOT).contains("-gov-")) {
            return imageCatalogPlatform(govCloudSegmentRequired(cloudPlatform) ?
                    cloudPlatform.concat(CommonGovService.GOV).toUpperCase(Locale.ROOT) : cloudPlatform.toUpperCase(Locale.ROOT));
        }
        return imageCatalogPlatform(cloudPlatform);
    }

    public Set<ImageCatalogPlatform> getPlatformStringForImageCatalogSet(String cloudPlatform, String variant) {
        return Set.of(getPlatformStringForImageCatalog(cloudPlatform, variant));
    }

    private boolean govCloudSegmentRequired(String cloudPlatform) {
        return !cloudPlatform.toLowerCase().endsWith(CommonGovService.GOV);
    }
}
