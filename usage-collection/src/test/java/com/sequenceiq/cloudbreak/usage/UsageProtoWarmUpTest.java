package com.sequenceiq.cloudbreak.usage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.protobuf.Descriptors.Descriptor;

class UsageProtoWarmUpTest {

    private final UsageProtoWarmUp underTest = new UsageProtoWarmUp();

    @Test
    void testWarmUpVisitsEventAndNestedMessageTypes() {
        Set<Descriptor> warmedUpTypes = underTest.warmUp(UsageProto.Event.getDefaultInstance(), new HashSet<>());

        assertTrue(warmedUpTypes.contains(UsageProto.Event.getDescriptor()));
        assertTrue(warmedUpTypes.contains(UsageProto.CDPCloudbreakFlowEvent.getDescriptor()));
        assertTrue(warmedUpTypes.contains(UsageProto.CDPEnvironmentRequested.getDescriptor()));
        assertTrue(warmedUpTypes.contains(UsageProto.CDPOperationDetails.getDescriptor()));
    }

    @Test
    void testWarmUpDoesNotThrow() {
        assertDoesNotThrow(() -> underTest.warmUp());
    }
}
