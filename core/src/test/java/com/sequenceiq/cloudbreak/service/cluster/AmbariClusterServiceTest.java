package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private BlueprintValidator blueprintValidator;

    @Mock
    private ClusterTerminationService clusterTerminationService;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ReactorFlowManager flowManager;

    @InjectMocks
    private AmbariClusterService ambariClusterService;

    @Before
    public void setup() throws CloudbreakException {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setBlueprint(new Blueprint());
        cluster.getBlueprint().setId(1L);
        Stack stack = new Stack();
        stack.setOrchestrator(new Orchestrator());
        stack.setCluster(cluster);
        when(clusterRepository.findById(any(Long.class))).thenReturn(cluster);
        when(stackService.getByIdWithLists(any(Long.class))).thenReturn(stack);
        when(orchestratorTypeResolver.resolveType(nullable(Orchestrator.class))).thenReturn(OrchestratorType.HOST);
        when(orchestratorTypeResolver.resolveType(nullable(String.class))).thenReturn(OrchestratorType.HOST);
        when(clusterComponentConfigProvider.getHDPRepo(any(Long.class))).thenReturn(new StackRepoDetails());
        when(clusterComponentConfigProvider.store(any(ClusterComponent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clusterComponentConfigProvider.getComponent(any(Long.class), any(ComponentType.class))).thenReturn(new ClusterComponent());
        when(blueprintService.get(any(Long.class))).thenReturn(cluster.getBlueprint());
    }

    @Test(expected = BadRequestException.class)
    public void testRecreateFailNotEmbeddedDb() {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES.name());
        when(rdsConfigService.findByClusterIdAndType(nullable(String.class), nullable(String.class), any(Long.class), eq(RdsType.AMBARI))).thenReturn(rdsConfig);
        ambariClusterService.recreate(1L, 1L, new HashSet<>(), false, new StackRepoDetails(), null, null);
    }

    @Test
    public void testRecreateSuccess() {
        ambariClusterService.recreate(1L, 1L, new HashSet<>(), false, new StackRepoDetails(), null, null);
    }
}