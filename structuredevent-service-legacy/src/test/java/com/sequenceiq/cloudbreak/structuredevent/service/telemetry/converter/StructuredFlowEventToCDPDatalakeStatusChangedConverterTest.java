package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

public class StructuredFlowEventToCDPDatalakeStatusChangedConverterTest {

    private StructuredFlowEventToCDPDatalakeStatusChangedConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToCDPDatalakeStatusChangedConverter();
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
        Whitebox.setInternalState(underTest, "featuresConverter", new StructuredEventToCDPDatalakeFeaturesConverter());
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPDatalakeStatusChanged datalakeStatusChanged = underTest.convert(null, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        Assertions.assertNotNull(datalakeStatusChanged.getOperationDetails());
        Assertions.assertNotNull(datalakeStatusChanged.getClusterDetails());
        Assertions.assertNotNull(datalakeStatusChanged.getStatusDetails());
        Assertions.assertNotNull(datalakeStatusChanged.getFeatures());
        Assertions.assertEquals("", datalakeStatusChanged.getEnvironmentCrn());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datalakeStatusChanged.getNewStatus());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datalakeStatusChanged.getOldStatus());
    }

    @Test
    public void testConvertWithEmptyStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        UsageProto.CDPDatalakeStatusChanged datalakeStatusChanged = underTest.convert(structuredFlowEvent, UsageProto.CDPClusterStatus.Value.CREATE_STARTED);

        Assertions.assertNotNull(datalakeStatusChanged.getOperationDetails());
        Assertions.assertNotNull(datalakeStatusChanged.getClusterDetails());
        Assertions.assertNotNull(datalakeStatusChanged.getStatusDetails());
        Assertions.assertNotNull(datalakeStatusChanged.getFeatures());
        Assertions.assertEquals("", datalakeStatusChanged.getEnvironmentCrn());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.CREATE_STARTED, datalakeStatusChanged.getNewStatus());
        Assertions.assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, datalakeStatusChanged.getOldStatus());
    }
}
