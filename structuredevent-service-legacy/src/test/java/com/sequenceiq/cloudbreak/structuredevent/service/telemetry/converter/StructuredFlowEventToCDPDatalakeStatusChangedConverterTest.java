package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

class StructuredFlowEventToCDPDatalakeStatusChangedConverterTest {

    private StructuredFlowEventToCDPDatalakeStatusChangedConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StructuredFlowEventToCDPDatalakeStatusChangedConverter();
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
        ReflectionTestUtils.setField(underTest, "featuresConverter", new StructuredEventToCDPDatalakeFeaturesConverter());
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPDatalakeStatusChanged datalakeStatusChanged = underTest.convert(null, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        assertNotNull(datalakeStatusChanged.getOperationDetails());
        assertNotNull(datalakeStatusChanged.getClusterDetails());
        assertNotNull(datalakeStatusChanged.getStatusDetails());
        assertNotNull(datalakeStatusChanged.getFeatures());
        assertEquals("", datalakeStatusChanged.getEnvironmentCrn());
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datalakeStatusChanged.getNewStatus());
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datalakeStatusChanged.getOldStatus());
    }

    @Test
    void testConvertWithEmptyStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        UsageProto.CDPDatalakeStatusChanged datalakeStatusChanged = underTest.convert(structuredFlowEvent, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        assertNotNull(datalakeStatusChanged.getOperationDetails());
        assertNotNull(datalakeStatusChanged.getClusterDetails());
        assertNotNull(datalakeStatusChanged.getStatusDetails());
        assertNotNull(datalakeStatusChanged.getFeatures());
        assertEquals("", datalakeStatusChanged.getEnvironmentCrn());
        assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datalakeStatusChanged.getNewStatus());
        assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datalakeStatusChanged.getOldStatus());
    }
}
