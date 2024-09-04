package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateBaseEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractRotateRdsCertificateAction<P extends RotateRdsCertificateBaseEvent>
        extends AbstractStackAction<RotateRdsCertificateState, RotateRdsCertificateEvent, RotateRdsCertificateContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractRotateRdsCertificateAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected RotateRdsCertificateContext createFlowContext(FlowParameters flowParameters, StateContext<RotateRdsCertificateState,
            RotateRdsCertificateEvent> stateContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.buildMdcContext(cluster);
        return new RotateRdsCertificateContext(flowParameters, payload.getResourceId(), stack, payload.getRotateRdsCertificateType());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RotateRdsCertificateContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
