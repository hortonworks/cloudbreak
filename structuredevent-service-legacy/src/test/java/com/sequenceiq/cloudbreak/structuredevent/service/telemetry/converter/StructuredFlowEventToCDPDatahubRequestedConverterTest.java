package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

class StructuredFlowEventToCDPDatahubRequestedConverterTest {

    private StructuredFlowEventToCDPDatahubRequestedConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StructuredFlowEventToCDPDatahubRequestedConverter();
        StructuredEventToCDPOperationDetailsConverter operationDetailsConverter = new StructuredEventToCDPOperationDetailsConverter();
        ReflectionTestUtils.setField(operationDetailsConverter, "appVersion", "version-1234");
        ReflectionTestUtils.setField(operationDetailsConverter, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
        ReflectionTestUtils.setField(underTest, "operationDetailsConverter", operationDetailsConverter);

        StructuredEventToCDPClusterDetailsConverter clusterDetailsConverter = new StructuredEventToCDPClusterDetailsConverter();
        ReflectionTestUtils.setField(clusterDetailsConverter, "clusterShapeConverter", new StructuredEventToCDPClusterShapeConverter());
        ReflectionTestUtils.setField(clusterDetailsConverter, "imageDetailsConverter", new StructuredEventToCDPImageDetailsConverter());
        ReflectionTestUtils.setField(clusterDetailsConverter, "versionDetailsConverter", new StructuredEventToCDPVersionDetailsConverter());
        ReflectionTestUtils.setField(underTest, "clusterDetailsConverter", clusterDetailsConverter);
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPDatahubRequested datahubRequested = underTest.convert(null);

        assertNotNull(datahubRequested.getOperationDetails());
        assertNotNull(datahubRequested.getClusterDetails());
        assertEquals("", datahubRequested.getEnvironmentCrn());
    }

    @Test
    void testConvertWithEmptyStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        UsageProto.CDPDatahubRequested datahubRequested = underTest.convert(structuredFlowEvent);

        assertNotNull(datahubRequested.getOperationDetails());
        assertNotNull(datahubRequested.getClusterDetails());
        assertEquals("", datahubRequested.getEnvironmentCrn());
    }
}
