package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

class RotateSaltPasswordReasonTest {

    @ParameterizedTest(name = "{0} has a corresponding usage event reason")
    @EnumSource(RotateSaltPasswordReason.class)
    void checkUsageValues(RotateSaltPasswordReason reason) {
        UsageProto.CDPSaltPasswordRotationEventReason.Value.valueOf(reason.name());
    }

}