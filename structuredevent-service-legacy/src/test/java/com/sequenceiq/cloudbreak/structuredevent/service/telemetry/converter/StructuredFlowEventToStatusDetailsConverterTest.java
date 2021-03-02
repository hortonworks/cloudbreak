package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

public class StructuredFlowEventToStatusDetailsConverterTest {

    private StructuredFlowEventToStatusDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToStatusDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert(null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPStatusDetails statusDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", statusDetails.getStackStatus());
        Assert.assertEquals("", statusDetails.getStackDetailedStatus());
        Assert.assertEquals("", statusDetails.getStackStatusReason());
        Assert.assertEquals("", statusDetails.getClusterStatus());
        Assert.assertEquals("", statusDetails.getClusterStatusReason());
    }

    @Test
    public void testConversionFilledOutValues() {
        StructuredFlowEvent structuredFlowEvent = createStructuredFlowEvent();

        UsageProto.CDPStatusDetails statusDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("AVAILABLE", statusDetails.getStackStatus());
        Assert.assertEquals("AVAILABLE", statusDetails.getStackDetailedStatus());
        Assert.assertEquals("statusreason", statusDetails.getStackStatusReason());
        Assert.assertEquals("AVAILABLE", statusDetails.getClusterStatus());
        Assert.assertEquals("statusreason", statusDetails.getClusterStatusReason());
    }

    private StructuredFlowEvent createStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        StackDetails stackDetails = new StackDetails();
        stackDetails.setStatus("AVAILABLE");
        stackDetails.setDetailedStatus("AVAILABLE");
        stackDetails.setStatusReason("statusreason");

        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setStatus("AVAILABLE");
        clusterDetails.setStatusReason("statusreason");

        structuredFlowEvent.setStack(stackDetails);
        structuredFlowEvent.setCluster(clusterDetails);

        return structuredFlowEvent;
    }
}
