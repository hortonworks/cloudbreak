package com.sequenceiq.datalake.flow.create;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.RDS_TRIGGERED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.RDS_WAIT_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.STORAGE_VALIDATION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_STORAGE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_TRIGGER_RDS_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_WAIT_ENV_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_WAIT_RDS_STATE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class SdxCreateFlowConfigTest {

    private SdxCreateFlowConfig underTest;

    private List<Transition<SdxCreateState, SdxCreateEvent>> transitions;

    @BeforeEach
    void setUp() {
        underTest = new SdxCreateFlowConfig();
        transitions = underTest.getTransitions().stream().toList();
    }

    @Test
    void storageValidationLeadsToTriggerRds() {
        Transition<SdxCreateState, SdxCreateEvent> transition = transitionFrom(SDX_CREATION_STORAGE_VALIDATION_STATE);

        assertThat(transition.getTarget()).isEqualTo(SDX_CREATION_TRIGGER_RDS_STATE);
        assertThat(transition.getEvent()).isEqualTo(STORAGE_VALIDATION_SUCCESS_EVENT);
    }

    @Test
    void triggerRdsLeadsToWaitEnv() {
        Transition<SdxCreateState, SdxCreateEvent> transition = transitionFrom(SDX_CREATION_TRIGGER_RDS_STATE);

        assertThat(transition.getTarget()).isEqualTo(SDX_CREATION_WAIT_ENV_STATE);
        assertThat(transition.getEvent()).isEqualTo(RDS_TRIGGERED_EVENT);
    }

    @Test
    void legacyWaitRdsEdgeRetainedForRollingDeploy() {
        Transition<SdxCreateState, SdxCreateEvent> transition = transitionFrom(SDX_CREATION_WAIT_RDS_STATE);

        assertThat(transition.getTarget()).isEqualTo(SDX_CREATION_WAIT_ENV_STATE);
        assertThat(transition.getEvent()).isEqualTo(RDS_WAIT_SUCCESS_EVENT);
    }

    @Test
    void getEventsMatchesEnum() {
        assertThat(underTest.getEvents()).hasSize(SdxCreateEvent.values().length);
    }

    private Transition<SdxCreateState, SdxCreateEvent> transitionFrom(SdxCreateState source) {
        List<Transition<SdxCreateState, SdxCreateEvent>> matching = transitions.stream()
                .filter(t -> t.getSource() == source)
                .toList();
        assertThat(matching).hasSize(1);
        return matching.getFirst();
    }
}
