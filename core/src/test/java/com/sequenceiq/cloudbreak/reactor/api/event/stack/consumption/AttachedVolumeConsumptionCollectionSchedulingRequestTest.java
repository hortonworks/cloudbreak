package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachedVolumeConsumptionCollectionSchedulingRequestTest {

    private static final Long STACK_ID = 123L;

    @Test
    void constructorTest() {
        AttachedVolumeConsumptionCollectionSchedulingRequest result = new AttachedVolumeConsumptionCollectionSchedulingRequest(STACK_ID);

        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void selectorTest() {
        AttachedVolumeConsumptionCollectionSchedulingRequest result = new AttachedVolumeConsumptionCollectionSchedulingRequest(STACK_ID);

        assertThat(result.getSelector()).isEqualTo("ATTACHEDVOLUMECONSUMPTIONCOLLECTIONSCHEDULINGREQUEST");
    }

}