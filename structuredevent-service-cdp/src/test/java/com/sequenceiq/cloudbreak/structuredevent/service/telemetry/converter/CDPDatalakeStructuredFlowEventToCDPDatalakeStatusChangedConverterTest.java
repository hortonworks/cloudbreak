package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPDatalakeStatusChanged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.CDPDatalakeStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.CDPRequestProcessingStepMapper;

@ExtendWith(MockitoExtension.class)
class CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverterTest {

    private CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter underTest;

    @BeforeEach()
    void setUp() {
        underTest = new CDPDatalakeStructuredFlowEventToCDPDatalakeStatusChangedConverter();
        CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter = new CDPStructuredFlowEventToCDPOperationDetailsConverter();
        ReflectionTestUtils.setField(operationDetailsConverter, "appVersion", "version-1234");
        ReflectionTestUtils.setField(operationDetailsConverter, "cdpRequestProcessingStepMapper", new CDPRequestProcessingStepMapper());
        ReflectionTestUtils.setField(underTest, "operationDetailsConverter", operationDetailsConverter);
        ReflectionTestUtils.setField(underTest, "statusDetailsConverter", new DatalakeDetailsToCDPStatusDetailsConverter());
        ReflectionTestUtils.setField(underTest, "clusterDetailsConverter", new DatalakeDetailsToCDPClusterDetailsConverter());
        ReflectionTestUtils.setField(underTest, "featuresConverter", new DatalakeDetailsToCDPDatalakeFeaturesConverter());
    }

    @Test
    void testNullStructuredFlowEvent() {
        CDPDatalakeStatusChanged datalakeStatusChanged = underTest.convert(null, CREATE_STARTED);

        assertEquals(Value.CREATE_STARTED, datalakeStatusChanged.getNewStatus());
        assertEquals(Value.UNSET, datalakeStatusChanged.getOldStatus());
        assertNotNull(datalakeStatusChanged.getOperationDetails());
        assertNotNull(datalakeStatusChanged.getStatusDetails());
        assertNotNull(datalakeStatusChanged.getFeatures());
        assertNotNull(datalakeStatusChanged.getClusterDetails());
    }

    @Test
    void testConvertingEmptyStructuredFlowEvent() {
        CDPDatalakeStructuredFlowEvent cdpStructuredFlowEvent = new CDPDatalakeStructuredFlowEvent();
        CDPDatalakeStatusChanged datalakeStatusChanged = underTest.convert(cdpStructuredFlowEvent, Value.CREATE_STARTED);

        assertEquals(Value.CREATE_STARTED, datalakeStatusChanged.getNewStatus());
        assertEquals(Value.UNSET, datalakeStatusChanged.getOldStatus());
        assertNotNull(datalakeStatusChanged.getOperationDetails());
        assertNotNull(datalakeStatusChanged.getOperationDetails());
        assertNotNull(datalakeStatusChanged.getStatusDetails());
        assertNotNull(datalakeStatusChanged.getFeatures());
        assertNotNull(datalakeStatusChanged.getClusterDetails());
    }
}