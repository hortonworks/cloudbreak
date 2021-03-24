package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;

class StructuredFlowEventToCDPDatalakeFeaturesConverterTest {

    private StructuredFlowEventToCDPDatalakeFeaturesConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToCDPDatalakeFeaturesConverter();
    }

    @Test
    public void testNullCluster() {
        UsageProto.CDPDatalakeFeatures features = underTest.convert(null);

        assertEquals("", features.getRaz().getStatus());
    }

    @Test
    public void testRazEnabled() {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setRazEnabled(true);

        UsageProto.CDPDatalakeFeatures features = underTest.convert(clusterDetails);

        assertEquals("ENABLED", features.getRaz().getStatus());
    }

    @Test
    public void testRazDisabled() {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setRazEnabled(false);

        UsageProto.CDPDatalakeFeatures features = underTest.convert(clusterDetails);

        assertEquals("DISABLED", features.getRaz().getStatus());
    }
}