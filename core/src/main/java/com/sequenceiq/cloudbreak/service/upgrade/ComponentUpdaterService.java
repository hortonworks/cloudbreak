package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFoundException;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ComponentUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentUpdaterService.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackService stackService;

    @Inject
    private StackComponentUpdater stackComponentUpdater;

    @Inject
    private ClusterComponentUpdater clusterComponentUpdater;

    public Pair<StatedImage, StatedImage> updateComponents(String imageId, Long stackId) {
        Stack stack = stackService.getById(stackId);
        try {
            Image currentImage = componentConfigProviderService.getImage(stackId);
            StatedImage currentStatedImage =
                    imageCatalogService.getImage(currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), currentImage.getImageId());
            StatedImage targetStatedImage = imageCatalogService.getImage(currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), imageId);
            Set<Component> targetComponents = stackComponentUpdater.updateComponentsByStackId(stack, targetStatedImage, currentImage.getUserdata());
            clusterComponentUpdater.updateClusterComponentsByStackId(stack, targetComponents);
            return Pair.of(currentStatedImage, targetStatedImage);
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn(String.format("Image was not found for stack %s", stack.getName()), e);
            throw notFoundException("Image", imageId);
        }
    }
}
