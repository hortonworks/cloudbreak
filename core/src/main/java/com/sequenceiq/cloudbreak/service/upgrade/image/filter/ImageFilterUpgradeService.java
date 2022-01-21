package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;

import javax.inject.Named;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Service
public class ImageFilterUpgradeService {

    private final List<UpgradeImageFilter> orderedUpgradeImageFilters;

    public ImageFilterUpgradeService(@Named("orderedUpgradeImageFilters") List<UpgradeImageFilter> orderedUpgradeImageFilters) {
        this.orderedUpgradeImageFilters = orderedUpgradeImageFilters;
    }

    public ImageFilterResult filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        for (UpgradeImageFilter imageFilter : orderedUpgradeImageFilters) {
            if (imageFilterResult.getImages().isEmpty()) {
                return imageFilterResult;
            } else {
                imageFilterResult = imageFilter.filter(imageFilterResult, imageFilterParams);
            }
        }
        return imageFilterResult;
    }
}
