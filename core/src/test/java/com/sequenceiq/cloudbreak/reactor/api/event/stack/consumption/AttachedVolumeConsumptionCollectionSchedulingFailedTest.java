package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachedVolumeConsumptionCollectionSchedulingFailedTest {

    private static final Long STACK_ID = 123L;

    @Test
    void constructorTest() {
        Exception exception = new Exception();

        AttachedVolumeConsumptionCollectionSchedulingFailed result = new AttachedVolumeConsumptionCollectionSchedulingFailed(STACK_ID, exception);

        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
        assertThat(result.getException()).isSameAs(exception);
    }

    @Test
    void selectorTest() {
        AttachedVolumeConsumptionCollectionSchedulingFailed result = new AttachedVolumeConsumptionCollectionSchedulingFailed(STACK_ID, new Exception());

        assertThat(result.getSelector()).isEqualTo("ATTACHEDVOLUMECONSUMPTIONCOLLECTIONSCHEDULINGFAILED");
    }

}