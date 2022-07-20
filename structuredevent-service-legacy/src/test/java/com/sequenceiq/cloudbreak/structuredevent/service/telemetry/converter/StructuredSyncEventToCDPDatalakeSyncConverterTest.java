package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

class StructuredSyncEventToCDPDatalakeSyncConverterTest {

    private StructuredSyncEventToCDPDatalakeSyncConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StructuredSyncEventToCDPDatalakeSyncConverter();
        StructuredEventToCDPOperationDetailsConverter operationDetailsConverter = new StructuredEventToCDPOperationDetailsConverter();
        ReflectionTestUtils.setField(operationDetailsConverter, "appVersion", "version-1234");
        ReflectionTestUtils.setField(operationDetailsConverter, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
        ReflectionTestUtils.setField(underTest, "operationDetailsConverter", operationDetailsConverter);
        StructuredEventToCDPClusterDetailsConverter clusterDetailsConverter = new StructuredEventToCDPClusterDetailsConverter();
        ReflectionTestUtils.setField(clusterDetailsConverter, "clusterShapeConverter", new StructuredEventToCDPClusterShapeConverter());
        ReflectionTestUtils.setField(clusterDetailsConverter, "imageDetailsConverter", new StructuredEventToCDPImageDetailsConverter());
        ReflectionTestUtils.setField(clusterDetailsConverter, "versionDetailsConverter", new StructuredEventToCDPVersionDetailsConverter());
        ReflectionTestUtils.setField(underTest, "clusterDetailsConverter", clusterDetailsConverter);
        ReflectionTestUtils.setField(underTest, "statusDetailsConverter", new StructuredEventToCDPStatusDetailsConverter());
        ReflectionTestUtils.setField(underTest, "syncDetailsConverter", new StructuredSyncEventToCDPSyncDetailsConverter());
        ReflectionTestUtils.setField(underTest, "featuresConverter", new StructuredEventToCDPDatalakeFeaturesConverter());
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPDatalakeSync datalakeSync = underTest.convert(null);

        assertNotNull(datalakeSync.getOperationDetails());
        assertNotNull(datalakeSync.getSyncDetails());
        assertNotNull(datalakeSync.getClusterDetails());
        assertNotNull(datalakeSync.getStatusDetails());
        assertNotNull(datalakeSync.getFeatures());
    }

    @Test
    void testConvertWithEmptyStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        UsageProto.CDPDatalakeSync datalakeSync = underTest.convert(structuredSyncEvent);

        assertNotNull(datalakeSync.getOperationDetails());
        assertNotNull(datalakeSync.getSyncDetails());
        assertNotNull(datalakeSync.getClusterDetails());
        assertNotNull(datalakeSync.getStatusDetails());
        assertNotNull(datalakeSync.getFeatures());
    }
}
