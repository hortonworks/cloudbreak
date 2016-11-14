package com.sequenceiq.cloudbreak.service.stack.repair;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

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
    private StackRepairService underTest;

    private Stack stack;

    private Cluster cluster;

    @Before
    public void setUp() {
        stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        cluster = mock(Cluster.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(2L);
    }

    @Test
    public void shouldIgnoreIfNoInstancesToRepair() {
        underTest.add(stack, Collections.EMPTY_SET);

        verifyZeroInteractions(executorService);
        verify(flowMessageService).fireEventAndLog(stack.getId(), Msg.STACK_REPAIR_COMPLETE_CLEAN, Status.AVAILABLE.name());
    }

    @Test
    public void shouldGroupUnhealthyInstancesByHostGroup() {
        String instanceId1 = "i-0f1e0605506aaaaaa";
        String instanceId2 = "i-0f1e0605506bbbbbb";
        String instanceId3 = "i-0f1e0605506cccccc";

        String privateIp1 = "ip-10-0-0-1.ec2.internal";
        String privateIp2 = "ip-10-0-0-2.ec2.internal";
        String privateIp3 = "ip-10-0-0-3.ec2.internal";

        setupInstanceMetadata(stack.getId(), instanceId1, privateIp1);
        setupInstanceMetadata(stack.getId(), instanceId2, privateIp2);
        setupInstanceMetadata(stack.getId(), instanceId3, privateIp3);

        String slaveGroup1 = "slave_group1";
        String slaveGroup2 = "slave_group2";

        setupHostMetadata(cluster.getId(), privateIp1, slaveGroup1);
        setupHostMetadata(cluster.getId(), privateIp2, slaveGroup2);
        setupHostMetadata(cluster.getId(), privateIp3, slaveGroup2);

        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(instanceId1);
        instanceIds.add(instanceId2);
        instanceIds.add(instanceId3);

        underTest.add(stack, instanceIds);

        UnhealthyInstances expectedUnhealthyInstances = new UnhealthyInstances();
        expectedUnhealthyInstances.addInstance(instanceId1, slaveGroup1);
        expectedUnhealthyInstances.addInstance(instanceId2, slaveGroup2);
        expectedUnhealthyInstances.addInstance(instanceId3, slaveGroup2);

        verify(executorService).submit(argThat(new StackRepairFlowSubmitterMatcher(stack.getId(), expectedUnhealthyInstances)));
        verify(flowMessageService).fireEventAndLog(stack.getId(), Msg.STACK_REPAIR_ATTEMPTING, Status.UPDATE_IN_PROGRESS.name());
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
            StackRepairService.StackRepairFlowSubmitter stackRepairFlowSubmitter = (StackRepairService.StackRepairFlowSubmitter) argument;
            return stackRepairFlowSubmitter.getStackId().equals(expectedStackId) && expectedInstances.equals(stackRepairFlowSubmitter.getUnhealthyInstances());
        }
    }
}
