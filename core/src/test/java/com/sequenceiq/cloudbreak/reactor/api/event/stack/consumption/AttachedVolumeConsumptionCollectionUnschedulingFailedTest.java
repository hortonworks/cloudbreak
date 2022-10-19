package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachedVolumeConsumptionCollectionUnschedulingFailedTest {

    private static final Long STACK_ID = 123L;

    @Test
    void constructorTest() {
        Exception exception = new Exception();

        AttachedVolumeConsumptionCollectionUnschedulingFailed result = new AttachedVolumeConsumptionCollectionUnschedulingFailed(STACK_ID, exception);

        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
        assertThat(result.getException()).isSameAs(exception);
    }

    @Test
    void selectorTest() {
        AttachedVolumeConsumptionCollectionUnschedulingFailed result = new AttachedVolumeConsumptionCollectionUnschedulingFailed(STACK_ID, new Exception());

        assertThat(result.getSelector()).isEqualTo("ATTACHEDVOLUMECONSUMPTIONCOLLECTIONUNSCHEDULINGFAILED");
    }

}