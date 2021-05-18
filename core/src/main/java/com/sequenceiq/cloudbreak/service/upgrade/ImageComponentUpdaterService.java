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
import com.sequenceiq.cloudbreak.service.stack.StackService;

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

    public UpgradeImageInfo updateForUpgrade(String imageId, Long stackId) {
        Stack stack = stackService.getById(stackId);
        try {
            UpgradeImageInfo upgradeImageInfo = upgradeImageInfoFactory.create(imageId, stackId);
            Set<Component> targetComponents = stackComponentUpdater.updateComponentsByStackId(
                    stack, upgradeImageInfo.getTargetStatedImage(), upgradeImageInfo.getCurrentImage().getUserdata());
            clusterComponentUpdater.updateClusterComponentsByStackId(stack, targetComponents);
            return upgradeImageInfo;
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn(String.format("Image was not found for stack %s", stack.getName()), e);
            throw notFoundException("Image", imageId);
        }
    }
}
