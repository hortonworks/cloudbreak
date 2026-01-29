package com.sequenceiq.datalake.flow.cert.rotation;

import static com.sequenceiq.datalake.flow.cert.rotation.SdxCertRotationState.CERT_ROTATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.cert.rotation.SdxCertRotationState.CERT_ROTATION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.cert.rotation.SdxCertRotationState.CERT_ROTATION_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.cert.rotation.SdxCertRotationState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.cert.rotation.SdxCertRotationState.INIT_STATE;
import static com.sequenceiq.datalake.flow.cert.rotation.SdxCertRotationState.START_CERT_ROTATION_STATE;
import static com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent.CERT_ROTATION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent.CERT_ROTATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent.CERT_ROTATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent.CERT_ROTATION_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent.CERT_ROTATION_STARTED_EVENT;
import static com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent.ROTATE_CERT_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class CertRotationFlowConfig extends AbstractFlowConfiguration<SdxCertRotationState, SdxCertRotationEvent>
        implements RetryableDatalakeFlowConfiguration<SdxCertRotationEvent> {

    private static final List<Transition<SdxCertRotationState, SdxCertRotationEvent>> TRANSITIONS = new Builder<SdxCertRotationState, SdxCertRotationEvent>()
            .defaultFailureEvent(CERT_ROTATION_FAILED_EVENT)

            .from(INIT_STATE)
            .to(START_CERT_ROTATION_STATE)
            .event(ROTATE_CERT_EVENT)
            .defaultFailureEvent()

            .from(START_CERT_ROTATION_STATE)
            .to(CERT_ROTATION_IN_PROGRESS_STATE)
            .event(CERT_ROTATION_STARTED_EVENT)
            .defaultFailureEvent()

            .from(CERT_ROTATION_IN_PROGRESS_STATE)
            .to(CERT_ROTATION_FINISHED_STATE)
            .event(CERT_ROTATION_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(CERT_ROTATION_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(CERT_ROTATION_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    protected CertRotationFlowConfig() {
        super(SdxCertRotationState.class, SdxCertRotationEvent.class);
    }

    @Override
    protected List<Transition<SdxCertRotationState, SdxCertRotationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<SdxCertRotationState, SdxCertRotationEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CERT_ROTATION_FAILED_STATE, CERT_ROTATION_FAILURE_HANDLED_EVENT);
    }

    @Override
    public SdxCertRotationEvent[] getEvents() {
        return SdxCertRotationEvent.values();
    }

    @Override
    public SdxCertRotationEvent[] getInitEvents() {
        return new SdxCertRotationEvent[]{ROTATE_CERT_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Rotate cluster certificates";
    }

    @Override
    public SdxCertRotationEvent getRetryableEvent() {
        return CERT_ROTATION_FAILED_EVENT;
    }

    @Override
    public List<SdxCertRotationEvent> getStackRetryEvents() {
        return List.of(CERT_ROTATION_STARTED_EVENT);
    }
}
