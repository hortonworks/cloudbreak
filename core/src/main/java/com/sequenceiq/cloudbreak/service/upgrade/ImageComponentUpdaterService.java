package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFoundException;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CM_REPO_DETAILS;

import java.util.EnumSet;
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

    public UpgradeImageInfo updateForUpgrade(String targetImageId, Long stackId) {
        Stack stack = stackService.getById(stackId);
        try {
            restRequestThreadLocalService.setWorkspace(stack.getWorkspace());

            UpgradeImageInfo upgradeImageInfo = upgradeImageInfoFactory.create(targetImageId, stackId);
            Set<Component> targetComponents = imageService.getComponents(
                    stack, upgradeImageInfo.getCurrentImage().getUserdata(), upgradeImageInfo.getTargetStatedImage(),
                    EnumSet.of(CDH_PRODUCT_DETAILS, CM_REPO_DETAILS)
            );
            stackComponentUpdater.updateComponentsByStackId(stack, targetComponents, true);
            clusterComponentUpdater.updateClusterComponentsByStackId(stack, targetComponents, true);
            return upgradeImageInfo;
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn(String.format("Image was not found for stack %s", stack.getName()), e);
            throw notFoundException("Image", targetImageId);
        }
    }
}
