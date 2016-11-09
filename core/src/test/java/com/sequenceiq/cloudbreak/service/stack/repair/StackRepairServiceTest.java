package com.sequenceiq.cloudbreak.service.stack.repair;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.repair.StackRepairNotificationRequest;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StackRepairServiceTest {

    @Mock
    private ReactorFlowManager reactorFlowManager;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @InjectMocks
    private StackRepairService stackRepairService;

    @Test
    public void shouldIgnoreIfNoInstancesToRepair() {
        Stack stack = mock(Stack.class);
        StackRepairNotificationRequest stackRepairNotificationRequest = new StackRepairNotificationRequest(stack, Collections.EMPTY_SET);
        stackRepairService.add(stackRepairNotificationRequest);
        verifyZeroInteractions(reactorFlowManager);
    }

    @Test
    public void shouldGroupUnhealthyInstancesByHostGroup() {
        long stackId = 1L;
        long clusterId = 2L;

        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(stackId);
        Cluster cluster = mock(Cluster.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(clusterId);

        String instanceId1 = "i-0f1e0605506aaaaaa";
        String instanceId2 = "i-0f1e0605506bbbbbb";
        String instanceId3 = "i-0f1e0605506cccccc";

        String privateIp1 = "ip-10-0-0-1.ec2.internal";
        String privateIp2 = "ip-10-0-0-2.ec2.internal";
        String privateIp3 = "ip-10-0-0-3.ec2.internal";

        setupInstanceMetadata(stackId, instanceId1, privateIp1);
        setupInstanceMetadata(stackId, instanceId2, privateIp2);
        setupInstanceMetadata(stackId, instanceId3, privateIp3);

        String slaveGroup1 = "slave_group1";
        String slaveGroup2 = "slave_group2";

        setupHostMetadata(clusterId, privateIp1, slaveGroup1);
        setupHostMetadata(clusterId, privateIp2, slaveGroup2);
        setupHostMetadata(clusterId, privateIp3, slaveGroup2);

        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(instanceId1);
        instanceIds.add(instanceId2);
        instanceIds.add(instanceId3);
        StackRepairNotificationRequest stackRepairNotificationRequest = new StackRepairNotificationRequest(stack, instanceIds);

        stackRepairService.add(stackRepairNotificationRequest);
        UnhealthyInstances expectedUnhealthyInstances = new UnhealthyInstances();
        expectedUnhealthyInstances.addInstance(instanceId1, slaveGroup1);
        expectedUnhealthyInstances.addInstance(instanceId2, slaveGroup2);
        expectedUnhealthyInstances.addInstance(instanceId3, slaveGroup2);

        verify(reactorFlowManager).triggerStackRepairFlow(stackId, expectedUnhealthyInstances);
    }

    private void setupHostMetadata(Long clusterId, String privateIp, String hostGroupName) {
        HostMetadata hmd1 = mock(HostMetadata.class);
        HostGroup hg1 = mock(HostGroup.class);
        when(hg1.getName()).thenReturn(hostGroupName);
        when(hmd1.getHostGroup()).thenReturn(hg1);
        when(hostMetadataRepository.findHostInClusterByName(clusterId, privateIp)).thenReturn(hmd1);
    }

    private void setupInstanceMetadata(Long stackId, String instanceId, String privateIp) {
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        when(imd1.getDiscoveryFQDN()).thenReturn(privateIp);
        when(instanceMetaDataRepository.findByInstanceId(stackId, instanceId)).thenReturn(imd1);
    }
}
