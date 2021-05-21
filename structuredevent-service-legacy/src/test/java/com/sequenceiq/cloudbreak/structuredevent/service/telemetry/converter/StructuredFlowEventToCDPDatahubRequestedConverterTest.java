package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

public class StructuredFlowEventToCDPDatahubRequestedConverterTest {

    private StructuredFlowEventToCDPDatahubRequestedConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToCDPDatahubRequestedConverter();
        StructuredEventToCDPOperationDetailsConverter operationDetailsConverter = new StructuredEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(operationDetailsConverter, "appVersion", "version-1234");
        Whitebox.setInternalState(operationDetailsConverter, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
        Whitebox.setInternalState(underTest, "operationDetailsConverter", operationDetailsConverter);
        StructuredEventToClusterDetailsConverter clusterDetailsConverter = new StructuredEventToClusterDetailsConverter();
        Whitebox.setInternalState(clusterDetailsConverter, "clusterShapeConverter", new StructuredEventToClusterShapeConverter());
        Whitebox.setInternalState(clusterDetailsConverter, "imageDetailsConverter", new StructuredEventToImageDetailsConverter());
        Whitebox.setInternalState(clusterDetailsConverter, "versionDetailsConverter", new StructuredEventToVersionDetailsConverter());
        Whitebox.setInternalState(underTest, "clusterDetailsConverter", clusterDetailsConverter);
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPDatahubRequested datahubRequested = underTest.convert(null);

        Assertions.assertNotNull(datahubRequested.getOperationDetails());
        Assertions.assertNotNull(datahubRequested.getClusterDetails());
        Assertions.assertEquals("", datahubRequested.getEnvironmentCrn());
    }

    @Test
    public void testConvertWithEmptyStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        UsageProto.CDPDatahubRequested datahubRequested = underTest.convert(structuredFlowEvent);

        Assertions.assertNotNull(datahubRequested.getOperationDetails());
        Assertions.assertNotNull(datahubRequested.getClusterDetails());
        Assertions.assertEquals("", datahubRequested.getEnvironmentCrn());
    }
}
