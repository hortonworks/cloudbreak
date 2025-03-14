package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.ImageCatalogService.CDP_DEFAULT_CATALOG_NAME;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackImageFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackImageFilterService.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackImageUpdateService stackImageUpdateService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    public Images getApplicableImages(Long workspaceId, String stackName, boolean defaultOnly) throws CloudbreakImageCatalogException {
        return getApplicableImages(workspaceId, CDP_DEFAULT_CATALOG_NAME, stackName, defaultOnly);
    }

    public Images getApplicableImages(Long workspaceId, String imageCatalogName, String stackName, boolean defaultOnly) throws CloudbreakImageCatalogException {
        Stack stack = stackService.getByNameInWorkspaceWithLists(stackName, workspaceId)
                .orElseThrow(NotFoundException.notFound("stack", stackName));
        StatedImages statedImages = imageCatalogService.getImages(
                workspaceId,
                imageCatalogName,
                null,
                platformStringTransformer.getPlatformStringForImageCatalog(stack.cloudPlatform(), stack.getPlatformVariant()),
                defaultOnly, null);
        return getApplicableImages(imageCatalogName, statedImages, stack);
    }

    private Images getApplicableImages(String imageCatalogName, StatedImages statedImages, Stack stack) {
        if (Objects.nonNull(statedImages)) {
            LOGGER.info("Selecting applicable images from catalog {} from {} candidates.", imageCatalogName, statedImages.getImages().getNumberOfImages());
        }
        if (!stack.isAvailable() && !stack.isMaintenanceModeEnabled()) {
            throw new BadRequestException("To retrieve list of images for upgrade cluster have to be in AVAILABLE state");
        }

        String currentImageUuid = getCurrentImageUuid(stack);
        List<Image> filteredBaseImages =
                filterByApplicability(imageCatalogName, statedImages.getImageCatalogUrl(), stack, statedImages.getImages().getBaseImages(), currentImageUuid);
        LOGGER.info("Filtered base images: {}", filteredBaseImages);

        List<Image> filteredCdhImages =
                filterByApplicability(imageCatalogName, statedImages.getImageCatalogUrl(), stack, statedImages.getImages().getCdhImages(), currentImageUuid);
        LOGGER.info("Filtered CDH images: {}", filteredCdhImages);

        return new Images(filteredBaseImages, filteredCdhImages, statedImages.getImages().getFreeIpaImages(), statedImages.getImages().getSupportedVersions());

    }

    private String getCurrentImageUuid(Stack stack) {
        try {
            return componentConfigProviderService.getImage(stack.getId()).getImageId();
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Could not find last used image when preparing upgrade image list for current cluster", e);
            return "";
        }
    }

    private List<Image> filterByApplicability(String imageCatalogName, String imageCatalogUrl, Stack stack, List<Image> imagesList, String currentUuid) {
        return imagesList.stream()
                .filter(image -> !image.getUuid().equals(currentUuid))
                .filter(image -> stackImageUpdateService.isValidImage(stack, image.getUuid(), imageCatalogName, imageCatalogUrl))
                .collect(Collectors.toList());
    }
}
