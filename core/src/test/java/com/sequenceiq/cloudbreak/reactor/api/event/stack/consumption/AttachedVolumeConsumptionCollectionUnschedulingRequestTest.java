package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachedVolumeConsumptionCollectionUnschedulingRequestTest {

    private static final Long STACK_ID = 123L;

    @Test
    void constructorTest() {
        AttachedVolumeConsumptionCollectionUnschedulingRequest result = new AttachedVolumeConsumptionCollectionUnschedulingRequest(STACK_ID);

        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
    }

    @Test
    void selectorTest() {
        AttachedVolumeConsumptionCollectionUnschedulingRequest result = new AttachedVolumeConsumptionCollectionUnschedulingRequest(STACK_ID);

        assertThat(result.getSelector()).isEqualTo("ATTACHEDVOLUMECONSUMPTIONCOLLECTIONUNSCHEDULINGREQUEST");
    }

}