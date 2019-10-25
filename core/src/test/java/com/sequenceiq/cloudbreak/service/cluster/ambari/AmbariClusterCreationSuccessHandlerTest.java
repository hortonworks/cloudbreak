package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterCreationSuccessHandlerTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataRepository instanceMetadataRepository;

    @InjectMocks
    private final AmbariClusterCreationSuccessHandler underTest = new AmbariClusterCreationSuccessHandler();

    @Test
    public void testHandleClusterCreationSuccessWhenEverythingGoesFine() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();

        Set<InstanceMetaData> instanceMetaDataList = new HashSet<>();
        stack.getInstanceGroups().forEach(instanceGroup -> instanceMetaDataList.addAll(instanceGroup.getAllInstanceMetaData()));

        when(clusterService.updateCluster(cluster)).thenReturn(cluster);
        when(instanceMetadataRepository.saveAll(anyCollection())).thenReturn(instanceMetaDataList);

        underTest.handleClusterCreationSuccess(instanceMetaDataList, cluster);

        ArgumentCaptor<Cluster> clusterCaptor = ArgumentCaptor.forClass(Cluster.class);
        verify(clusterService, times(1)).updateCluster(clusterCaptor.capture());
        assertNotNull(clusterCaptor.getValue().getCreationFinished());
        assertNotNull(clusterCaptor.getValue().getUpSince());
    }
}