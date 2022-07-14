package com.sequenceiq.cloudbreak.service.stack.repair;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@RunWith(MockitoJUnitRunner.class)
public class CandidateUnhealthyInstancesSelectorTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private CandidateUnhealthyInstanceSelector undertest;

    @Captor
    private ArgumentCaptor<List<String>> hostNamesCaptor;

    private Stack stack;

    @Before
    public void setUp() {
        stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
    }

    @Test
    public void shouldSelectInstancesWithUnknownStatus() {
        Map<HostName, String> hostStatuses = new HashMap<>();
        hostStatuses.put(hostName("ip-10-0-0-1.ec2.internal"), "HEALTHY");
        hostStatuses.put(hostName("ip-10-0-0-2.ec2.internal"), "UNKNOWN");
        hostStatuses.put(hostName("ip-10-0-0-3.ec2.internal"), "HEALTHY");
        hostStatuses.put(hostName("ip-10-0-0-4.ec2.internal"), "UNKNOWN");

        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        InstanceMetaData imd2 = mock(InstanceMetaData.class);

        when(clusterService.getHostStatuses(stack.getId())).thenReturn(hostStatuses);
        when(instanceMetaDataService.findAllWorkerWithHostnamesInStack(any(), any())).thenReturn(List.of(imd1, imd2));

        Set<InstanceMetadataView> candidateUnhealthyInstances = undertest.selectCandidateUnhealthyInstances(stack.getId());

        verify(instanceMetaDataService).findAllWorkerWithHostnamesInStack(eq(stack.getId()), hostNamesCaptor.capture());
        assertTrue(hostNamesCaptor.getValue().contains("ip-10-0-0-2.ec2.internal"));
        assertTrue(hostNamesCaptor.getValue().contains("ip-10-0-0-4.ec2.internal"));
        assertEquals(2, hostNamesCaptor.getValue().size());
        assertEquals(2, candidateUnhealthyInstances.size());
        assertTrue(candidateUnhealthyInstances.contains(imd1));
        assertTrue(candidateUnhealthyInstances.contains(imd2));
    }

    @Test
    public void shouldReturnEmptyListIfAllInstancesHealthy() {
        Map<HostName, String> hostStatuses = new HashMap<>();
        hostStatuses.put(hostName("ip-10-0-0-1.ec2.internal"), "HEALTHY");
        hostStatuses.put(hostName("ip-10-0-0-3.ec2.internal"), "HEALTHY");

        when(clusterService.getHostStatuses(stack.getId())).thenReturn(hostStatuses);

        Set<InstanceMetadataView> candidateUnhealthyInstances = undertest.selectCandidateUnhealthyInstances(stack.getId());

        assertTrue(candidateUnhealthyInstances.isEmpty());
    }

    @Test
    public void shouldRemoveNonCoreGroupNodes() {
        Map<HostName, String> hostStatuses = new HashMap<>();
        hostStatuses.put(hostName("ip-10-0-0-1.ec2.internal"), "HEALTHY");
        hostStatuses.put(hostName("ip-10-0-0-2.ec2.internal"), "UNKNOWN");
        hostStatuses.put(hostName("ip-10-0-0-3.ec2.internal"), "UNKNOWN");
        hostStatuses.put(hostName("ip-10-0-0-4.ec2.internal"), "UNKNOWN");

        InstanceMetaData imd1 = mock(InstanceMetaData.class);
        InstanceMetaData imd2 = mock(InstanceMetaData.class);

        when(clusterService.getHostStatuses(stack.getId())).thenReturn(hostStatuses);
        when(instanceMetaDataService.findAllWorkerWithHostnamesInStack(any(), any())).thenReturn(List.of(imd1, imd2));

        Set<InstanceMetadataView> candidateUnhealthyInstances = undertest.selectCandidateUnhealthyInstances(stack.getId());

        verify(instanceMetaDataService).findAllWorkerWithHostnamesInStack(eq(stack.getId()), hostNamesCaptor.capture());
        assertTrue(hostNamesCaptor.getValue().contains("ip-10-0-0-2.ec2.internal"));
        assertTrue("it is not worker", hostNamesCaptor.getValue().contains("ip-10-0-0-3.ec2.internal"));
        assertTrue(hostNamesCaptor.getValue().contains("ip-10-0-0-4.ec2.internal"));
        assertEquals(3, hostNamesCaptor.getValue().size());
        assertEquals(2L, candidateUnhealthyInstances.size());
        assertTrue(candidateUnhealthyInstances.contains(imd1));
        assertTrue(candidateUnhealthyInstances.contains(imd2));
    }
}
