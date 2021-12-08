package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
public class StructuredFlowEventToCDPDatahubStatusChangedConverterTest {

    private StructuredFlowEventToCDPDatahubStatusChangedConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToCDPDatahubStatusChangedConverter();
        StructuredEventToCDPOperationDetailsConverter operationDetailsConverter = new StructuredEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(operationDetailsConverter, "appVersion", "version-1234");
        Whitebox.setInternalState(operationDetailsConverter, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
        Whitebox.setInternalState(underTest, "operationDetailsConverter", operationDetailsConverter);
        StructuredEventToCDPClusterDetailsConverter clusterDetailsConverter = new StructuredEventToCDPClusterDetailsConverter();
        Whitebox.setInternalState(clusterDetailsConverter, "clusterShapeConverter", new StructuredEventToCDPClusterShapeConverter());
        Whitebox.setInternalState(clusterDetailsConverter, "imageDetailsConverter", new StructuredEventToCDPImageDetailsConverter());
        Whitebox.setInternalState(clusterDetailsConverter, "versionDetailsConverter", new StructuredEventToCDPVersionDetailsConverter());
        Whitebox.setInternalState(underTest, "clusterDetailsConverter", clusterDetailsConverter);
        Whitebox.setInternalState(underTest, "statusDetailsConverter", new StructuredEventToCDPStatusDetailsConverter());
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPDatahubStatusChanged datahubStatusChanged = underTest.convert(null, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        Assertions.assertNotNull(datahubStatusChanged.getOperationDetails());
        Assertions.assertNotNull(datahubStatusChanged.getClusterDetails());
        Assertions.assertNotNull(datahubStatusChanged.getStatusDetails());
        Assertions.assertEquals("", datahubStatusChanged.getEnvironmentCrn());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datahubStatusChanged.getNewStatus());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datahubStatusChanged.getOldStatus());
    }

    @Test
    public void testConvertWithEmptyStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        UsageProto.CDPDatahubStatusChanged datahubStatusChanged = underTest.convert(structuredFlowEvent, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        Assertions.assertNotNull(datahubStatusChanged.getOperationDetails());
        Assertions.assertNotNull(datahubStatusChanged.getClusterDetails());
        Assertions.assertNotNull(datahubStatusChanged.getStatusDetails());
        Assertions.assertEquals("", datahubStatusChanged.getEnvironmentCrn());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datahubStatusChanged.getNewStatus());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datahubStatusChanged.getOldStatus());
    }
}
