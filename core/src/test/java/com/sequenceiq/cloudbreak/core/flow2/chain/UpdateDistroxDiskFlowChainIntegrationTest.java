package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerDefaultConfigAdjuster;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.config.DistroXDiskUpdateFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler.DistroXDiskUpdateHandler;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler.DistroXDiskUpdateValidationHandler;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXDiskUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.FlowIntegrationTestConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.reactor.handler.kerberos.KeytabConfigurationHandler;
import com.sequenceiq.cloudbreak.reactor.handler.kerberos.KeytabProvider;
import com.sequenceiq.cloudbreak.reactor.handler.orchestration.BootstrapMachineHandler;
import com.sequenceiq.cloudbreak.reactor.handler.orchestration.StartAmbariServicesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.recipe.UploadRecipesHandler;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.VerticalScalingValidatorService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListenerAdapter;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Boots the real {@link UpdateDistroxDiskFlowEventChainFactory} chain and drives it end to end on the non-GCP (AWS) path:
 * {@code FlowChainInit -> SaltUpdate -> DistroXDiskUpdate -> DiskResize -> FlowChainFinalize}. It verifies the chain is
 * actually wired together across the three sub-flows - that SaltUpdate hands off to the disk type/size update, which hands
 * off to the SSH-orchestrated resize - and that a failure in an earlier link aborts the chain so later links never run
 * (no deadlock, chain still cleaned up). The GCP path additionally chains the heavy FULL_STOP / FULL_START stack flows and
 * is covered structurally by {@code UpdateDistroxDiskFlowEventChainFactoryTest}; only the queue-independent wiring is
 * exercised here.
 */
@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class UpdateDistroxDiskFlowChainIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String DATAHUB_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":cluster:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String GROUP = "compute";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String CLUSTER_NAME = "test-cluster";

    private static final String ACCOUNT_ID = "test-account";

    private static final int CURRENT_SIZE = 50;

    private static final int REQUESTED_SIZE = 100;

    private static final String CURRENT_TYPE = "gp2";

    private static final String REQUESTED_TYPE = "gp3";

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private ReactorNotifier reactorNotifier;

    @MockitoBean(reset = MockReset.NONE)
    private StackDtoService stackDtoService;

    @MockitoBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockitoBean
    private DiskUpdateService diskUpdateService;

    @MockitoBean
    private VerticalScalingValidatorService verticalScalingValidatorService;

    @MockitoBean
    private CloudbreakFlowMessageService flowMessageService;

    @MockitoBean
    private StackUpdater stackUpdater;

    @MockitoBean
    private SaltUpdateService saltUpdateService;

    @MockitoBean
    private ClusterBootstrapper clusterBootstrapper;

    @MockitoBean
    private ClusterProxyService clusterProxyService;

    @MockitoBean
    private RecipeEngine recipeEngine;

    @MockitoBean
    private GatewayConfigService gatewayConfigService;

    @MockitoBean
    private KerberosConfigService kerberosConfigService;

    @MockitoBean
    private KerberosDetailService kerberosDetailService;

    @MockitoBean
    private HostOrchestrator hostOrchestrator;

    @MockitoBean
    private KeytabProvider keytabProvider;

    @MockitoBean
    private EnvironmentConfigProvider environmentConfigProvider;

    @MockitoBean
    private ClusterServiceRunner clusterServiceRunner;

    @MockitoBean
    private ClusterApiConnectors clusterApiConnectors;

    @MockitoBean
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    @MockitoBean
    private StackUtil stackUtil;

    @MockitoBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockitoBean
    private CrnUserDetailsService crnUserDetailsService;

    @MockitoBean
    private NodeConfig nodeConfig;

    @MockitoSpyBean
    private FlowChains flowChains;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @MockitoBean
    private NodeValidator nodeValidator;

    @MockitoBean
    private FlowCancelService flowCancelService;

    @MockitoBean
    private FlowUsageSender flowUsageSender;

    private ClusterApi clusterApi;

    @BeforeEach
    void setUp() {
        Stack stack = mockStack();
        StackDto stackDto = mockStackDto(stack);
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(stack.getCluster());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        Stack resizeStack = mock(Stack.class);
        when(resizeStack.getDiskResourceType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(resizeStack);
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(mock(Stack.class));

        clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(), any())).thenReturn(clusterApi);
        doNothing().when(nodeValidator).checkForRecentHeartbeat();

        when(diskUpdateService.isDiskTypeChangeSupported(CLOUD_PLATFORM)).thenReturn(true);
        when(verticalScalingValidatorService.validateAddVolumesRequest(any(), any(), any())).thenReturn(ValidationResult.builder().build());
    }

    @Test
    void testChainHappyPathRunsSaltUpdateDiskUpdateAndResize() throws Exception {
        triggerAndWait();

        chainFinished();
        verify(diskUpdateService).updateDiskTypeAndSize(eq(GROUP), eq(REQUESTED_TYPE), eq(REQUESTED_SIZE), any(), eq(STACK_ID));
        verify(diskUpdateService).resizeDisks(any(Stack.class), eq(GROUP));
    }

    @Test
    void testChainAbortsWhenSaltUpdateFails() throws Exception {
        doThrow(BadRequestException.class).when(clusterBootstrapper).reBootstrapMachines(anyLong());

        triggerAndWait();

        chainFinished();
        verify(diskUpdateService, never()).updateDiskTypeAndSize(anyString(), anyString(), anyInt(), any(), anyLong());
        verify(diskUpdateService, never()).resizeDisks(any(), anyString());
    }

    @Test
    void testChainAbortsWhenDiskUpdateFails() throws Exception {
        doThrow(new RuntimeException("disk update failed")).when(diskUpdateService)
                .updateDiskTypeAndSize(anyString(), anyString(), anyInt(), any(), anyLong());

        triggerAndWait();

        chainFinished();
        verify(diskUpdateService, never()).resizeDisks(any(), anyString());
    }

    private void triggerAndWait() {
        triggerFlow();
        letItFlow();
    }

    private FlowIdentifier triggerFlow() {
        String selector = DISTROX_DISK_UPDATE_CHAIN_TRIGGER_EVENT;
        DistroXDiskUpdateTriggerEvent triggerEvent = DistroXDiskUpdateTriggerEvent.builder()
                .withSelector(selector)
                .withResourceId(STACK_ID)
                .withStackId(STACK_ID)
                .withClusterName(CLUSTER_NAME)
                .withAccountId(ACCOUNT_ID)
                .withCloudPlatform(CLOUD_PLATFORM)
                .withGroup(GROUP)
                .withSize(REQUESTED_SIZE)
                .withVolumeType(REQUESTED_TYPE)
                .withDiskTypeChangeRequested(true)
                .withAccepted(new Promise<>())
                .build();
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> reactorNotifier.notify(STACK_ID, selector, triggerEvent));
    }

    private void letItFlow() {
        verify(flowChains, timeout(50000).atLeastOnce()).removeFlowChain(anyString(), anyBoolean());
    }

    private void chainFinished() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, timeout(50000).atLeastOnce()).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    private Stack mockStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("stackname");
        StackStatus stackStatus = new StackStatus(stack, Status.AVAILABLE, "no reason at all", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);
        Cluster cluster = new Cluster();
        cluster.setId(0L);
        stack.setCluster(cluster);
        User user = new User();
        user.setUserId("alma");
        stack.setCreator(user);
        stack.setResourceCrn(DATAHUB_CRN);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        return stack;
    }

    private StackDto mockStackDto(Stack stack) {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getName()).thenReturn("stackname");
        when(stackDto.getResourceCrn()).thenReturn(DATAHUB_CRN);
        when(stackDto.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(stackDto.getResources()).thenReturn(Set.of(volumeSetResource()));
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getWorkspace()).thenReturn(stack.getWorkspace());
        return stackDto;
    }

    private Resource volumeSetResource() {
        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder()
                .withVolumes(List.of(new VolumeSetAttributes.Volume("vol-1", "/dev/xvdb", CURRENT_SIZE, CURRENT_TYPE, CloudVolumeUsageType.GENERAL)))
                .build();
        Resource resource = new Resource();
        resource.setInstanceId("i-123");
        resource.setInstanceGroup(GROUP);
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        resource.setResourceName("vol-set-1");
        resource.setAttributes(new Json(attributes));
        return resource;
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            SaltUpdateFlowConfig.class,
            SaltUpdateActions.class,
            BootstrapMachineHandler.class,
            UploadRecipesHandler.class,
            KeytabConfigurationHandler.class,
            StartAmbariServicesHandler.class,
            UpdateDistroxDiskFlowEventChainFactory.class,
            DistroXDiskUpdateFlowConfig.class,
            DistroXDiskUpdateActions.class,
            DistroXDiskUpdateValidationHandler.class,
            DistroXDiskUpdateHandler.class,
            DiskResizeFlowConfig.class,
            DiskResizeActions.class,
            DiskResizeHandler.class,
            FlowEventListenerAdapter.class,
            FlowIntegrationTestConfig.class
    })
    static class Config {

        @Bean
        CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any()))
                    .thenReturn(Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }
}
