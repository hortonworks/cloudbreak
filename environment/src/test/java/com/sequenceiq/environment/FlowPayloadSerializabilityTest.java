package com.sequenceiq.environment;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.helper.FlowPayloadSerializabilityChecker;

class FlowPayloadSerializabilityTest {

    @Test
    void serializabilityTest() {
        new FlowPayloadSerializabilityChecker().checkAcceptableClasses();
    }

}
