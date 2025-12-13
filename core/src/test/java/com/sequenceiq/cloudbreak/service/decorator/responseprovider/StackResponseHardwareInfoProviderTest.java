package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class StackResponseHardwareInfoProviderTest {

    @InjectMocks
    private StackResponseHardwareInfoProvider underTest;

    @Mock
    private HostGroupService hostGroupService;

    @Test
    void testProviderEntriesToStackResponseEmptyInstanceGroup() {

        Stack stack = new Stack();
        stack.setInstanceGroups(emptySet());

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        assertEquals(0L, actual.getHardwareInfoGroups().size());
    }

    @Test
    void testProviderEntriesToStackResponseWhenOneInstanceMetadataPresented() {
        Stack stack = TestUtil.stack();
        stack.setInstanceGroups(Sets.newHashSet(TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, TestUtil.gcpTemplate(1L))));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    void testProviderEntriesToStackResponseClusterNull() {

        Stack stack = new Stack();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    void testProviderEntriesToStackResponseClusterNotNullButFQDNNull() {
        Stack stack = new Stack();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    void testProviderEntriesToStackResponseClusterNotNullAndFQDNNotNull() {

        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setId(1L);
        instanceMetaData.setDiscoveryFQDN("fqdn");
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackV4Response stackResponse = new StackV4Response();
        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, stackResponse);

        assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    void testProviderEntriesToStackResponseMultipleInstanceGroup() {

        Stack stack = new Stack();
        stack.setCluster(new Cluster());
        Set<InstanceGroup> instanceGroups = getInstanceGroups(new InstanceMetaData());
        instanceGroups.addAll(getInstanceGroups(new InstanceMetaData()));
        stack.setInstanceGroups(instanceGroups);

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        assertEquals(2L, actual.getHardwareInfoGroups().size());
    }

    @Test
    void testProviderEntriesToStackResponseConvertsResult() {

        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setId(1L);
        instanceMetaData.setDiscoveryFQDN("fqdn");
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        assertEquals(1L, actual.getHardwareInfoGroups().size());

    }

    private Set<InstanceGroup> getInstanceGroups(InstanceMetaData... instanceMetaData) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        for (InstanceMetaData im : instanceMetaData) {
            im.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
            im.setInstanceGroup(instanceGroup);
        }
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        return Sets.newHashSet(instanceGroup);
    }
}
