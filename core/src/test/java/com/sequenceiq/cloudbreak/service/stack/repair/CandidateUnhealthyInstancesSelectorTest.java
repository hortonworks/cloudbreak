package com.sequenceiq.cloudbreak.service.stack.repair;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CandidateUnhealthyInstancesSelectorTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @InjectMocks
    private CandidateUnhealthyInstanceSelector candidateUnhealthyInstanceSelector;

    @Test
    public void shouldSelectInstancesWithUnknownStatus() throws CloudbreakSecuritySetupException {
        Stack stack = mock(Stack.class);
        long stackId = 1L;
        when(stack.getId()).thenReturn(stackId);
        Map<String, String> hostStatuses = new HashMap<>();
        hostStatuses.put("ip-10-0-0-1.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-2.ec2.internal", "UNKNOWN");
        hostStatuses.put("ip-10-0-0-3.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-4.ec2.internal", "UNKNOWN");

        when(clusterService.getHostStatuses(stackId)).thenReturn(hostStatuses);
        InstanceGroup slaveGroup = setupInstanceGroup(InstanceGroupType.CORE);

        InstanceMetaData imd1 = setupInstanceMetaData(stackId, "ip-10-0-0-2.ec2.internal", slaveGroup);
        InstanceMetaData imd2 = setupInstanceMetaData(stackId, "ip-10-0-0-4.ec2.internal", slaveGroup);

        Set<InstanceMetaData> candidateUnhealthyInstances = candidateUnhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack);

        assertEquals(2, candidateUnhealthyInstances.size());
        assertTrue(candidateUnhealthyInstances.contains(imd1));
        assertTrue(candidateUnhealthyInstances.contains(imd2));
    }

    @Test
    public void shouldReturnEmptyListIfAllInstancesHealthy() throws CloudbreakSecuritySetupException {
        Stack stack = mock(Stack.class);
        long stackId = 1L;
        when(stack.getId()).thenReturn(stackId);
        Map<String, String> hostStatuses = new HashMap<>();
        hostStatuses.put("ip-10-0-0-1.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-3.ec2.internal", "HEALTHY");

        when(clusterService.getHostStatuses(stackId)).thenReturn(hostStatuses);


        Set<InstanceMetaData> candidateUnhealthyInstances = candidateUnhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack);

        assertTrue(candidateUnhealthyInstances.isEmpty());
    }

    @Test
    public void shouldRemoveNonCoreGroupNodes() throws CloudbreakSecuritySetupException {
        Stack stack = mock(Stack.class);
        long stackId = 1L;
        when(stack.getId()).thenReturn(stackId);
        Map<String, String> hostStatuses = new HashMap<>();
        hostStatuses.put("ip-10-0-0-1.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-2.ec2.internal", "UNKNOWN");
        hostStatuses.put("ip-10-0-0-3.ec2.internal", "UNKNOWN");
        hostStatuses.put("ip-10-0-0-4.ec2.internal", "UNKNOWN");

        when(clusterService.getHostStatuses(stackId)).thenReturn(hostStatuses);

        InstanceGroup slaveGroup = setupInstanceGroup(InstanceGroupType.CORE);
        InstanceGroup gatewayGroup = setupInstanceGroup(InstanceGroupType.GATEWAY);

        InstanceMetaData imd1 = setupInstanceMetaData(stackId, "ip-10-0-0-2.ec2.internal", slaveGroup);
        InstanceMetaData imd2 = setupInstanceMetaData(stackId, "ip-10-0-0-4.ec2.internal", slaveGroup);
        InstanceMetaData imd3 = setupInstanceMetaData(stackId, "ip-10-0-0-3.ec2.internal", gatewayGroup);

        Set<InstanceMetaData> candidateUnhealthyInstances = candidateUnhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack);

        assertEquals(2, candidateUnhealthyInstances.size());
        assertTrue(candidateUnhealthyInstances.contains(imd1));
        assertTrue(candidateUnhealthyInstances.contains(imd2));
    }

    private InstanceGroup setupInstanceGroup(InstanceGroupType instanceGroupType) {
        InstanceGroup slaveGroup = mock(InstanceGroup.class);
        when(slaveGroup.getInstanceGroupType()).thenReturn(instanceGroupType);
        return slaveGroup;
    }

    private InstanceMetaData setupInstanceMetaData(long stackId, String privateIp, InstanceGroup group) {
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        when(imd1.getInstanceGroup()).thenReturn(group);
        when(instanceMetaDataRepository.findHostInStack(stackId, privateIp)).thenReturn(imd1);
        return imd1;
    }
}
