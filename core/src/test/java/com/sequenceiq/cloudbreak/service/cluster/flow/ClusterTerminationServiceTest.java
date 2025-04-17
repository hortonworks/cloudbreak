package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ContainerOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurator;
import com.sequenceiq.common.model.FileSystemType;

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

    @InjectMocks
    private ClusterTerminationService underTest;

    @Test
    void finalizeDoesNotRemoveStackLink() throws TransactionExecutionException {
        Cluster cluster = prepareMockForFinalization();

        underTest.finalizeClusterTermination(STACK_ID, FORCE);

        assertThat(cluster.getStack()).isNotNull();
        assertThat(cluster.getStack()).isEqualTo(cluster.getStack());
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
