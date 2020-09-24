package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import java.util.Map;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractClusterUpgradeAction<P extends Payload>
        extends AbstractAction<FlowState, FlowEvent, ClusterUpgradeContext, P> {

    protected static final String CURRENT_IMAGE = "CURRENT_IMAGE";

    protected static final String TARGET_IMAGE = "TARGET_IMAGE";

    protected AbstractClusterUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    protected StatedImage getCurrentImage(Map<Object, Object> variables) {
        return (StatedImage) variables.get(CURRENT_IMAGE);
    }

    protected StatedImage getTargetImage(Map<Object, Object> variables) {
        return (StatedImage) variables.get(TARGET_IMAGE);
    }
}
