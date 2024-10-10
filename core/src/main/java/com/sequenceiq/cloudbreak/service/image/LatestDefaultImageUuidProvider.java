package com.sequenceiq.cloudbreak.service.image;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class LatestDefaultImageUuidProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatestDefaultImageUuidProvider.class);

    private final ImageComparator comparator;

    @Inject
    public LatestDefaultImageUuidProvider(ImageComparator comparator) {
        this.comparator = comparator;
    }

    public Collection<String> getLatestDefaultImageUuids(Set<ImageCatalogPlatform> platforms, List<Image> defaultImages) {
        Collection<String> latestDefaultImageUuids = platforms.stream()
                .flatMap(p -> defaultImages.stream()
                        .filter(isPlatformMatching(p.nameToLowerCase()))
                        .collect(Collectors.toMap(this::getImageGroupingKey, Function.identity(), BinaryOperator.maxBy(comparator)))
                        .values().stream())
                .map(Image::getUuid)
                .collect(Collectors.toList());

        LOGGER.info("The following images are the latest default ones: {}", latestDefaultImageUuids);

        return latestDefaultImageUuids;
    }

    private String getImageGroupingKey(Image image) {
        return image.getOs() + ";" + image.getVersion() + ";" + Architecture.fromStringWithFallback(image.getArchitecture()).getName();
    }

    private Predicate<Image> isPlatformMatching(String platform) {
        return image -> image.getImageSetsByProvider().keySet().stream().anyMatch(platform::equalsIgnoreCase);
    }
}
