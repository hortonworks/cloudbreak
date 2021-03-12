package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import com.sequenceiq.cloudbreak.service.cluster.FinalizeClusterInstallHandlerService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCreationSuccessHandlerTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private final FinalizeClusterInstallHandlerService underTest = new FinalizeClusterInstallHandlerService();

    @Test
    public void testFinalizeClusterInstallWhenEverythingGoesFine() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();

        Set<InstanceMetaData> instanceMetaDataList = new HashSet<>();
        stack.getInstanceGroups().forEach(instanceGroup -> instanceMetaDataList.addAll(instanceGroup.getAllInstanceMetaData()));

        when(clusterService.updateCluster(cluster)).thenReturn(cluster);
        when(instanceMetaDataService.saveAll(anyCollection())).thenReturn(instanceMetaDataList);

        underTest.finalizeClusterInstall(instanceMetaDataList, cluster);

        ArgumentCaptor<Cluster> clusterCaptor = ArgumentCaptor.forClass(Cluster.class);
        verify(clusterService, times(1)).updateCluster(clusterCaptor.capture());
        assertNotNull(clusterCaptor.getValue().getCreationFinished());
        assertNotNull(clusterCaptor.getValue().getUpSince());
    }
}