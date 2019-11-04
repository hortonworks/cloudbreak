package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterCreationSuccessHandlerTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @InjectMocks
    private final AmbariClusterCreationSuccessHandler underTest = new AmbariClusterCreationSuccessHandler();

    @Test
    public void testHandleClusterCreationSuccessWhenEverythingGoesFine() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();

        Set<HostMetadata> hostMetadataList = new HashSet<>();
        cluster.getHostGroups().forEach(hostGroup -> hostMetadataList.addAll(hostGroup.getHostMetadata()));
        List<InstanceMetaData> instanceMetaDataList = new ArrayList<>();
        stack.getInstanceGroups().forEach(instanceGroup -> instanceMetaDataList.addAll(instanceGroup.getAllInstanceMetaData()));

        when(clusterService.updateCluster(cluster)).thenReturn(cluster);
        when(instanceMetadataRepository.saveAll(anyCollection())).thenReturn(instanceMetaDataList);
        when(hostMetadataRepository.findHostsInCluster(cluster.getId())).thenReturn(hostMetadataList);
        when(hostMetadataRepository.saveAll(anyCollection())).thenReturn(hostMetadataList);

        underTest.handleClusterCreationSuccess(stack, cluster);

        ArgumentCaptor<Cluster> clusterCaptor = ArgumentCaptor.forClass(Cluster.class);
        verify(clusterService, times(1)).updateCluster(clusterCaptor.capture());
        assertNotNull(clusterCaptor.getValue().getCreationFinished());
        assertNotNull(clusterCaptor.getValue().getUpSince());

        ArgumentCaptor<List<InstanceMetaData>> instanceMetadataCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceMetadataRepository, times(1)).saveAll(instanceMetadataCaptor.capture());
        for (InstanceMetaData instanceMetaData : instanceMetadataCaptor.getValue()) {
            Assert.assertEquals(InstanceStatus.REGISTERED, instanceMetaData.getInstanceStatus());
        }

        ArgumentCaptor<List<HostMetadata>> hostMetadataCaptor = ArgumentCaptor.forClass(List.class);
        verify(hostMetadataRepository, times(1)).saveAll(hostMetadataCaptor.capture());
        for (HostMetadata hostMetadata : hostMetadataCaptor.getValue()) {
            Assert.assertEquals(HostMetadataState.HEALTHY, hostMetadata.getHostMetadataState());
        }

        verify(hostMetadataRepository, times(1)).findHostsInCluster(cluster.getId());
    }
}