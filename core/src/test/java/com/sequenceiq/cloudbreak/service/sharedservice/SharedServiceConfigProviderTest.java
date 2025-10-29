package com.sequenceiq.cloudbreak.service.sharedservice;

import static com.sequenceiq.cloudbreak.sdx.TargetPlatform.PAAS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxFileSystemView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class SharedServiceConfigProviderTest {

    @Mock
    private RemoteDataContextWorkaroundService remoteDataContextWorkaroundService;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private ClusterService clusterService;

    @Mock
    private SecretService secretService;

    @Mock
    private DatabaseCommon dbCommon;

    @InjectMocks
    private SharedServiceConfigProvider underTest;

    @Test
    void testConfigureForPaasDatalake() {
        Cluster cluster = cluster();
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(SdxBasicView.builder().withPlatform(PAAS).withFileSystemView(new SdxFileSystemView(null, null)).build()));
        Stack dlStack = new Stack();
        Cluster dlCluster = new Cluster();
        dlCluster.setId(1L);
        dlStack.setCluster(dlCluster);
        when(stackService.getByCrn(any())).thenReturn(dlStack);
        when(remoteDataContextWorkaroundService.prepareFilesystem(any(), any(), any())).thenReturn(new FileSystem());
        RdsConfigWithoutCluster rdsConfigWithoutCluster = mock(RdsConfigWithoutCluster.class);
        when(rdsConfigWithoutCluster.getId()).thenReturn(1L);
        when(rdsConfigWithoutClusterService.findByClusterIdAndStatusInAndTypeIn(any(), any(), any())).thenReturn(List.of(rdsConfigWithoutCluster));

        underTest.configureCluster(cluster);

        verify(remoteDataContextWorkaroundService).prepareFilesystem(any(), any(), any());
        verify(platformAwareSdxConnector, never()).getHmsServiceConfig(any());
        assertEquals(cluster.getRdsConfigs().size(), 1);
    }

    private Cluster cluster() {
        Cluster requestedCluster = new Cluster();
        requestedCluster.setRdsConfigs(new LinkedHashSet<>());
        requestedCluster.setBlueprint(new Blueprint());
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        requestedCluster.setStack(stack);
        return requestedCluster;
    }
}