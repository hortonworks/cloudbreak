package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;

abstract class AbstractClusterUpscaleAction<C extends ClusterUpscaleContext>
        extends AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, C, ClusterScalingContext> {

    AbstractClusterUpscaleAction() {
        super(ClusterScalingContext.class);
    }

    protected Object getFailurePayload(C flowContext, String reason) {
        return new UpscaleClusterFailedPayload(flowContext.getStack().getId(), reason);
    }

    @Override
    protected Object getFailurePayload(C flowContext, Exception ex) {
        return getFailurePayload(flowContext, ex.getMessage());
    }
}
