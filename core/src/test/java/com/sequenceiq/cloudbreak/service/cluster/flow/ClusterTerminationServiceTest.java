package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.rotation.service.SharedDBRotationUtils;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class ClusterTerminationServiceTest {

    private static final boolean FORCE = true;

    private static final long STACK_ID = 1L;

    private static final String DATABASE_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    @Mock
    private ClusterService clusterService;

    @Mock
    private HostGroupService hostGroupService;

    @Resource
    private Map<FileSystemType, FileSystemConfigurator<BaseFileSystemConfigurationsView>> fileSystemConfigurators;

    @Mock
    private ContainerService containerService;

    @Mock
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    @Mock
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private SharedDBRotationUtils sharedDBRotationUtils;

    @InjectMocks
    private ClusterTerminationService underTest;

    @Test
    void finalizeDoesNotRemoveStackLink() throws TransactionExecutionException {
        Cluster cluster = prepareMockForFinalization();

        underTest.finalizeClusterTermination(STACK_ID, FORCE, StackType.WORKLOAD, "");

        assertThat(cluster.getStack()).isNotNull();
        assertThat(cluster.getStack()).isEqualTo(cluster.getStack());
    }

    @Test
    void testOrphanRdsConfigCleanupForDataLake() throws TransactionExecutionException {
        Cluster cluster = prepareMockForFinalization();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        cluster.setRdsConfigs(Set.of(mock(RDSConfig.class)));
        when(sharedDBRotationUtils.getJdbcConnectionUrl(any())).thenReturn("url");
        RDSConfig orphanRDSConfig = mock(RDSConfig.class);
        when(orphanRDSConfig.getType()).thenReturn(DatabaseType.HIVE.name());
        when(orphanRDSConfig.getClusters()).thenReturn(Set.of());
        when(rdsConfigService.findAllByConnectionUrlAndTypeWithClusters(any())).thenReturn(Set.of(orphanRDSConfig));
        doNothing().when(rdsConfigService).deleteDefaultRdsConfigs(any());

        underTest.finalizeClusterTermination(STACK_ID, FORCE, StackType.DATALAKE, "");

        verify(rdsConfigService).deleteDefaultRdsConfigs(any());
    }

    @Test
    void testOrphanRdsConfigCleanupForDataLakeIfQueryFails() throws TransactionExecutionException {
        Cluster cluster = prepareMockForFinalization();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        cluster.setRdsConfigs(Set.of(mock(RDSConfig.class)));
        when(sharedDBRotationUtils.getJdbcConnectionUrl(any())).thenReturn("url");
        when(rdsConfigService.findAllByConnectionUrlAndTypeWithClusters(any())).thenThrow(new RuntimeException("anything"));

        underTest.finalizeClusterTermination(STACK_ID, FORCE, StackType.DATALAKE, "");

        verify(rdsConfigService, never()).deleteDefaultRdsConfigs(any());
    }

    @Test
    void testHmsDbUserCleanupForDataHub() throws TransactionExecutionException {
        Cluster cluster = prepareMockForFinalization();
        RDSConfig hmsRdsConfig = mock(RDSConfig.class);
        when(hmsRdsConfig.getType()).thenReturn(DatabaseType.HIVE.name());
        when(hmsRdsConfig.getClusters()).thenReturn(Set.of(cluster));
        when(hmsRdsConfig.getConnectionUserName()).thenReturn("hive");
        cluster.setRdsConfigs(Set.of(hmsRdsConfig));
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.of(SdxBasicView.builder().withCrn("DL").build()));
        when(stackOperationService.manageDatabaseUser(any(), any(), any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        underTest.finalizeClusterTermination(STACK_ID, FORCE, StackType.WORKLOAD, "");

        verify(stackOperationService).manageDatabaseUser(eq("DL"), eq("hive"), eq("HIVE"), eq("DELETION"));
    }

    @Test
    void testHmsDbUserCleanupForDataHubifThereIsNoUniqueHMSUser() throws TransactionExecutionException {
        Cluster cluster = prepareMockForFinalization();
        RDSConfig hmsRdsConfig = mock(RDSConfig.class);
        when(hmsRdsConfig.getType()).thenReturn(DatabaseType.HIVE.name());
        when(hmsRdsConfig.getClusters()).thenReturn(Set.of(cluster, new Cluster()));
        cluster.setRdsConfigs(Set.of(hmsRdsConfig));

        underTest.finalizeClusterTermination(STACK_ID, FORCE, StackType.WORKLOAD, "");

        verify(stackOperationService, never()).manageDatabaseUser(eq("DL"), eq("hive"), eq("HIVE"), eq("DELETION"));
    }

    @Test
    void testHmsDbUserCleanupForDataHubIfDatalakeCallFails() throws TransactionExecutionException {
        Cluster cluster = prepareMockForFinalization();
        RDSConfig hmsRdsConfig = mock(RDSConfig.class);
        when(hmsRdsConfig.getType()).thenReturn(DatabaseType.HIVE.name());
        when(hmsRdsConfig.getClusters()).thenReturn(Set.of(cluster));
        when(hmsRdsConfig.getConnectionUserName()).thenReturn("hive");
        cluster.setRdsConfigs(Set.of(hmsRdsConfig));
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.of(SdxBasicView.builder().withCrn("DL").build()));
        when(stackOperationService.manageDatabaseUser(any(), any(), any(), any())).thenThrow(new RuntimeException("anything"));

        underTest.finalizeClusterTermination(STACK_ID, FORCE, StackType.WORKLOAD, "");

        verify(stackOperationService).manageDatabaseUser(eq("DL"), eq("hive"), eq("HIVE"), eq("DELETION"));
    }

    private Cluster prepareMockForFinalization() {
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        cluster.setStack(stack);
        when(clusterService.findOneWithLists(STACK_ID)).thenReturn(Optional.of(cluster));
        cluster.setId(1L);
        return cluster;
    }

}
