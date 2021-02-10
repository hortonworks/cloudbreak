package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.SyncDetails;

public class StructuredSyncEventToCDPSyncDetailsConverterTest {

    private StructuredSyncEventToCDPSyncDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredSyncEventToCDPSyncDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNull("We should return with null if the input is null", underTest.convert(null));
    }

    @Test
    public void testConversionWithNullOperation() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPSyncDetails details = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", details.getStatus());
        Assert.assertEquals("", details.getDetailedStatus());
        Assert.assertEquals(0, details.getClusterCreationStarted());
        Assert.assertEquals(0, details.getClusterCreationFinished());
    }

    @Test
    public void testConversionWithNullableSyncDetailsFields() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        SyncDetails syncDetails = new SyncDetails();
        syncDetails.setStatus("status");
        structuredSyncEvent.setsyncDetails(syncDetails);

        UsageProto.CDPSyncDetails details = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("status", details.getStatus());
        Assert.assertEquals("", details.getDetailedStatus());
        Assert.assertEquals(0L, details.getClusterCreationStarted());
        Assert.assertEquals(0L, details.getClusterCreationFinished());
    }

}
