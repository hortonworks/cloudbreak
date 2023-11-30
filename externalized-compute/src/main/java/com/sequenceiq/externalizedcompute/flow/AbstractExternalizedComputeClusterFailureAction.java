package com.sequenceiq.externalizedcompute.flow;

import java.util.Optional;

import com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFailedEvent;

public abstract class AbstractExternalizedComputeClusterFailureAction<P extends ExternalizedComputeClusterFailedEvent>
        extends AbstractExternalizedComputeClusterAction<P> {

    protected AbstractExternalizedComputeClusterFailureAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(ExternalizedComputeClusterFailedEvent payload, Optional flowContext, Exception ex) {
        return ExternalizedComputeClusterCreateFailedEvent.from(payload, ex);
    }
}
