package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

class StructuredEventToCDPDatalakeFeaturesConverterTest {

    private StructuredEventToCDPDatalakeFeaturesConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToCDPDatalakeFeaturesConverter();
    }

    @Test
    public void testNullCluster() {
        UsageProto.CDPDatalakeFeatures features = underTest.convert((StructuredFlowEvent) null);

        assertEquals("", features.getRaz().getStatus());

        features = underTest.convert((StructuredSyncEvent) null);

        assertEquals("", features.getRaz().getStatus());
    }

    @Test
    public void testRazEnabled() {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setRazEnabled(true);
        StructuredFlowEvent flowEvent = new StructuredFlowEvent();
        flowEvent.setCluster(clusterDetails);

        UsageProto.CDPDatalakeFeatures features = underTest.convert(flowEvent);

        assertEquals("ENABLED", features.getRaz().getStatus());

        StructuredSyncEvent syncEvent = new StructuredSyncEvent();
        syncEvent.setCluster(clusterDetails);

        features = underTest.convert(syncEvent);

        assertEquals("ENABLED", features.getRaz().getStatus());
    }

    @Test
    public void testRazDisabled() {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setRazEnabled(false);
        StructuredFlowEvent flowEvent = new StructuredFlowEvent();
        flowEvent.setCluster(clusterDetails);

        UsageProto.CDPDatalakeFeatures features = underTest.convert(flowEvent);

        assertEquals("DISABLED", features.getRaz().getStatus());

        StructuredSyncEvent syncEvent = new StructuredSyncEvent();
        syncEvent.setCluster(clusterDetails);

        features = underTest.convert(syncEvent);

        assertEquals("DISABLED", features.getRaz().getStatus());
    }
}