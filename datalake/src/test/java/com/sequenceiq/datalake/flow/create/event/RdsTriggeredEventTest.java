package com.sequenceiq.datalake.flow.create.event;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.RDS_TRIGGERED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RdsTriggeredEventTest {

    @Test
    void selectorMatchesTheEventTheFlowRegistersForTheTransition() {
        RdsTriggeredEvent underTest = new RdsTriggeredEvent(1L, "userId");

        assertThat(underTest.selector()).isEqualTo(RDS_TRIGGERED_EVENT.event());
    }
}
