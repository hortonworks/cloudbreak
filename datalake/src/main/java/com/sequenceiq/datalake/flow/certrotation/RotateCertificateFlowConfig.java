package com.sequenceiq.datalake.flow.certrotation;

import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateState.INIT_STATE;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateState.ROTATE_CERTIFICATE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateState.ROTATE_CERTIFICATE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateState.ROTATE_CERTIFICATE_STACK_STATE;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateStateSelectors.ROTATE_CERTIFICATE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateStateSelectors.ROTATE_CERTIFICATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateStateSelectors.ROTATE_CERTIFICATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateStateSelectors.ROTATE_CERTIFICATE_STACK_EVENT;
import static com.sequenceiq.datalake.flow.certrotation.RotateCertificateStateSelectors.ROTATE_CERTIFICATE_SUCCESS_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class RotateCertificateFlowConfig extends AbstractFlowConfiguration<RotateCertificateState, RotateCertificateStateSelectors>
        implements RetryableDatalakeFlowConfiguration<RotateCertificateStateSelectors> {

    private static final List<Transition<RotateCertificateState, RotateCertificateStateSelectors>> TRANSITIONS =
            new Transition.Builder<RotateCertificateState, RotateCertificateStateSelectors>()
            .defaultFailureEvent(ROTATE_CERTIFICATE_FAILED_EVENT)

            .from(INIT_STATE)
            .to(ROTATE_CERTIFICATE_STACK_STATE)
            .event(ROTATE_CERTIFICATE_STACK_EVENT)
            .defaultFailureEvent()

            .from(ROTATE_CERTIFICATE_STACK_STATE)
            .to(ROTATE_CERTIFICATE_FINISHED_STATE)
            .event(ROTATE_CERTIFICATE_SUCCESS_EVENT)
            .defaultFailureEvent()

            .from(ROTATE_CERTIFICATE_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(ROTATE_CERTIFICATE_FINALIZED_EVENT)
            .defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<RotateCertificateState, RotateCertificateStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(
                    INIT_STATE,
                    FINAL_STATE,
                    ROTATE_CERTIFICATE_FAILED_STATE,
                    ROTATE_CERTIFICATE_FAILED_HANDLED_EVENT);

    public RotateCertificateFlowConfig() {
        super(RotateCertificateState.class, RotateCertificateStateSelectors.class);
    }

    @Override
    public RotateCertificateStateSelectors[] getEvents() {
        return RotateCertificateStateSelectors.values();
    }

    @Override
    public RotateCertificateStateSelectors[] getInitEvents() {
        return new RotateCertificateStateSelectors[]{
                ROTATE_CERTIFICATE_STACK_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Rotate Certificate";
    }

    @Override
    protected List<Transition<RotateCertificateState, RotateCertificateStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RotateCertificateState, RotateCertificateStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RotateCertificateStateSelectors getRetryableEvent() {
        return ROTATE_CERTIFICATE_FAILED_HANDLED_EVENT;
    }
}
