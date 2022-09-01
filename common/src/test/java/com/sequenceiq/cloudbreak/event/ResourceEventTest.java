package com.sequenceiq.cloudbreak.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ResourceEventTest {

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(ResourceEvent.class)
    void getMessageTest(ResourceEvent underTest) {
        assertThat(underTest.getMessage()).isNotBlank();
    }

}