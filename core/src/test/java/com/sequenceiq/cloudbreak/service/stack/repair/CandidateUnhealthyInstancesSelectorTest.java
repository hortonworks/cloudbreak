package com.sequenceiq.cloudbreak.service.stack.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@RunWith(MockitoJUnitRunner.class)
public class CandidateUnhealthyInstancesSelectorTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private CandidateUnhealthyInstanceSelector undertest;

    private Stack stack;

    @Before
    public void setUp() {
        stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
    }

    @Test
    public void shouldSelectInstancesWithUnknownStatus() {
        Map<String, String> hostStatuses = new HashMap<>();
        hostStatuses.put("ip-10-0-0-1.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-2.ec2.internal", "UNKNOWN");
        hostStatuses.put("ip-10-0-0-3.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-4.ec2.internal", "UNKNOWN");

        when(clusterService.getHostStatuses(stack.getId())).thenReturn(hostStatuses);
        InstanceGroup slaveGroup = setupInstanceGroup(InstanceGroupType.CORE);

        InstanceMetaData imd1 = setupInstanceMetaData(stack.getId(), "ip-10-0-0-2.ec2.internal", slaveGroup);
        InstanceMetaData imd2 = setupInstanceMetaData(stack.getId(), "ip-10-0-0-4.ec2.internal", slaveGroup);

        Set<InstanceMetaData> candidateUnhealthyInstances = undertest.selectCandidateUnhealthyInstances(stack.getId());

        assertEquals(2, candidateUnhealthyInstances.size());
        assertTrue(candidateUnhealthyInstances.contains(imd1));
        assertTrue(candidateUnhealthyInstances.contains(imd2));
    }

    @Test
    public void shouldReturnEmptyListIfAllInstancesHealthy() {
        Map<String, String> hostStatuses = new HashMap<>();
        hostStatuses.put("ip-10-0-0-1.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-3.ec2.internal", "HEALTHY");

        when(clusterService.getHostStatuses(stack.getId())).thenReturn(hostStatuses);

        Set<InstanceMetaData> candidateUnhealthyInstances = undertest.selectCandidateUnhealthyInstances(stack.getId());

        assertTrue(candidateUnhealthyInstances.isEmpty());
    }

    @Test
    public void shouldRemoveNonCoreGroupNodes() {
        Map<String, String> hostStatuses = new HashMap<>();
        hostStatuses.put("ip-10-0-0-1.ec2.internal", "HEALTHY");
        hostStatuses.put("ip-10-0-0-2.ec2.internal", "UNKNOWN");
        hostStatuses.put("ip-10-0-0-3.ec2.internal", "UNKNOWN");
        hostStatuses.put("ip-10-0-0-4.ec2.internal", "UNKNOWN");

        when(clusterService.getHostStatuses(stack.getId())).thenReturn(hostStatuses);

        InstanceGroup slaveGroup = setupInstanceGroup(InstanceGroupType.CORE);
        InstanceGroup gatewayGroup = setupInstanceGroup(InstanceGroupType.GATEWAY);

        InstanceMetaData imd1 = setupInstanceMetaData(stack.getId(), "ip-10-0-0-2.ec2.internal", slaveGroup);
        InstanceMetaData imd2 = setupInstanceMetaData(stack.getId(), "ip-10-0-0-4.ec2.internal", slaveGroup);
        setupInstanceMetaData(stack.getId(), "ip-10-0-0-3.ec2.internal", gatewayGroup);

        Set<InstanceMetaData> candidateUnhealthyInstances = undertest.selectCandidateUnhealthyInstances(stack.getId());

        assertEquals(2L, candidateUnhealthyInstances.size());
        assertTrue(candidateUnhealthyInstances.contains(imd1));
        assertTrue(candidateUnhealthyInstances.contains(imd2));
    }

    private InstanceGroup setupInstanceGroup(InstanceGroupType instanceGroupType) {
        InstanceGroup slaveGroup = mock(InstanceGroup.class);
        when(slaveGroup.getInstanceGroupType()).thenReturn(instanceGroupType);
        return slaveGroup;
    }

    private InstanceMetaData setupInstanceMetaData(Long stackId, String privateIp, InstanceGroup group) {
        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        when(imd1.getInstanceGroup()).thenReturn(group);
        when(instanceMetaDataService.findHostInStack(stackId, privateIp)).thenReturn(Optional.of(imd1));
        return imd1;
    }
}
