package com.sequenceiq.cloudbreak.service.stack.repair;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
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
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class StackRepairServiceTest {

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private ExecutorService executorService;

    @Mock
    private FlowMessageService flowMessageService;

    @InjectMocks
    private StackRepairService stackRepairService;

    @Test
    public void shouldIgnoreIfNoInstancesToRepair() {
        long stackId = 1L;
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(stackId);
        StackRepairNotificationRequest stackRepairNotificationRequest = new StackRepairNotificationRequest(stack, Collections.EMPTY_SET);
        stackRepairService.add(stackRepairNotificationRequest);
        verifyZeroInteractions(executorService);
        verify(flowMessageService).fireEventAndLog(stackId, Msg.STACK_REPAIR_COMPLETE_CLEAN, Status.AVAILABLE.name());
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

        verify(executorService).submit(argThat(new StackRepairFlowSubmitterMatcher(stackId, expectedUnhealthyInstances)));
        verify(flowMessageService).fireEventAndLog(stackId, Msg.STACK_REPAIR_ATTEMPTING, Status.UPDATE_IN_PROGRESS.name());
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

    private class StackRepairFlowSubmitterMatcher extends ArgumentMatcher<StackRepairService.StackRepairFlowSubmitter> {

        private final Long expectedStackId;

        private final UnhealthyInstances expectedInstances;

        StackRepairFlowSubmitterMatcher(Long expectedStackId, UnhealthyInstances expectedInstances) {
            this.expectedStackId = expectedStackId;
            this.expectedInstances = expectedInstances;
        }

        @Override
        public boolean matches(Object argument) {
            StackRepairService.StackRepairFlowSubmitter stackRepairFlowSubmitter =
                    (StackRepairService.StackRepairFlowSubmitter) argument;
            return (stackRepairFlowSubmitter.getStackId() == expectedStackId)
                    && (expectedInstances.equals(stackRepairFlowSubmitter.getUnhealthyInstances()));
        }
    }
}
