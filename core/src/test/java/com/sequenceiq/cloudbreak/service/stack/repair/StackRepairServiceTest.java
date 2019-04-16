package com.sequenceiq.cloudbreak.service.stack.repair;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.repair.StackRepairService.StackRepairFlowSubmitter;

@RunWith(MockitoJUnitRunner.class)
public class StackRepairServiceTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private HostMetadataService hostMetadataService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

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
        underTest.add(stack, Collections.emptySet());

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
        when(hostMetadataService.findHostInClusterByName(clusterId, privateIp)).thenReturn(Optional.of(hmd1));
    }

    private void setupInstanceMetadata(Long stackId, String instanceId, String privateIp) {
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        when(imd1.getDiscoveryFQDN()).thenReturn(privateIp);
        when(instanceMetaDataService.findByStackIdAndInstanceId(stackId, instanceId)).thenReturn(Optional.of(imd1));
    }

    private static class StackRepairFlowSubmitterMatcher implements ArgumentMatcher<StackRepairFlowSubmitter> {

        private final Long expectedStackId;

        private final UnhealthyInstances expectedInstances;

        private StackRepairFlowSubmitterMatcher(Long expectedStackId, UnhealthyInstances expectedInstances) {
            this.expectedStackId = expectedStackId;
            this.expectedInstances = expectedInstances;
        }

        @Override
        public boolean matches(StackRepairFlowSubmitter argument) {
            StackRepairFlowSubmitter stackRepairFlowSubmitter = argument;
            return stackRepairFlowSubmitter.getStackId().equals(expectedStackId) && expectedInstances.equals(stackRepairFlowSubmitter.getUnhealthyInstances());
        }
    }
}
