package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

public class StructuredEventToCDPStatusDetailsConverterTest {

    private StructuredEventToCDPStatusDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToCDPStatusDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        assertNotNull(underTest.convert((StructuredFlowEvent) null), "We should return empty object for not null");
        assertNotNull(underTest.convert((StructuredSyncEvent) null), "We should return empty object for not null");
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPStatusDetails flowStatusDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", flowStatusDetails.getStackStatus());
        assertEquals("", flowStatusDetails.getStackDetailedStatus());
        assertEquals("", flowStatusDetails.getStackStatusReason());
        assertEquals("", flowStatusDetails.getClusterStatus());
        assertEquals("", flowStatusDetails.getClusterStatusReason());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPStatusDetails syncStatusDetails = underTest.convert(structuredSyncEvent);

        assertEquals("", syncStatusDetails.getStackStatus());
        assertEquals("", syncStatusDetails.getStackDetailedStatus());
        assertEquals("", syncStatusDetails.getStackStatusReason());
        assertEquals("", syncStatusDetails.getClusterStatus());
        assertEquals("", syncStatusDetails.getClusterStatusReason());
    }

    @Test
    public void testConversionFilledOutValues() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());
        structuredFlowEvent.setCluster(createClusterDetails());

        UsageProto.CDPStatusDetails flowStatusDetails = underTest.convert(structuredFlowEvent);

        assertEquals("AVAILABLE", flowStatusDetails.getStackStatus());
        assertEquals("AVAILABLE", flowStatusDetails.getStackDetailedStatus());
        assertEquals("statusreason", flowStatusDetails.getStackStatusReason());
        assertEquals("AVAILABLE", flowStatusDetails.getClusterStatus());
        assertEquals("statusreason", flowStatusDetails.getClusterStatusReason());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());
        structuredSyncEvent.setCluster(createClusterDetails());

        UsageProto.CDPStatusDetails syncStatusDetails = underTest.convert(structuredSyncEvent);

        assertEquals("AVAILABLE", syncStatusDetails.getStackStatus());
        assertEquals("AVAILABLE", syncStatusDetails.getStackDetailedStatus());
        assertEquals("statusreason", syncStatusDetails.getStackStatusReason());
        assertEquals("AVAILABLE", syncStatusDetails.getClusterStatus());
        assertEquals("statusreason", syncStatusDetails.getClusterStatusReason());
    }

    @Test
    public void testStatusReasonStringTrimming() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        ClusterDetails clusterDetails = new ClusterDetails();
        structuredFlowEvent.setStack(stackDetails);
        structuredFlowEvent.setCluster(clusterDetails);

        UsageProto.CDPStatusDetails flowStatusDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", flowStatusDetails.getStackStatusReason());
        assertEquals("", flowStatusDetails.getClusterStatusReason());

        stackDetails.setStatusReason("");
        clusterDetails.setStatusReason("");
        flowStatusDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", flowStatusDetails.getStackStatusReason());
        assertEquals("", flowStatusDetails.getClusterStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 10));
        clusterDetails.setStatusReason(StringUtils.repeat("*", 10));
        flowStatusDetails = underTest.convert(structuredFlowEvent);

        assertEquals(StringUtils.repeat("*", 10), flowStatusDetails.getStackStatusReason());
        assertEquals(StringUtils.repeat("*", 10), flowStatusDetails.getClusterStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 1500));
        clusterDetails.setStatusReason(StringUtils.repeat("*", 1500));
        flowStatusDetails = underTest.convert(structuredFlowEvent);

        assertEquals(StringUtils.repeat("*", 1500), flowStatusDetails.getStackStatusReason());
        assertEquals(StringUtils.repeat("*", 1500), flowStatusDetails.getClusterStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 3000));
        clusterDetails.setStatusReason(StringUtils.repeat("*", 3000));
        flowStatusDetails = underTest.convert(structuredFlowEvent);

        assertEquals(StringUtils.repeat("*", 1500), flowStatusDetails.getStackStatusReason());
        assertEquals(StringUtils.repeat("*", 1500), flowStatusDetails.getClusterStatusReason());

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
