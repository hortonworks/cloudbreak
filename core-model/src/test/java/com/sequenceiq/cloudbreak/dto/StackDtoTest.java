package com.sequenceiq.cloudbreak.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

class StackDtoTest {

    @Test
    void getAllFunctioningNodesAndReturnWithNewerInstancesIfWeHaveTwoInstancesWithTheSamePrivateIp() {
        Map<String, InstanceGroupDto> instanceGroups = new HashMap<>();
        ArrayList<InstanceMetadataView> workerInstanceMetadataViews = new ArrayList<>();
        InstanceMetaData instanceMetadataView1 = new InstanceMetaData();
        instanceMetadataView1.setInstanceStatus(InstanceStatus.ZOMBIE);
        instanceMetadataView1.setPrivateIp("192.168.1.1");
        instanceMetadataView1.setStartDate(1L);
        instanceMetadataView1.setDiscoveryFQDN("fqdn1");
        workerInstanceMetadataViews.add(instanceMetadataView1);
        InstanceMetaData instanceMetadataView2 = new InstanceMetaData();
        instanceMetadataView2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView2.setPrivateIp("192.168.1.1");
        instanceMetadataView2.setStartDate(2L);
        instanceMetadataView2.setDiscoveryFQDN("fqdn2");
        workerInstanceMetadataViews.add(instanceMetadataView2);
        InstanceMetaData instanceMetadataView3 = new InstanceMetaData();
        instanceMetadataView3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataView3.setPrivateIp("192.168.1.3");
        instanceMetadataView3.setStartDate(2L);
        instanceMetadataView3.setDiscoveryFQDN("fqdn3");
        workerInstanceMetadataViews.add(instanceMetadataView3);
        InstanceMetaData instanceMetadataView4 = new InstanceMetaData();
        instanceMetadataView4.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
        instanceMetadataView4.setPrivateIp("192.168.1.3");
        instanceMetadataView4.setStartDate(1L);
        instanceMetadataView4.setDiscoveryFQDN("fqdn4");
        workerInstanceMetadataViews.add(instanceMetadataView4);
        InstanceMetaData instanceMetadataView5 = new InstanceMetaData();
        instanceMetadataView5.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        instanceMetadataView5.setPrivateIp("192.168.1.3");
        instanceMetadataView5.setStartDate(1L);
        instanceMetadataView5.setDiscoveryFQDN("fqdn5");
        workerInstanceMetadataViews.add(instanceMetadataView5);
        InstanceMetaData instanceMetadataView6 = new InstanceMetaData();
        instanceMetadataView6.setInstanceStatus(InstanceStatus.ZOMBIE);
        instanceMetadataView6.setPrivateIp("192.168.1.4");
        instanceMetadataView6.setStartDate(1L);
        instanceMetadataView6.setDiscoveryFQDN("fqdn6");
        workerInstanceMetadataViews.add(instanceMetadataView6);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        when(instanceGroupView.getTemplate()).thenReturn(mock(Template.class));
        instanceGroups.put("worker", new InstanceGroupDto(instanceGroupView, workerInstanceMetadataViews));
        StackDto stackDto = new StackDto(null, null, null, null, null, null,
                instanceGroups, null, null, null, null, null, null, null, null, null, null, null, NotificationState.DISABLED);
        Set<Node> allFunctioningNodes = stackDto.getAllFunctioningNodes();
        assertEquals(2, allFunctioningNodes.size());
        assertThat(allFunctioningNodes, hasItem(hasProperty("hostname", equalTo("fqdn2"))));
        assertThat(allFunctioningNodes, hasItem(hasProperty("hostname", equalTo("fqdn3"))));
        Set<Node> allNotDeletedNodes = stackDto.getAllNotDeletedNodes();
        assertEquals(3, allNotDeletedNodes.size());
        assertThat(allNotDeletedNodes, hasItem(hasProperty("hostname", equalTo("fqdn2"))));
        assertThat(allNotDeletedNodes, hasItem(hasProperty("hostname", equalTo("fqdn3"))));
        assertThat(allNotDeletedNodes, hasItem(hasProperty("hostname", equalTo("fqdn6"))));
    }
}