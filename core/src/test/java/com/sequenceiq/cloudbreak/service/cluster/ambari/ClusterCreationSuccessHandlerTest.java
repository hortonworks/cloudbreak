package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.FinalizeClusterInstallHandlerService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClusterCreationSuccessHandlerTest {

    @Mock
    private ClusterService clusterService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private final FinalizeClusterInstallHandlerService underTest = new FinalizeClusterInstallHandlerService();

    @Test
    void testFinalizeClusterInstallWhenEverythingGoesFine() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster();

        List<InstanceMetadataView> instanceMetaDataList = new ArrayList<>();
        stack.getInstanceGroups().forEach(instanceGroup -> instanceMetaDataList.addAll(instanceGroup.getAllInstanceMetaData()));
        List<Long> metadataIds = instanceMetaDataList.stream().map(InstanceMetadataView::getId).collect(Collectors.toList());

        underTest.finalizeClusterInstall(instanceMetaDataList, cluster);

        verify(instanceMetaDataService).updateAllInstancesToStatus(metadataIds, InstanceStatus.SERVICES_HEALTHY,
                "Cluster install finalized, services are healthy");
        verify(clusterService, times(1)).updateCreationFinishedAndUpSinceToNowByClusterId(cluster.getId());
    }
}