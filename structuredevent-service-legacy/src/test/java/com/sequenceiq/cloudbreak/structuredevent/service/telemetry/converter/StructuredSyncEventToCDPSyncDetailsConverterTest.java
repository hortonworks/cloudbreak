package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

public class StructuredSyncEventToCDPSyncDetailsConverterTest {

    private StructuredSyncEventToCDPSyncDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredSyncEventToCDPSyncDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPSyncDetails details = underTest.convert(null);

        assertEquals(0, details.getClusterCreationStarted());
        assertEquals(0, details.getClusterCreationFinished());
    }

    @Test
    public void testConversionWithNullOperation() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPSyncDetails details = underTest.convert(structuredSyncEvent);

        assertEquals(0, details.getClusterCreationStarted());
        assertEquals(0, details.getClusterCreationFinished());
    }

    @Test
    public void testConversionWithEmptyClusterDetails() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        ClusterDetails clusterDetails = new ClusterDetails();
        structuredSyncEvent.setCluster(clusterDetails);

        UsageProto.CDPSyncDetails details = underTest.convert(structuredSyncEvent);

        assertEquals(0L, details.getClusterCreationStarted());
        assertEquals(0L, details.getClusterCreationFinished());
    }

    @Test
    public void testConversionWithEmptyStackDetails() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        StackDetails stackDetails = new StackDetails();
        structuredSyncEvent.setStack(stackDetails);

        UsageProto.CDPSyncDetails details = underTest.convert(structuredSyncEvent);

        assertEquals("UNKNOWN", details.getDatabaseType());
    }
}
