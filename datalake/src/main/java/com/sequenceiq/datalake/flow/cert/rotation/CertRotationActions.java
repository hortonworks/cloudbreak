package com.sequenceiq.datalake.flow.cert.rotation;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationFailedEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationWaitEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxStartCertRotationEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.cert.CertRotationService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class CertRotationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertRotationActions.class);

    @Inject
    private CertRotationService certRotationService;

    @Bean(name = "START_CERT_ROTATION_STATE")
    public Action<?, ?> starCertRotationAction() {
        return new AbstractSdxAction<>(SdxStartCertRotationEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxStartCertRotationEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartCertRotationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Start cert rotation with request: {}", payload.getRequest());
                certRotationService.startCertRotation(context.getSdxId(), payload.getRequest());
                SdxEvent sdxEvent = new SdxEvent(SdxCertRotationEvent.CERT_ROTATION_STARTED_EVENT.event(), payload.getResourceId(), payload.getUserId());
                sendEvent(context, sdxEvent);
            }

            @Override
            protected Object getFailurePayload(SdxStartCertRotationEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCertRotationFailedEvent(payload, ex);
            }
        };
    }

    @Bean(name = "CERT_ROTATION_IN_PROGRESS_STATE")
    public Action<?, ?> certRotationInProgressAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Cert rotation is in progress. Start waiting to finish");
                SdxCertRotationWaitEvent event = new SdxCertRotationWaitEvent(context);
                sendEvent(context, event);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCertRotationFailedEvent(payload, ex);
            }
        };
    }

    @Bean(name = "CERT_ROTATION_FINISHED_STATE")
    public Action<?, ?> certRotationFinishedAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Cert rotation is finished");
                certRotationService.finalizeCertRotation(payload.getResourceId());
                SdxEvent event = new SdxEvent(SdxCertRotationEvent.CERT_ROTATION_FINALIZED_EVENT.event(), context);
                sendEvent(context, event);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCertRotationFailedEvent(payload, ex);
            }
        };
    }

    @Bean(name = "CERT_ROTATION_FAILED_STATE")
    public Action<?, ?> handleFailureAction() {
        return new AbstractSdxAction<>(SdxCertRotationFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxCertRotationFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCertRotationFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Cert rotation failed for datalake", payload.getException());
                certRotationService.handleFailure(payload.getResourceId(), payload.getException());
                sendEvent(context, SdxCertRotationEvent.CERT_ROTATION_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxCertRotationFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
