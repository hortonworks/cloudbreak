package com.sequenceiq.cloudbreak.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

class StackDtoTest {

    @Test
    void getAllFunctioningNodesAndReturnWithNewerInstancesIfWeHaveTwoInstancesWithTheSamePrivateIp() {
        Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();
        ArrayList<InstanceMetadataView> workerInstanceMetadataViews = new ArrayList<>();
        InstanceMetadataView instanceMetadataView1 = mock(InstanceMetadataView.class);
        when(instanceMetadataView1.getInstanceStatus()).thenReturn(InstanceStatus.ZOMBIE);
        when(instanceMetadataView1.getPrivateIp()).thenReturn("192.168.1.1");
        when(instanceMetadataView1.getStartDate()).thenReturn(1L);
        when(instanceMetadataView1.getDiscoveryFQDN()).thenReturn("fqdn1");
        workerInstanceMetadataViews.add(instanceMetadataView1);
        InstanceMetadataView instanceMetadataView2 = mock(InstanceMetadataView.class);
        when(instanceMetadataView2.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_HEALTHY);
        when(instanceMetadataView2.getPrivateIp()).thenReturn("192.168.1.1");
        when(instanceMetadataView2.getStartDate()).thenReturn(2L);
        when(instanceMetadataView2.getDiscoveryFQDN()).thenReturn("fqdn2");
        workerInstanceMetadataViews.add(instanceMetadataView2);
        InstanceMetadataView instanceMetadataView3 = mock(InstanceMetadataView.class);
        when(instanceMetadataView3.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_HEALTHY);
        when(instanceMetadataView3.getPrivateIp()).thenReturn("192.168.1.3");
        when(instanceMetadataView3.getDiscoveryFQDN()).thenReturn("fqdn3");
        workerInstanceMetadataViews.add(instanceMetadataView3);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupView.getTemplate()).thenReturn(mock(Template.class));
        instanceGroups.put("worker", new InstanceGroupDto(instanceGroupView, workerInstanceMetadataViews));
        StackDto stackDto = new StackDto(null, null, null, null, null, null, instanceGroups, null, null, null, null, null, null, null, null, null, null);
        Set<Node> allFunctioningNodes = stackDto.getAllFunctioningNodes();
        assertThat(allFunctioningNodes, hasItem(hasProperty("hostname", equalTo("fqdn2"))));
        assertThat(allFunctioningNodes, hasItem(hasProperty("hostname", equalTo("fqdn3"))));
    }
}