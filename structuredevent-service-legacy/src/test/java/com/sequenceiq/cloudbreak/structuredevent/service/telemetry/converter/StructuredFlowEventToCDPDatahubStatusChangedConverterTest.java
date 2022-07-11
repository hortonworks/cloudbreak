package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
class StructuredFlowEventToCDPDatahubStatusChangedConverterTest {

    private StructuredFlowEventToCDPDatahubStatusChangedConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StructuredFlowEventToCDPDatahubStatusChangedConverter();
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
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPDatahubStatusChanged datahubStatusChanged = underTest.convert(null, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        assertNotNull(datahubStatusChanged.getOperationDetails());
        assertNotNull(datahubStatusChanged.getClusterDetails());
        assertNotNull(datahubStatusChanged.getStatusDetails());
        assertEquals("", datahubStatusChanged.getEnvironmentCrn());
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datahubStatusChanged.getNewStatus());
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datahubStatusChanged.getOldStatus());
    }

    @Test
    void testConvertWithEmptyStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        UsageProto.CDPDatahubStatusChanged datahubStatusChanged = underTest.convert(structuredFlowEvent, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        assertNotNull(datahubStatusChanged.getOperationDetails());
        assertNotNull(datahubStatusChanged.getClusterDetails());
        assertNotNull(datahubStatusChanged.getStatusDetails());
        assertEquals("", datahubStatusChanged.getEnvironmentCrn());
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datahubStatusChanged.getNewStatus());
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datahubStatusChanged.getOldStatus());
    }
}
