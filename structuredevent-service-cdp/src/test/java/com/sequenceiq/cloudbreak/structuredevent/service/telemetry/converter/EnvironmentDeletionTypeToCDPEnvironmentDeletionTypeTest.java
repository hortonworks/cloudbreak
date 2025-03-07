package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

class EnvironmentDeletionTypeToCDPEnvironmentDeletionTypeTest {

    @Test
    void testConvert() {
        assertEquals(UsageProto.CDPEnvironmentDeletionType.Value.UNSET, EnvironmentDeletionTypeToCDPEnvironmentDeletionType.convert(null));
        assertEquals(UsageProto.CDPEnvironmentDeletionType.Value.NONE, EnvironmentDeletionTypeToCDPEnvironmentDeletionType.convert("NONE"));
        assertEquals(UsageProto.CDPEnvironmentDeletionType.Value.SIMPLE, EnvironmentDeletionTypeToCDPEnvironmentDeletionType.convert("SIMPLE"));
        assertEquals(UsageProto.CDPEnvironmentDeletionType.Value.FORCE, EnvironmentDeletionTypeToCDPEnvironmentDeletionType.convert("FORCE"));
        assertEquals(UsageProto.CDPEnvironmentDeletionType.Value.UNSET, EnvironmentDeletionTypeToCDPEnvironmentDeletionType.convert("INVALID"));
    }
}
