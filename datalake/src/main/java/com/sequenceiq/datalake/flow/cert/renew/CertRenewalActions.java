package com.sequenceiq.datalake.flow.cert.renew;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalFailedEvent;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalWaitEvent;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxStartCertRenewalEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.cert.CertRenewalService;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class CertRenewalActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertRenewalActions.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private CertRenewalService certRenewalService;

    @Bean(name = "START_CERT_RENEWAL_STATE")
    public Action<?, ?> startCertRenewAction() {
        return new AbstractSdxAction<>(SdxStartCertRenewalEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxStartCertRenewalEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxStartCertRenewalEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Start cert renewal.");
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                certRenewalService.renewCertificate(sdxCluster, payload.getUserId());
                SdxEvent sdxEvent = new SdxEvent(SdxCertRenewalEvent.CERT_RENEWAL_STARTED_EVENT.event(), payload.getResourceId(), payload.getUserId());
                sendEvent(context, sdxEvent);
            }

            @Override
            protected Object getFailurePayload(SdxStartCertRenewalEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCertRenewalFailedEvent(payload, ex.getMessage());
            }
        };
    }

    @Bean(name = "CERT_RENEWAL_IN_PROGRESS_STATE")
    public Action<?, ?> certRenewInProgressAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Cert renewal is in progress. Start waiting to finish.");
                sendEvent(context, new SdxCertRenewalWaitEvent(context));
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCertRenewalFailedEvent(payload, ex.getMessage());
            }
        };
    }

    @Bean(name = "CERT_RENEWAL_FINISHED_STATE")
    public Action<?, ?> certRenewFinishedAction() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Cert renewal is finished");
                certRenewalService.finalizeCertRenewal(payload.getResourceId());
                SdxEvent event = new SdxEvent(SdxCertRenewalEvent.CERT_RENEWAL_FINALIZED_EVENT.event(), context);
                sendEvent(context, event);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SdxCertRenewalFailedEvent(payload, ex.getMessage());
            }
        };
    }

    @Bean(name = "CERT_RENEWAL_FAILED_STATE")
    public Action<?, ?> handleFailureAction() {
        return new AbstractSdxAction<>(SdxCertRenewalFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxCertRenewalFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCertRenewalFailedEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.error("Cert renewal failed for datalake. Reason: {}", payload.getFailureReason());
                certRenewalService.handleFailure(payload.getResourceId(), payload.getFailureReason());
                sendEvent(context, SdxCertRenewalEvent.CERT_RENEWAL_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxCertRenewalFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
