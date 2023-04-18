package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;

@Component
public class RuntimeDependencyBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeDependencyBasedUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 10;

    @Inject
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, imageFilterParams.getCurrentImage());
        LOGGER.debug("After the filtering {} image left.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return "There are no eligible images to upgrade because Python 3.8 dependency is missing from the current image";
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, Image currentImage) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(currentImage, image))
                .collect(Collectors.toList());
    }
}
