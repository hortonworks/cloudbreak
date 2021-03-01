package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

public class StructuredFlowEventToStatusDetailsConverterTest {

    private StructuredEventToStatusDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToStatusDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredFlowEvent) null));
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredSyncEvent) null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPStatusDetails flowStatusDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", flowStatusDetails.getStackStatus());
        Assert.assertEquals("", flowStatusDetails.getStackDetailedStatus());
        Assert.assertEquals("", flowStatusDetails.getStackStatusReason());
        Assert.assertEquals("", flowStatusDetails.getClusterStatus());
        Assert.assertEquals("", flowStatusDetails.getClusterStatusReason());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPStatusDetails syncStatusDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", syncStatusDetails.getStackStatus());
        Assert.assertEquals("", syncStatusDetails.getStackDetailedStatus());
        Assert.assertEquals("", syncStatusDetails.getStackStatusReason());
        Assert.assertEquals("", syncStatusDetails.getClusterStatus());
        Assert.assertEquals("", syncStatusDetails.getClusterStatusReason());
    }

    @Test
    public void testConversionFilledOutValues() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());
        structuredFlowEvent.setCluster(createClusterDetails());

        UsageProto.CDPStatusDetails flowStatusDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("AVAILABLE", flowStatusDetails.getStackStatus());
        Assert.assertEquals("AVAILABLE", flowStatusDetails.getStackDetailedStatus());
        Assert.assertEquals("statusreason", flowStatusDetails.getStackStatusReason());
        Assert.assertEquals("AVAILABLE", flowStatusDetails.getClusterStatus());
        Assert.assertEquals("statusreason", flowStatusDetails.getClusterStatusReason());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());
        structuredSyncEvent.setCluster(createClusterDetails());

        UsageProto.CDPStatusDetails syncStatusDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("AVAILABLE", syncStatusDetails.getStackStatus());
        Assert.assertEquals("AVAILABLE", syncStatusDetails.getStackDetailedStatus());
        Assert.assertEquals("statusreason", syncStatusDetails.getStackStatusReason());
        Assert.assertEquals("AVAILABLE", syncStatusDetails.getClusterStatus());
        Assert.assertEquals("statusreason", syncStatusDetails.getClusterStatusReason());
    }

    private StackDetails createStackDetails() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setStatus("AVAILABLE");
        stackDetails.setDetailedStatus("AVAILABLE");
        stackDetails.setStatusReason("statusreason");

        return stackDetails;
    }

    private ClusterDetails createClusterDetails() {
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setStatus("AVAILABLE");
        clusterDetails.setStatusReason("statusreason");

        return clusterDetails;
    }
}
