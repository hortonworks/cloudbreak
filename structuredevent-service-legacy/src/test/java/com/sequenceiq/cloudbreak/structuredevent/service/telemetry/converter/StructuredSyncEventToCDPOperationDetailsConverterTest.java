package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.SyncDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;

public class StructuredSyncEventToCDPOperationDetailsConverterTest {

    private StructuredSyncEventToCDPOperationDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredSyncEventToCDPOperationDetailsConverter();
        Whitebox.setInternalState(underTest, "appVersion", "version-1234");
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNull("We should return with null if the input is null", underTest.convert(null));
    }

    @Test
    public void testConversionWithNullOperation() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", details.getAccountId());
        Assert.assertEquals("", details.getResourceCrn());
        Assert.assertEquals("", details.getResourceName());
        Assert.assertEquals("", details.getInitiatorCrn());
        Assert.assertEquals("", details.getCorrelationId());
        Assert.assertEquals(UsageProto.CDPRequestProcessingStep.Value.UNSET, details.getCdpRequestProcessingStep());
        Assert.assertEquals("", details.getFlowId());
        Assert.assertEquals("", details.getFlowChainId());
        Assert.assertEquals("", details.getFlowState());
        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET, details.getEnvironmentType());

        Assert.assertEquals("version-1234", details.getApplicationVersion());
    }

    @Test
    public void testFlowRelatedOperationDetailsFieldsReturnEmptyString() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        OperationDetails operationDetails = new OperationDetails();
        operationDetails.setTenant("tenant1");
        operationDetails.setResourceCrn("crn1");
        operationDetails.setResourceName("name1");
        operationDetails.setUserCrn("crn2");
        structuredSyncEvent.setOperation(operationDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("tenant1", details.getAccountId());
        Assert.assertEquals("crn1", details.getResourceCrn());
        Assert.assertEquals("name1", details.getResourceName());
        Assert.assertEquals("crn2", details.getInitiatorCrn());

        Assert.assertEquals("", details.getFlowId());
        Assert.assertEquals("", details.getFlowChainId());
        Assert.assertEquals("", details.getFlowState());
    }

    @Test
    public void testEnvironmentTypeSetCorrectly() {
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        SyncDetails syncDetails = new SyncDetails();
        syncDetails.setCloudPlatform(CloudPlatform.AWS.name());
        structuredSyncEvent.setsyncDetails(syncDetails);

        UsageProto.CDPOperationDetails details = underTest.convert(structuredSyncEvent);

        Assert.assertEquals(UsageProto.CDPEnvironmentsEnvironmentType.Value.AWS, details.getEnvironmentType());
    }
}
