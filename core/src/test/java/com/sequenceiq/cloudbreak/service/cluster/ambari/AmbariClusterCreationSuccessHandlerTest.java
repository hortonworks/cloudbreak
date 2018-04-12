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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterCreationSuccessHandlerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @InjectMocks
    private AmbariClusterCreationSuccessHandler underTest = new AmbariClusterCreationSuccessHandler();

    @Test
    public void testHandleClusterCreationSuccessWhenEverythingGoesFine() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();

        Set<HostMetadata> hostMetadataList = new HashSet<>();
        cluster.getHostGroups().forEach(hostGroup -> hostGroup.getHostMetadata().forEach(hostMetadataList::add));
        List<InstanceMetaData> instanceMetaDataList = new ArrayList<>();
        stack.getInstanceGroups().forEach(instanceGroup -> instanceGroup.getInstanceMetaData().forEach(instanceMetaDataList::add));

        when(clusterService.updateCluster(cluster)).thenReturn(cluster);
        when(instanceMetadataRepository.save(anyCollection())).thenReturn(instanceMetaDataList);
        when(hostMetadataRepository.findHostsInCluster(cluster.getId())).thenReturn(hostMetadataList);
        when(hostMetadataRepository.save(anyCollection())).thenReturn(hostMetadataList);

        underTest.handleClusterCreationSuccess(stack, cluster);

        ArgumentCaptor<Cluster> clusterCaptor = ArgumentCaptor.forClass(Cluster.class);
        verify(clusterService, times(1)).updateCluster(clusterCaptor.capture());
        assertNotNull(clusterCaptor.getValue().getCreationFinished());
        assertNotNull(clusterCaptor.getValue().getUpSince());

        ArgumentCaptor<List> instanceMetadataCaptor = ArgumentCaptor.forClass(List.class);
        verify(instanceMetadataRepository, times(1)).save(instanceMetadataCaptor.capture());
        for (InstanceMetaData instanceMetaData : (List<InstanceMetaData>) instanceMetadataCaptor.getValue()) {
            Assert.assertEquals(InstanceStatus.REGISTERED, instanceMetaData.getInstanceStatus());
        }

        ArgumentCaptor<List> hostMetadataCaptor = ArgumentCaptor.forClass(List.class);
        verify(hostMetadataRepository, times(1)).save(hostMetadataCaptor.capture());
        for (HostMetadata hostMetadata : (List<HostMetadata>) hostMetadataCaptor.getValue()) {
            Assert.assertEquals(HostMetadataState.HEALTHY, hostMetadata.getHostMetadataState());
        }

        verify(hostMetadataRepository, times(1)).findHostsInCluster(cluster.getId());
    }


}