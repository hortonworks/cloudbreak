package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

class StructuredFlowEventToClusterShapeConverterTest {

    private StructuredFlowEventToClusterShapeConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToClusterShapeConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert(null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPClusterShape clusterShape = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", clusterShape.getClusterTemplateName());
        Assert.assertEquals(0, clusterShape.getNodes());
        Assert.assertEquals("", clusterShape.getDefinitionDetails());
    }

    @Test
    public void testConversionWithValues() {
        StructuredFlowEvent structuredFlowEvent = createStructuredFlowEvent();

        UsageProto.CDPClusterShape clusterShape = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("My Blueprint", clusterShape.getClusterTemplateName());
        Assert.assertEquals(10, clusterShape.getNodes());
        Assert.assertEquals("compute=3, gw=4, master=1, worker=2", clusterShape.getHostGroupNodeCount());
    }

    private StructuredFlowEvent createStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = createInstanceGroupDetails("master", 1);
        InstanceGroupDetails worker = createInstanceGroupDetails("worker", 2);
        InstanceGroupDetails compute = createInstanceGroupDetails("compute", 3);
        InstanceGroupDetails gw = createInstanceGroupDetails("gw", 4);

        stackDetails.setInstanceGroups(List.of(master, worker, compute, gw));
        structuredFlowEvent.setStack(stackDetails);

        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setName("My Blueprint");
        structuredFlowEvent.setBlueprintDetails(blueprintDetails);

        return structuredFlowEvent;
    }

    private InstanceGroupDetails createInstanceGroupDetails(String groupName, int nodeCount) {
        InstanceGroupDetails ig = new InstanceGroupDetails();
        ig.setGroupName(groupName);
        ig.setNodeCount(nodeCount);
        return ig;
    }
}