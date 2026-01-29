package com.sequenceiq.datalake.flow.cert.renew;

import static com.sequenceiq.datalake.flow.cert.renew.SdxCertRenewalState.CERT_RENEWAL_FAILED_STATE;
import static com.sequenceiq.datalake.flow.cert.renew.SdxCertRenewalState.CERT_RENEWAL_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.cert.renew.SdxCertRenewalState.CERT_RENEWAL_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.cert.renew.SdxCertRenewalState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.cert.renew.SdxCertRenewalState.INIT_STATE;
import static com.sequenceiq.datalake.flow.cert.renew.SdxCertRenewalState.START_CERT_RENEWAL_STATE;
import static com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent.CERT_RENEWAL_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent.CERT_RENEWAL_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent.CERT_RENEWAL_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent.CERT_RENEWAL_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent.CERT_RENEWAL_STARTED_EVENT;
import static com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent.RENEW_CERT_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxCertRenewalEvent;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class CertRenewalFlowConfig extends AbstractFlowConfiguration<SdxCertRenewalState, SdxCertRenewalEvent>
        implements RetryableDatalakeFlowConfiguration<SdxCertRenewalEvent> {

    private static final List<Transition<SdxCertRenewalState, SdxCertRenewalEvent>> TRANSITIONS =
            new Transition.Builder<SdxCertRenewalState, SdxCertRenewalEvent>()
                    .defaultFailureEvent(CERT_RENEWAL_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(START_CERT_RENEWAL_STATE)
                    .event(RENEW_CERT_EVENT)
                    .defaultFailureEvent()

                    .from(START_CERT_RENEWAL_STATE)
                    .to(CERT_RENEWAL_IN_PROGRESS_STATE)
                    .event(CERT_RENEWAL_STARTED_EVENT)
                    .defaultFailureEvent()

                    .from(CERT_RENEWAL_IN_PROGRESS_STATE)
                    .to(CERT_RENEWAL_FINISHED_STATE)
                    .event(CERT_RENEWAL_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(CERT_RENEWAL_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(CERT_RENEWAL_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected CertRenewalFlowConfig() {
        super(SdxCertRenewalState.class, SdxCertRenewalEvent.class);
    }

    @Override
    protected List<Transition<SdxCertRenewalState, SdxCertRenewalEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxCertRenewalState, SdxCertRenewalEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CERT_RENEWAL_FAILED_STATE, CERT_RENEWAL_FAILURE_HANDLED_EVENT);
    }

    @Override
    public SdxCertRenewalEvent[] getEvents() {
        return SdxCertRenewalEvent.values();
    }

    @Override
    public SdxCertRenewalEvent[] getInitEvents() {
        return new SdxCertRenewalEvent[]{RENEW_CERT_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Renew cluster certificate";
    }

    @Override
    public SdxCertRenewalEvent getRetryableEvent() {
        return CERT_RENEWAL_FAILED_EVENT;
    }

    @Override
    public List<SdxCertRenewalEvent> getStackRetryEvents() {
        return List.of(CERT_RENEWAL_STARTED_EVENT);
    }
}
