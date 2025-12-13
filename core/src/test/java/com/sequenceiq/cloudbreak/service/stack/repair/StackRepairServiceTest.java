package com.sequenceiq.cloudbreak.service.stack.repair;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REPAIR_ATTEMPTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_REPAIR_COMPLETE_CLEAN;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.repair.StackRepairService.StackRepairFlowSubmitter;

@ExtendWith(MockitoExtension.class)
class StackRepairServiceTest {

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @InjectMocks
    private StackRepairService underTest;

    private Stack stack;

    @BeforeEach
    public void setUp() {
        stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
    }

    @Test
    void shouldIgnoreIfNoInstancesToRepair() {
        underTest.add(stack, Collections.emptySet());

        verifyNoInteractions(executorService);
        verify(flowMessageService).fireEventAndLog(stack.getId(), Status.AVAILABLE.name(), STACK_REPAIR_COMPLETE_CLEAN);
    }

    @Test
    void shouldGroupUnhealthyInstancesByHostGroup() {
        String instanceId1 = "i-0f1e0605506aaaaaa";
        String instanceId2 = "i-0f1e0605506bbbbbb";
        String instanceId3 = "i-0f1e0605506cccccc";

        String privateIp1 = "ip-10-0-0-1.ec2.internal";
        String privateIp2 = "ip-10-0-0-2.ec2.internal";
        String privateIp3 = "ip-10-0-0-3.ec2.internal";

        String slaveGroup1 = "slave_group1";
        String slaveGroup2 = "slave_group2";

        InstanceGroup slaveInstanceGroup1 = new InstanceGroup();
        slaveInstanceGroup1.setGroupName(slaveGroup1);

        InstanceGroup slaveInstanceGroup2 = new InstanceGroup();
        slaveInstanceGroup2.setGroupName(slaveGroup2);

        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(instanceId1);
        instanceIds.add(instanceId2);
        instanceIds.add(instanceId3);

        setupInstanceMetadata(stack.getId(), instanceId1, privateIp1, slaveInstanceGroup1);
        setupInstanceMetadata(stack.getId(), instanceId2, privateIp2, slaveInstanceGroup2);
        setupInstanceMetadata(stack.getId(), instanceId3, privateIp3, slaveInstanceGroup2);

        underTest.add(stack, instanceIds);

        UnhealthyInstances expectedUnhealthyInstances = new UnhealthyInstances();
        expectedUnhealthyInstances.addInstance(instanceId1, slaveGroup1);
        expectedUnhealthyInstances.addInstance(instanceId2, slaveGroup2);
        expectedUnhealthyInstances.addInstance(instanceId3, slaveGroup2);

        verify(executorService).submit(argThat(new StackRepairFlowSubmitterMatcher(stack.getId(), expectedUnhealthyInstances)));
        verify(flowMessageService).fireEventAndLog(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), STACK_REPAIR_ATTEMPTING);
    }

    private void setupInstanceMetadata(Long stackId, String instanceId, String privateIp, InstanceGroup instanceGroup) {
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        imd1.setInstanceId(instanceId);
        imd1.setPrivateIp(privateIp);
        when(imd1.getInstanceGroup()).thenReturn(instanceGroup);
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
