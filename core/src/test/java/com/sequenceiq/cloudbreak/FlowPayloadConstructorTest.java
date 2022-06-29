package com.sequenceiq.cloudbreak;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.helper.FlowPayloadConstructorChecker;

class FlowPayloadConstructorTest {

    @Test
    void constructorTest() {
        new FlowPayloadConstructorChecker().checkConstructorOnAcceptableClasses();
    }

}
