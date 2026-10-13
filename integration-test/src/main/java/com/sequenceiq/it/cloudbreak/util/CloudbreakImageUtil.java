package com.sequenceiq.it.cloudbreak.util;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class CloudbreakImageUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakImageUtil.class);

    @Inject
    private ImageCatalogDownloader imageCatalogDownloader;

    public String getNewestBaseImageId(String catalogUrl, String cloudProvider, String region, String osType, Architecture architecture) {
        CloudbreakImageCatalogV3 imageCatalog = imageCatalogDownloader.download(catalogUrl, CloudbreakImageCatalogV3.class);
        Image newestBaseImage = imageCatalog.getImages().getBaseImages().stream()
                .filter(image -> hasImageInRegion(image, cloudProvider, region))
                .filter(image -> osType.equalsIgnoreCase(image.getOsType()))
                .filter(image -> architecture == Architecture.fromStringWithFallback(image.getArchitecture()))
                .max(Comparator.comparing(Image::getCreated, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow(() -> new TestFailException(String.format(
                        "Not found any '%s' %s base image on %s (region: %s). Please burn an image in %s catalog.",
                        osType, architecture, cloudProvider, region, catalogUrl)));
        Log.log(LOGGER, " Selected newest '%s' base image on %s (region: %s) from %s catalog | ID: %s | Date: %s ",
                osType, cloudProvider, region, catalogUrl, newestBaseImage.getUuid(), newestBaseImage.getDate());
        return newestBaseImage.getUuid();
    }

    private boolean hasImageInRegion(Image image, String cloudProvider, String region) {
        return image.getImageSetsByProvider().getOrDefault(cloudProvider.toLowerCase(Locale.ROOT), Map.of()).containsKey(region);
    }
}
