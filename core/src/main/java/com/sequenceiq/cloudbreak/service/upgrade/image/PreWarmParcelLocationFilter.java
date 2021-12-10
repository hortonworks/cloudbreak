package com.sequenceiq.cloudbreak.service.upgrade.image;

import static org.springframework.util.StringUtils.hasText;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class PreWarmParcelLocationFilter implements PackageLocationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreWarmParcelLocationFilter.class);

    private static final String URL_PREFIX = "http";

    private static final String IGNORED_PARCEL_NAME = "CDH";

    @Override
    public boolean filterImage(Image image, Image currentImage, ImageFilterParams imageFilterParams) {
        if (StackType.WORKLOAD.equals(imageFilterParams.getStackType())) {
            if (isInvalidImage(image)) {
                LOGGER.debug("Image or some part of it is null or not contains the parcel urls: {}", image);
                return false;
            } else {
                Set<String> requiredParcels = getRequiredParcels(imageFilterParams);
                LOGGER.debug("Stack related required parcels: {}", requiredParcels);
                return requiredParcels.isEmpty() || isEligibleForUpgrade(image, requiredParcels);
            }
        } else {
            LOGGER.debug("Skip filtering because the stack type is {}", imageFilterParams.getStackType());
            return true;
        }
    }

    private boolean isInvalidImage(Image image) {
        return image == null || image.getPreWarmParcels() == null || image.getPreWarmParcels().isEmpty();
    }

    private Set<String> getRequiredParcels(ImageFilterParams imageFilterParams) {
        return Optional.ofNullable(imageFilterParams.getStackRelatedParcels())
                .map(Map::keySet)
                .orElse(Collections.emptySet())
                .stream()
                .filter(parcel -> !IGNORED_PARCEL_NAME.equals(parcel))
                .collect(Collectors.toSet());
    }

    private boolean isEligibleForUpgrade(Image image, Set<String> requiredParcels) {
        Map<String, Set<String>> preWarmParcelsByParcelName = getPreWarmParcelsByParcelName(image, requiredParcels);
        LOGGER.debug("Available pre warm parcels: {} on image: {}", preWarmParcelsByParcelName, image.getUuid());
        return allAvailableRequiredPreWarmParcelHasValue(preWarmParcelsByParcelName)
                && allAvailableRequiredPreWarmParcelUrlLocationIsValid(preWarmParcelsByParcelName);
    }

    private Map<String, Set<String>> getPreWarmParcelsByParcelName(Image image, Set<String> requiredParcels) {
        List<List<String>> imagePreWarmParcels = image.getPreWarmParcels();
        LOGGER.debug("Available preWarmParcels: {}", imagePreWarmParcels);
        return requiredParcels.stream()
                .filter(parcel -> isParcelExistsOnImage(image, parcel))
                .collect(Collectors.toMap(
                        parcelName -> parcelName,
                        parcelName -> filterUrlsByStackRelatedParcel(image, parcelName)));
    }

    private boolean allAvailableRequiredPreWarmParcelHasValue(Map<String, Set<String>> preWarmParcelsByParcelName) {
        return preWarmParcelsByParcelName.entrySet().stream().noneMatch(parcels -> parcels.getValue().isEmpty());
    }

    private boolean allAvailableRequiredPreWarmParcelUrlLocationIsValid(Map<String, Set<String>> preWarmParcelsByParcelName) {
        return preWarmParcelsByParcelName.entrySet().stream()
                .allMatch(parcels -> parcels.getValue().stream()
                        .allMatch(parcelUrl -> URL_PATTERN.matcher(parcelUrl).find()));
    }

    private boolean isParcelExistsOnImage(Image image, String requiredParcel) {
        return getMatchingParcelStreamFromImage(image, requiredParcel).anyMatch(StringUtils::hasText);
    }

    private Set<String> filterUrlsByStackRelatedParcel(Image image, String requiredParcel) {
        return getMatchingParcelStreamFromImage(image, requiredParcel)
                .filter(parcel -> hasText(parcel) && parcel.startsWith(URL_PREFIX))
                .collect(Collectors.toSet());
    }

    private Stream<String> getMatchingParcelStreamFromImage(Image image, String requiredParcel) {
        return Optional.ofNullable(image)
                .map(Image::getPreWarmParcels).orElse(Collections.emptyList())
                .stream()
                .filter(filterByStackRelatedParcel(requiredParcel))
                .flatMap(List::stream);
    }

    private Predicate<List<String>> filterByStackRelatedParcel(String requiredParcel) {
        return parcelList -> parcelList.stream()
                .anyMatch(relatedParcel -> hasText(relatedParcel) && relatedParcel.toLowerCase().startsWith(requiredParcel.toLowerCase()));
    }
}
