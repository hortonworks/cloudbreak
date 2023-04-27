package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.PythonVersionValidator;

@Component
public class RuntimeDependencyBasedUpgradeImageFilter implements UpgradeImageFilter {

    public static final String WARNING_MESSAGE = "This cluster cannot be directly upgraded to the latest Cloudera Runtime 7.2.16 Service Pack. "
            + "You will need to perform the upgrade in multiple steps to bring this cluster to the latest version. "
            + "First please perform an upgrade to the latest Service Pack of the current runtime. "
            + "(Select the last item in “Target Cloudera Runtime Version” and perform the upgrade.) "
            + "After the Runtime upgrade has been completed, please also perform an OS upgrade. "
            + "(Return to this screen, select the last item in “Target Cloudera Runtime Version” and perform the upgrade.) "
            + "Once you have completed the steps above, you will be able to upgrade to the latest Cloudera Runtime 7.2.16 Service Pack";

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeDependencyBasedUpgradeImageFilter.class);

    private static final int ORDER_NUMBER = 10;

    @Inject
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, imageFilterParams);
        LOGGER.debug("After the filtering {} image left.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReasonMessage(imageFilterResult, imageFilterParams, filteredImages));
    }

    @Override
    public String getMessage(ImageFilterParams imageFilterParams) {
        return PythonVersionValidator.ERROR_MESSAGE;
    }

    @Override
    public Integer getFilterOrderNumber() {
        return ORDER_NUMBER;
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        StackDto stack = stackDtoService.getById(imageFilterParams.getStackId());
        List<Image> allCdhImages = getAllCdhImagesFromCatalog(imageFilterParams, stack);
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, allCdhImages,
                        imageFilterParams.getCurrentImage(), image))
                .collect(Collectors.toList());
    }

    private List<Image> getAllCdhImagesFromCatalog(ImageFilterParams imageFilterParams, StackDto stack) {
        try {
            return imageCatalogService.getAllCdhImages(ThreadBasedUserCrnProvider.getUserCrn(), stack.getWorkspaceId(),
                    imageFilterParams.getImageCatalogName(), Set.of(imageFilterParams.getImageCatalogPlatform()));
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.error("Failed to retrieve images from catalog {}", imageFilterParams.getImageCatalogName(), e);
            throw new CloudbreakServiceException(e);
        }
    }

    private String getReasonMessage(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams, List<Image> filteredImages) {
        return !filteredImages.isEmpty() && imageFilterResult.getImages().size() != filteredImages.size() ?
                WARNING_MESSAGE :
                getReason(filteredImages, imageFilterParams);
    }
}
