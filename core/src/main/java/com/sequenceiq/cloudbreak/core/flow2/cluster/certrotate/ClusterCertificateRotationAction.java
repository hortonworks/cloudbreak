package com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificateRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class ClusterCertificateRotationAction<P extends Payload> extends
        AbstractAction<ClusterCertificatesRotationState, FlowEvent, ClusterCertificateRotationContext, P> {

    public static final String CERT_ROTATION_TYPE = "CERT_ROTATION_TYPE";

    @Inject
    private StackDtoService stackDtoService;

    protected ClusterCertificateRotationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterCertificateRotationContext createFlowContext(FlowParameters flowParameters,
            StateContext<ClusterCertificatesRotationState, FlowEvent> stateContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        CertificateRotationType certificateRotationType =
                (CertificateRotationType) variables.getOrDefault(CERT_ROTATION_TYPE, CertificateRotationType.HOST_CERTS);
        return new ClusterCertificateRotationContext(flowParameters, certificateRotationType);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterCertificateRotationContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
