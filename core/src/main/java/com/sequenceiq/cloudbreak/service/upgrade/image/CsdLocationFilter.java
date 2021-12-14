package com.sequenceiq.cloudbreak.service.upgrade.image;

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
public class CsdLocationFilter implements PackageLocationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsdLocationFilter.class);

    private static final String IGNORED_PARCEL_NAME = "CDH";

    @Override
    public boolean filterImage(Image image, Image currentImage, ImageFilterParams imageFilterParams) {
        if (StackType.WORKLOAD.equals(imageFilterParams.getStackType())) {
            if (isInvalidImage(image)) {
                LOGGER.debug("Image or CSD parcels are not present. Image: {}", image);
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
        return image == null || image.getPreWarmCsd() == null || image.getPreWarmCsd().isEmpty();
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
        Map<String, Set<String>> csdUrlsByParcel = getCsdUrlsByParcelNames(image, requiredParcels);
        LOGGER.debug("Available CSD URLs for the parcels: {} on image: {}", csdUrlsByParcel, image.getUuid());
        return allAvailableRequiredCsdHasValue(csdUrlsByParcel) && allAvailableRequiredCsdUrlLocationIsValid(csdUrlsByParcel);
    }

    private boolean allAvailableRequiredCsdHasValue(Map<String, Set<String>> csdList) {
        return csdList.entrySet().stream().noneMatch(csdListByParcel -> csdListByParcel.getValue().isEmpty());
    }

    private boolean allAvailableRequiredCsdUrlLocationIsValid(Map<String, Set<String>> csdList) {
        return csdList.entrySet().stream()
                .allMatch(csdListByParcel -> csdListByParcel.getValue().stream()
                        .allMatch(csdUrl -> URL_PATTERN.matcher(csdUrl).find()));
    }

    private Map<String, Set<String>> getCsdUrlsByParcelNames(Image image, Set<String> requiredParcels) {
        List<String> imagePreWarmCsd = image.getPreWarmCsd();
        LOGGER.debug("Available CSD urls: {}", imagePreWarmCsd);
        return requiredParcels.stream()
                .filter(parcel -> isCsdExistsOnImage(imagePreWarmCsd, parcel))
                .collect(Collectors.toMap(
                        parcelName -> parcelName,
                        parcelName -> filterCsdUrlsByStackRelatedParcel(imagePreWarmCsd, parcelName)));
    }

    private boolean isCsdExistsOnImage(List<String> imagePreWarmCsd, String parcelName) {
        return getCsdStreamWithMatchingParcelName(imagePreWarmCsd, parcelName).anyMatch(StringUtils::hasText);
    }

    private Set<String> filterCsdUrlsByStackRelatedParcel(List<String> imagePreWarmCsd, String parcelName) {
        return getCsdStreamWithMatchingParcelName(imagePreWarmCsd, parcelName)
                .collect(Collectors.toSet());
    }

    private Stream<String> getCsdStreamWithMatchingParcelName(List<String> imagePreWarmCsd, String parcelName) {
        return imagePreWarmCsd.stream()
                .filter(csdUrlContainsParcelName(parcelName));
    }

    private Predicate<String> csdUrlContainsParcelName(String parcelName) {
        return preWarmCsd -> preWarmCsd.toLowerCase().contains(parcelName.toLowerCase());
    }

}
