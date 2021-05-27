package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

class StructuredEventToClusterShapeConverterTest {

    private StructuredEventToClusterShapeConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToClusterShapeConverter();
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPClusterShape flowClusterShape = underTest.convert((StructuredFlowEvent) null);

        Assert.assertEquals("", flowClusterShape.getClusterTemplateName());
        Assert.assertEquals(-1, flowClusterShape.getNodes());
        Assert.assertEquals("", flowClusterShape.getDefinitionDetails());
        Assert.assertFalse(flowClusterShape.getTemporaryStorageUsed());

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert((StructuredSyncEvent) null);

        Assert.assertEquals("", syncClusterShape.getClusterTemplateName());
        Assert.assertEquals(-1, syncClusterShape.getNodes());
        Assert.assertEquals("", syncClusterShape.getDefinitionDetails());
        Assert.assertFalse(syncClusterShape.getTemporaryStorageUsed());
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", flowClusterShape.getClusterTemplateName());
        Assert.assertEquals(-1, flowClusterShape.getNodes());
        Assert.assertEquals("", flowClusterShape.getDefinitionDetails());
        Assert.assertFalse(flowClusterShape.getTemporaryStorageUsed());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", syncClusterShape.getClusterTemplateName());
        Assert.assertEquals(-1, syncClusterShape.getNodes());
        Assert.assertEquals("", syncClusterShape.getDefinitionDetails());
        Assert.assertFalse(syncClusterShape.getTemporaryStorageUsed());
    }

    @Test
    public void testConversionWithValues() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());
        BlueprintDetails flowBlueprintDetails = new BlueprintDetails();
        flowBlueprintDetails.setName("My Blueprint");
        structuredFlowEvent.setBlueprintDetails(flowBlueprintDetails);

        UsageProto.CDPClusterShape flowClusterShape = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("My Blueprint", flowClusterShape.getClusterTemplateName());
        Assert.assertEquals(10, flowClusterShape.getNodes());
        Assert.assertEquals("compute=3, gw=4, master=1, worker=2", flowClusterShape.getHostGroupNodeCount());
        Assert.assertTrue(flowClusterShape.getTemporaryStorageUsed());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());
        BlueprintDetails syncBlueprintDetails = new BlueprintDetails();
        syncBlueprintDetails.setName("My Blueprint");
        structuredSyncEvent.setBlueprintDetails(syncBlueprintDetails);

        UsageProto.CDPClusterShape syncClusterShape = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("My Blueprint", syncClusterShape.getClusterTemplateName());
        Assert.assertEquals(10, syncClusterShape.getNodes());
        Assert.assertEquals("compute=3, gw=4, master=1, worker=2", syncClusterShape.getHostGroupNodeCount());
        Assert.assertTrue(syncClusterShape.getTemporaryStorageUsed());
    }

    private StackDetails createStackDetails() {
        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = createInstanceGroupDetails("master", 1, TemporaryStorage.ATTACHED_VOLUMES.name());
        InstanceGroupDetails worker = createInstanceGroupDetails("worker", 2, TemporaryStorage.ATTACHED_VOLUMES.name());
        InstanceGroupDetails compute = createInstanceGroupDetails("compute", 3, TemporaryStorage.EPHEMERAL_VOLUMES.name());
        InstanceGroupDetails gw = createInstanceGroupDetails("gw", 4, TemporaryStorage.ATTACHED_VOLUMES.name());

        stackDetails.setInstanceGroups(List.of(master, worker, compute, gw));
        return stackDetails;
    }

    private InstanceGroupDetails createInstanceGroupDetails(String groupName, int nodeCount, String storage) {
        InstanceGroupDetails ig = new InstanceGroupDetails();
        ig.setGroupName(groupName);
        ig.setNodeCount(nodeCount);
        ig.setTemporaryStorage(storage);
        return ig;
    }
}