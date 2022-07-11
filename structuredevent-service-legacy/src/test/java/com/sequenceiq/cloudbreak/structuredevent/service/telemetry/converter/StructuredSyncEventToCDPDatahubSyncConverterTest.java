package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
class StructuredSyncEventToCDPDatahubSyncConverterTest {

    private StructuredSyncEventToCDPDatahubSyncConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StructuredSyncEventToCDPDatahubSyncConverter();
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
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPDatahubSync datahubSync = underTest.convert(null);

        assertNotNull(datahubSync.getOperationDetails());
        assertNotNull(datahubSync.getSyncDetails());
        assertNotNull(datahubSync.getClusterDetails());
        assertNotNull(datahubSync.getStatusDetails());
    }

    @Test
    void testConvertWithEmptyStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        UsageProto.CDPDatahubSync datahubSync = underTest.convert(structuredSyncEvent);

        assertNotNull(datahubSync.getOperationDetails());
        assertNotNull(datahubSync.getSyncDetails());
        assertNotNull(datahubSync.getClusterDetails());
        assertNotNull(datahubSync.getStatusDetails());
    }
}
