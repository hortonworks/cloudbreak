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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

@Component
public class CsdLocationFilter implements PackageLocationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsdLocationFilter.class);

    @Override
    public boolean filterImage(Image image, Image currentImage, ImageFilterParams imageFilterParams) {
        if (StackType.WORKLOAD.equals(imageFilterParams.getStackType())) {
            if (image == null || image.getPreWarmCsd() == null || image.getPreWarmCsd().isEmpty()) {
                LOGGER.debug("Image or CSD parcels are not present. Image: {}", image);
                return false;
            } else {
                List<String> csdList = getPreWarmCsdList(image, imageFilterParams);
                LOGGER.debug("Matching URLs: [{}]", csdList);
                return !csdList.isEmpty() && csdList.stream().allMatch(csdUrl -> URL_PATTERN.matcher(csdUrl).find());
            }
        } else {
            LOGGER.debug("Skip filtering because the stack type is {}", imageFilterParams.getStackType());
            return true;
        }
    }

    private List<String> getPreWarmCsdList(Image image, ImageFilterParams imageFilterParams) {
        Set<String> relatedParcelNames = getParcelNames(imageFilterParams);
        List<String> imagePreWarmCsd = image.getPreWarmCsd();
        return CollectionUtils.isEmpty(relatedParcelNames) ? imagePreWarmCsd
                : filterPreWarmCsdListByStackRelatedParcels(relatedParcelNames, imagePreWarmCsd);
    }

    private List<String> filterPreWarmCsdListByStackRelatedParcels(Set<String> stackRelatedParcelNames, List<String> imagePreWarmCsd) {
        return imagePreWarmCsd.stream()
                .filter(preWarmCsd -> stackRelatedParcelNames.stream().anyMatch(csdUrlContainsParcelName(preWarmCsd)))
                .collect(Collectors.toList());
    }

    private Set<String> getParcelNames(ImageFilterParams imageFilterParams) {
        return Optional.ofNullable(imageFilterParams)
                .map(ImageFilterParams::getStackRelatedParcels)
                .map(Map::keySet)
                .orElse(Collections.emptySet());
    }

    private Predicate<String> csdUrlContainsParcelName(String preWarmCsd) {
        return parcelName -> !"CDH".equals(parcelName) && preWarmCsd.toLowerCase().contains(parcelName.toLowerCase());
    }

}
