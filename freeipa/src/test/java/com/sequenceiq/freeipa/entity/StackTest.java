package com.sequenceiq.freeipa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;

public class StackTest {

    private Stack underTest = new Stack();

    @Test
    public void testGetPrimaryGatewayWhenPresent() {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData1, instanceMetaData2));
        underTest.setInstanceGroups(Set.of(instanceGroup));

        Optional<InstanceMetaData> primaryGateway = underTest.getPrimaryGateway();

        assertEquals(Optional.of(instanceMetaData2), primaryGateway);
    }

    @Test
    public void testGetPrimaryGatewayWhenNotPresent() {
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData1, instanceMetaData2));
        underTest.setInstanceGroups(Set.of(instanceGroup));

        Optional<InstanceMetaData> primaryGateway = underTest.getPrimaryGateway();

        assertEquals(Optional.empty(), primaryGateway);
    }

}