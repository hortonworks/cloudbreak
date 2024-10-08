package com.sequenceiq.environment.environment.flow.externalizedcluster.create;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

abstract class AbstractExternalizedComputeCreationAction<P extends BaseNamedFlowEvent>
    extends AbstractAction<ExternalizedComputeClusterCreationState, ExternalizedComputeClusterCreationStateSelectors, CommonContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExternalizedComputeCreationAction.class);

    AbstractExternalizedComputeCreationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters,
            StateContext<ExternalizedComputeClusterCreationState, ExternalizedComputeClusterCreationStateSelectors> stateContext, P payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected void prepareExecution(P payload, Map<Object, Object> variables) {
        if (payload != null) {
            MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
            MdcContext.builder().resourceName(payload.getResourceName()).buildMdc();
        } else {
            LOGGER.warn("Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
        }
    }

}
