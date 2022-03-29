package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@ExtendWith(MockitoExtension.class)
public class StackDetailsToCDPFreeIPAShapeConverterTest {

    private StackDetailsToCDPFreeIPAShapeConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StackDetailsToCDPFreeIPAShapeConverter();
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPFreeIPAShape freeIPAShape = underTest.convert(null);

        Assertions.assertEquals(-1, freeIPAShape.getNodes());
        Assertions.assertEquals("", freeIPAShape.getHostGroupNodeCount());
    }

    @Test
    public void testConvertWithEmpty() {
        UsageProto.CDPFreeIPAShape freeIPAShape = underTest.convert(new StackDetails());

        Assertions.assertEquals(-1, freeIPAShape.getNodes());
        Assertions.assertEquals("", freeIPAShape.getHostGroupNodeCount());
    }

    @Test
    public void testConversionWithValues() {
        StackDetails stackDetails = new StackDetails();
        InstanceGroupDetails master = new InstanceGroupDetails();
        master.setGroupName("master");
        master.setNodeCount(2);
        stackDetails.setInstanceGroups(List.of(master));

        UsageProto.CDPFreeIPAShape freeIPAShape = underTest.convert(stackDetails);

        Assertions.assertEquals(2, freeIPAShape.getNodes());
        Assertions.assertEquals("master=2", freeIPAShape.getHostGroupNodeCount());
    }
}
