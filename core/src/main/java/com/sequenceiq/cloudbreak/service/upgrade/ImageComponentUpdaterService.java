package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFoundException;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@Service
public class ImageComponentUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageComponentUpdaterService.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackComponentUpdater stackComponentUpdater;

    @Inject
    private ClusterComponentUpdater clusterComponentUpdater;

    @Inject
    private UpgradeImageInfoFactory upgradeImageInfoFactory;

    @Inject
    private ImageService imageService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    public UpgradeImageInfo updateComponentsForUpgrade(String targetImageId, Long stackId) {
        Stack stack = stackService.getById(stackId);
        restRequestThreadLocalService.setWorkspaceId(stack.getWorkspaceId());
        UpgradeImageInfo upgradeImageInfo = getUpgradeImageInfo(targetImageId, stackId, stack);
        updateComponents(upgradeImageInfo.targetStatedImage(), stack, targetImageId);

        return upgradeImageInfo;
    }

    public void updateComponentsForUpgrade(StatedImage targetStatedImage, Long stackId) {
        Stack stack = stackService.getById(stackId);
        restRequestThreadLocalService.setWorkspaceId(stack.getWorkspaceId());
        updateComponents(targetStatedImage, stack, targetStatedImage.getImage().getUuid());
    }

    private void updateComponents(StatedImage upgradeImageInfo, Stack stack, String targetImageId) {
        try {
            Set<Component> targetComponents = imageService.getComponentsWithoutUserData(stack, upgradeImageInfo);
            stackComponentUpdater.updateComponentsByStackId(stack, targetComponents, true);
            clusterComponentUpdater.updateClusterComponentsByStackId(stack, targetComponents, true);
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn(String.format("Image was not found for stack %s", stack.getName()), e);
            throw notFoundException("Image", targetImageId);
        }
    }

    private UpgradeImageInfo getUpgradeImageInfo(String targetImageId, Long stackId, Stack stack) {
        try {
            return upgradeImageInfoFactory.create(targetImageId, stackId);
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn(String.format("Image was not found for stack %s", stack.getName()), e);
            throw notFoundException("Image", targetImageId);
        }
    }

}
