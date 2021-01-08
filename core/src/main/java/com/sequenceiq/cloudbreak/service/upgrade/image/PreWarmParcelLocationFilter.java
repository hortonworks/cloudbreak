package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class PreWarmParcelLocationFilter implements PackageLocationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreWarmParcelLocationFilter.class);

    private static final String URL_PREFIX = "http";

    @Override
    public boolean filterImage(Image image, Image currentImage, ImageFilterParams imageFilterParams) {
        if (StackType.WORKLOAD.equals(imageFilterParams.getStackType())) {
            List<String> parcelUrls = getUrls(image, imageFilterParams.getStackRelatedParcels());
            if (parcelUrls.isEmpty()) {
                LOGGER.debug("Image or some part of it is null or not contains the parcel urls: {}", image);
                return false;
            } else {
                LOGGER.debug("Matching URLs: [{}]", parcelUrls);
                return parcelUrls.stream().allMatch(parcelUrl -> URL_PATTERN.matcher(parcelUrl).find());
            }
        } else {
            LOGGER.debug("Skip filtering because the stack type is {}", imageFilterParams.getStackType());
            return true;
        }
    }

    private List<String> getUrls(Image image, Map<String, String> stackRelatedParcels) {
        return Optional.ofNullable(image)
                .map(Image::getPreWarmParcels).orElse(Collections.emptyList())
                .stream()
                .filter(filterByStackRelatedParcels(stackRelatedParcels))
                .flatMap(List::stream)
                .filter(parcel -> StringUtils.hasText(parcel) && parcel.startsWith(URL_PREFIX))
                .collect(Collectors.toList());
    }

    private Predicate<List<String>> filterByStackRelatedParcels(Map<String, String> stackRelatedParcels) {
        Set<String> stackRelatedParcelNames = getParcelNames(stackRelatedParcels);
        return parcelList -> CollectionUtils.isEmpty(stackRelatedParcels) || parcelList.stream()
                .anyMatch(parcel -> stackRelatedParcelNames.stream()
                        .anyMatch(relatedParcel -> StringUtils.hasText(parcel) && parcel.startsWith(relatedParcel)));
    }

    private Set<String> getParcelNames(Map<String, String> parcels) {
        return Optional.ofNullable(parcels)
                .map(Map::keySet)
                .orElse(Collections.emptySet());
    }
}
