package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;

@Component
public class SeLinuxUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeLinuxUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 11;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private SeLinuxValidationService seLinuxValidationService;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        StackDto stack = stackDtoService.getById(imageFilterParams.getStackId());
        List<Image> filteredImages = filterImages(imageFilterResult, image -> {
            try {
                seLinuxValidationService.validateSeLinuxEntitlementGranted(stack);
                seLinuxValidationService.validateSeLinuxSupportedOnTargetImage(stack, image);
                return true;
            } catch (CloudbreakServiceException e) {
                LOGGER.info("Filtering out image '{}'. Reason: {}", image.getUuid(), e.getMessage());
                return false;
            }
        });
        logNotEligibleImages(imageFilterResult, filteredImages, LOGGER);
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        if (hasTargetImage(imageFilterParams)) {
            return getCantUpgradeToImageMessage(imageFilterParams,
                    String.format("SeLinux validation filtered out the target image '%s'.", imageFilterParams.getTargetImageId()));
        } else {
            return "SeLinux validation filtered out some of the potential images.";
        }
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }
}
