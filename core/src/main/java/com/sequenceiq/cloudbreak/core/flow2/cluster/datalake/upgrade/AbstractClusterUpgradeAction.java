package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import java.util.Map;

import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.CurrentImageRetrieverService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractClusterUpgradeAction<P extends Payload>
        extends AbstractAction<FlowState, FlowEvent, ClusterUpgradeContext, P> {

    protected static final String CURRENT_IMAGE = "CURRENT_IMAGE";

    protected static final String CURRENT_MODEL_IMAGE = "CURRENT_MODEL_IMAGE";

    protected static final String TARGET_IMAGE = "TARGET_IMAGE";

    protected static final String ROLLING_UPGRADE_ENABLED = "ROLLING_UPGRADE_ENABLED";

    @Inject
    private CurrentImageRetrieverService currentImageRetrieverService;

    @Inject
    private StackService stackService;

    protected AbstractClusterUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    protected StatedImage getCurrentImage(Map<Object, Object> variables) {
        return (StatedImage) variables.get(CURRENT_IMAGE);
    }

    protected Image getCurrentModelImage(Map<Object, Object> variables) {
        return (Image) variables.get(CURRENT_MODEL_IMAGE);
    }

    protected StatedImage getTargetImage(Map<Object, Object> variables) {
        return (StatedImage) variables.get(TARGET_IMAGE);
    }

    protected StackService getStackService() {
        return stackService;
    }

    protected Image retrieveCurrentImageFromVariables(Map<Object, Object> variables, Long stackId) {
        return (Image) variables.computeIfAbsent(CURRENT_MODEL_IMAGE, key -> {
            try {
                Stack stack = stackService.getById(stackId);
                return currentImageRetrieverService.retrieveCurrentModelImage(stack);
            } catch (CloudbreakImageNotFoundException e) {
                throw new CloudbreakServiceException(e);
            }
        });
    }
}
