package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

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

public class StackResponseHardwareInfoProviderTest {

    @InjectMocks
    private StackResponseHardwareInfoProvider underTest;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testProviderEntriesToStackResponseEmptyInstanceGroup() {

        Stack stack = new Stack();
        stack.setInstanceGroups(emptySet());

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        Assert.assertEquals(0L, actual.getHardwareInfoGroups().size());
    }

    @Test
    public void testProviderEntriesToStackResponseWhenOneInstanceMetadataPresented() {
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(TestUtil.hostGroup()));

        Stack stack = TestUtil.stack();
        stack.setInstanceGroups(Sets.newHashSet(TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, TestUtil.gcpTemplate(1L))));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        Assert.assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    public void testProviderEntriesToStackResponseClusterNull() {

        Stack stack = new Stack();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        Assert.assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    public void testProviderEntriesToStackResponseClusterNotNullButFQDNNull() {
        Stack stack = new Stack();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        Assert.assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    public void testProviderEntriesToStackResponseClusterNotNullAndFQDNNotNull() {

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

        Assert.assertEquals(1L, actual.getHardwareInfoGroups().size());
    }

    @Test
    public void testProviderEntriesToStackResponseMultipleInstanceGroup() {

        Stack stack = new Stack();
        stack.setCluster(new Cluster());
        Set<InstanceGroup> instanceGroups = getInstanceGroups(new InstanceMetaData());
        instanceGroups.addAll(getInstanceGroups(new InstanceMetaData()));
        stack.setInstanceGroups(instanceGroups);

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        Assert.assertEquals(2L, actual.getHardwareInfoGroups().size());
    }

    @Test
    public void testProviderEntriesToStackResponseConvertsResult() {

        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setId(1L);
        instanceMetaData.setDiscoveryFQDN("fqdn");
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackV4Response actual = underTest.providerEntriesToStackResponse(stack, new StackV4Response());

        Assert.assertEquals(1L, actual.getHardwareInfoGroups().size());

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
