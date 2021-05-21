package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterRequestProcessingStepMapper;

public class StructuredSyncEventToCDPDatalakeSyncConverterTest {

    private StructuredSyncEventToCDPDatalakeSyncConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredSyncEventToCDPDatalakeSyncConverter();
        StructuredEventToCDPOperationDetailsConverter operationDetailsConverter = new StructuredEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(operationDetailsConverter, "appVersion", "version-1234");
        Whitebox.setInternalState(operationDetailsConverter, "clusterRequestProcessingStepMapper", new ClusterRequestProcessingStepMapper());
        Whitebox.setInternalState(underTest, "operationDetailsConverter", operationDetailsConverter);
        StructuredEventToClusterDetailsConverter clusterDetailsConverter = new StructuredEventToClusterDetailsConverter();
        Whitebox.setInternalState(clusterDetailsConverter, "clusterShapeConverter", new StructuredEventToClusterShapeConverter());
        Whitebox.setInternalState(clusterDetailsConverter, "imageDetailsConverter", new StructuredEventToImageDetailsConverter());
        Whitebox.setInternalState(clusterDetailsConverter, "versionDetailsConverter", new StructuredEventToVersionDetailsConverter());
        Whitebox.setInternalState(underTest, "clusterDetailsConverter", clusterDetailsConverter);
        Whitebox.setInternalState(underTest, "syncDetailsConverter", new StructuredSyncEventToCDPSyncDetailsConverter());
        Whitebox.setInternalState(underTest, "statusDetailsConverter", new StructuredEventToStatusDetailsConverter());
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPDatalakeSync datalakeSync = underTest.convert(null);

        Assertions.assertNotNull(datalakeSync.getOperationDetails());
        Assertions.assertNotNull(datalakeSync.getSyncDetails());
        Assertions.assertNotNull(datalakeSync.getClusterDetails());
        Assertions.assertNotNull(datalakeSync.getStatusDetails());
    }

    @Test
    public void testConvertWithEmptyStructuredSyncEvent() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        UsageProto.CDPDatalakeSync datalakeSync = underTest.convert(structuredSyncEvent);

        Assertions.assertNotNull(datalakeSync.getOperationDetails());
        Assertions.assertNotNull(datalakeSync.getSyncDetails());
        Assertions.assertNotNull(datalakeSync.getClusterDetails());
        Assertions.assertNotNull(datalakeSync.getStatusDetails());
    }
}
