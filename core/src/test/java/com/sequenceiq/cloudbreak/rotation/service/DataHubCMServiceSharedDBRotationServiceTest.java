package com.sequenceiq.cloudbreak.rotation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashBasedTable;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class DataHubCMServiceSharedDBRotationServiceTest {

    @Mock
    private StackDtoService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private SharedDBRotationUtils sharedDBRotationUtils;

    @InjectMocks
    private DataHubCMServiceSharedDBRotationService underTest;

    @Test
    void testValidations() {
        StackDto stack = mock(StackDto.class);
        StackDto dlStack = mock(StackDto.class);
        when(dlStack.isAvailable()).thenReturn(Boolean.FALSE);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any()))
                .thenReturn(Optional.of(SdxBasicView.builder().withPlatform(TargetPlatform.CDL).build()))
                .thenReturn(Optional.of(SdxBasicView.builder().withCrn("DL").build()));
        when(stackService.getByCrn(eq("DL"))).thenReturn(dlStack);

        assertThrows(SecretRotationException.class, () -> underTest.rotateSharedServiceDbSecretOnDataHub(stack),
                "Shared service DB user/password rotation for Data Hub connected to a CDL is not supported yet!");
        assertThrows(SecretRotationException.class, () -> underTest.rotateSharedServiceDbSecretOnDataHub(stack),
                "Data Lake is not available, which is a requirement for shared service DB user/password rotation!");
    }

    @Test
    void testRotationIfDataLakeHmsUserIsUsedByDataHub() throws Exception {
        StackDto stack = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(2L);
        when(stack.getCluster()).thenReturn(cluster);

        StackDto dlStack = mock(StackDto.class);
        Cluster dlCluster = mock(Cluster.class);
        when(dlCluster.getId()).thenReturn(1L);
        when(dlStack.getCluster()).thenReturn(dlCluster);
        when(dlStack.isAvailable()).thenReturn(Boolean.TRUE);

        ClusterModificationService clusterModService = mock(ClusterModificationService.class);
        mockIndependentCalls(dlStack, clusterModService);
        RDSConfig dlRdsConfig = mock(RDSConfig.class);
        when(dlRdsConfig.getClusters()).thenReturn(Set.of(dlCluster, cluster));
        RDSConfig pooledRdsConfig = mock(RDSConfig.class);
        when(pooledRdsConfig.getClusters()).thenReturn(Set.of());
        when(rdsConfigService.findAllByConnectionUrlAndTypeWithClusters(any()))
                .thenReturn(Set.of(dlRdsConfig))
                .thenReturn(Set.of(dlRdsConfig, pooledRdsConfig));
        when(clusterService.getByIdWithLists(eq(2L))).thenReturn(cluster);
        when(clusterService.save(eq(cluster))).thenReturn(cluster);

        underTest.rotateSharedServiceDbSecretOnDataHub(stack);

        verify(stackOperationService).manageDatabaseUser(any(), any(), any(), eq(ExternalDatabaseUserOperation.CREATION.name()));
        verify(clusterModService).updateConfig(any(), any());
        verify(stackOperationService, never()).manageDatabaseUser(any(), any(), any(), eq(ExternalDatabaseUserOperation.DELETION.name()));
    }

    @Test
    void testRotationIfOwnHmsUserIsUsedByDataHub() throws Exception {
        StackDto stack = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(2L);
        when(stack.getCluster()).thenReturn(cluster);

        StackDto dlStack = mock(StackDto.class);
        Cluster dlCluster = mock(Cluster.class);
        when(dlStack.isAvailable()).thenReturn(Boolean.TRUE);

        ClusterModificationService clusterModService = mock(ClusterModificationService.class);
        mockIndependentCalls(dlStack, clusterModService);
        RDSConfig dlRdsConfig = mock(RDSConfig.class);
        when(dlRdsConfig.getClusters()).thenReturn(Set.of(dlCluster));
        RDSConfig dhRdsConfig = mock(RDSConfig.class);
        when(dhRdsConfig.getClusters()).thenReturn(Set.of(cluster));
        RDSConfig pooledRdsConfig = mock(RDSConfig.class);
        when(pooledRdsConfig.getClusters()).thenReturn(Set.of());
        when(rdsConfigService.findAllByConnectionUrlAndTypeWithClusters(any()))
                .thenReturn(Set.of(dlRdsConfig, dhRdsConfig))
                .thenReturn(Set.of(dlRdsConfig, dhRdsConfig, pooledRdsConfig));
        when(clusterService.getByIdWithLists(eq(2L))).thenReturn(cluster);
        when(clusterService.save(eq(cluster))).thenReturn(cluster);

        underTest.rotateSharedServiceDbSecretOnDataHub(stack);

        verify(stackOperationService).manageDatabaseUser(any(), any(), any(), eq(ExternalDatabaseUserOperation.CREATION.name()));
        verify(clusterModService).updateConfig(any(), any());
        verify(stackOperationService).manageDatabaseUser(any(), any(), any(), eq(ExternalDatabaseUserOperation.DELETION.name()));
    }

    private void mockIndependentCalls(StackDto dlStack, ClusterModificationService clusterModService) throws Exception {
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any()))
                .thenReturn(Optional.of(SdxBasicView.builder().withCrn("DL").withDbServerCrn("DB").build()));
        when(stackService.getByCrn(eq("DL"))).thenReturn(dlStack);
        when(sharedDBRotationUtils.getJdbcConnectionUrl(any())).thenReturn("whatever");
        when(sharedDBRotationUtils.getNewDatabaseUserName(any())).thenReturn("again_whatever");
        when(stackOperationService.manageDatabaseUser(any(), any(), any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));
        doNothing().when(sharedDBRotationUtils).pollFlow(any());
        when(sharedDBRotationUtils.getConfigTableForRotationInCM(any())).thenReturn(HashBasedTable.create());
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModService);
        doNothing().when(clusterModService).updateConfig(any(), any());
    }
}
