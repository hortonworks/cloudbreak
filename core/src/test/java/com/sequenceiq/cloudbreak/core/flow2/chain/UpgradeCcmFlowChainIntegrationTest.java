package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.common.api.type.Tunnel.CCM;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerDefaultConfigAdjuster;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.CcmUpgradeFlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateActions;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.FlowIntegrationTestConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UserDataUpdateActions;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.DeregisterAgentHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.FinalizeHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.PushSaltStateHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.ReconfigureNginxHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RegisterClusterProxyHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RemoveAgentHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RevertAllHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.RevertSaltStatesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm.TunnelUpdateHandler;
import com.sequenceiq.cloudbreak.reactor.handler.kerberos.KeytabConfigurationHandler;
import com.sequenceiq.cloudbreak.reactor.handler.kerberos.KeytabProvider;
import com.sequenceiq.cloudbreak.reactor.handler.orchestration.BootstrapMachineHandler;
import com.sequenceiq.cloudbreak.reactor.handler.orchestration.StartAmbariServicesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.recipe.UploadRecipesHandler;
import com.sequenceiq.cloudbreak.reactor.handler.userdata.UpdateUserDataHandler;
import com.sequenceiq.cloudbreak.reactor.handler.userdata.UpdateUserDataOnProviderHandler;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowCancelService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"cb.ccmRevertJob.activationInMinutes=0"})
class UpgradeCcmFlowChainIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String DATAHUB_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":cluster:" + UUID.randomUUID();

    private static final String USER_DATA = "hello hello is there anybody out there";

    private static final Map<InstanceGroupType, String> USER_DATA_MAP = Map.of(InstanceGroupType.GATEWAY, USER_DATA);

    private static final long STACK_ID = 1L;

    private static final int ALL_CALLED_ONCE = 4;

    private static final int CALLED_TILL_BOOTSTRAP = 1;

    private static final int CALLED_TILL_UPDATE_TUNNEL = 2;

    private static final int CALLED_TILL_UPDATE_USERDATA = 4;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private ReactorNotifier reactorNotifier;

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean(reset = MockReset.NONE)
    private StackDtoService stackDtoService;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private CloudbreakFlowMessageService flowMessageService;

    @MockBean
    private UserDataService userDataService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private StackToCloudStackConverter cloudStackConverter;

    @MockBean
    private CredentialToCloudCredentialConverter credentialConverter;

    @MockBean(reset = MockReset.NONE)
    private ImageService imageService;

    @MockBean
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @MockBean
    private StackUtil stackUtil;

    @MockBean
    private UpgradeCcmService upgradeCcmService;

    @MockBean
    private SaltUpdateService saltUpdateService;

    @MockBean
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @MockBean
    private ClusterBootstrapper clusterBootstrapper;

    @MockBean
    private ClusterProxyService clusterProxyService;

    @MockBean
    private RecipeEngine recipeEngine;

    @MockBean
    private GatewayConfigService gatewayConfigService;

    @MockBean
    private KerberosConfigService kerberosConfigService;

    @MockBean
    private KerberosDetailService kerberosDetailService;

    @MockBean
    private HostOrchestrator hostOrchestrator;

    @MockBean
    private KeytabProvider keytabProvider;

    @MockBean
    private EnvironmentConfigProvider environmentConfigProvider;

    @MockBean
    private ClusterServiceRunner clusterServiceRunner;

    @MockBean
    private ClusterApiConnectors clusterApiConnectors;

    @MockBean
    private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

    @MockBean
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    @MockBean
    private CrnUserDetailsService crnUserDetailsService;

    @MockBean
    private NodeConfig nodeConfig;

    @SpyBean
    private FlowChains flowChains;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private NodeValidator nodeValidator;

    @MockBean
    private FlowCancelService flowCancelService;

    @MockBean
    private FlowUsageSender flowUsageSender;

    @MockBean
    private FlowEventListener flowEventListener;

    @Mock
    private ResourceConnector resourcesApi;

    @Mock
    private ClusterApi clusterApi;

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

    private void mockStackService() {
        StackDto stackDto = spy(StackDto.class);
        Stack stack = mockStack();
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(stack.getCluster());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getWorkspace()).thenReturn(stack.getWorkspace());
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
    }

    @BeforeEach
    public void setup() throws CloudbreakImageNotFoundException {
        mockStackService();
        Image image = new Image("alma", USER_DATA_MAP, "", "", "", "", "", "", null, null, null, null);
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        when(userDataService.getUserData(anyLong())).thenReturn(Map.of(
                InstanceGroupType.CORE, "core",
                InstanceGroupType.GATEWAY, "gateway"));

        CloudConnector connector = mock(CloudConnector.class);
        AuthenticatedContext context = mock(AuthenticatedContext.class);
        Authenticator authApi = mock(Authenticator.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authApi);
        when(connector.resources()).thenReturn(resourcesApi);
        when(authApi.authenticate(any(), any())).thenReturn(context);
        when(clusterApiConnectors.getConnector(any(), any())).thenReturn(clusterApi);
        when(userDataService.getUserData(anyLong())).thenReturn(USER_DATA_MAP);
        doNothing().when(nodeValidator).checkForRecentHeartbeat();
    }

    @Test
    public void testCcmUpgradeFlowChainWhenSuccessful() throws Exception {
        testFlow(ALL_CALLED_ONCE, true, true, true);
    }

    @Test
    public void testCcmUpgradeFlowChainWhenSaltUpdateFails() throws Exception {
        doThrow(BadRequestException.class).when(clusterBootstrapper).reBootstrapMachines(anyLong());
        testFlow(CALLED_TILL_BOOTSTRAP, false, false, false);
    }

    @Test
    public void testCcmUpgradeFlowChainWhenUpdateCcmFails() throws Exception {
        doThrow(BadRequestException.class).when(upgradeCcmService).updateTunnel(anyLong(), eq(Tunnel.latestUpgradeTarget()));
        testFlow(CALLED_TILL_UPDATE_TUNNEL, true, false, false);
    }

    @Test
    public void testCcmUpgradeFlowChainPassWhenAgentRemoveFails() throws Exception {
        doThrow(BadRequestException.class).when(upgradeCcmService).removeAgent(anyLong(), eq(Tunnel.latestUpgradeTarget()));
        testFlow(ALL_CALLED_ONCE, true, true, true);
    }

    @Test
    public void testCcmUpgradeFlowChainWhenUpdateUserDataFails() throws Exception {
        doThrow(BadRequestException.class).when(userDataService).updateJumpgateFlagOnly(anyLong());
        testFlow(CALLED_TILL_UPDATE_USERDATA, true, true, false);
    }

    private void testFlow(int calledOnce, boolean saltUpdateSuccess, boolean ccmUpgradeSuccess, boolean userDataUpdateSuccess) throws Exception {
        triggerFlow();
        letItFlow();

        flowFinished(ccmUpgradeSuccess ? 3 : (saltUpdateSuccess ? 2 : 1));
        verifyFinishingStatCalls(saltUpdateSuccess, ccmUpgradeSuccess, userDataUpdateSuccess);
        verifyServiceCalls(calledOnce);
    }

    private void verifyFinishingStatCalls(boolean saltUpdateSuccess, boolean ccmUpgradeSuccess, boolean userDataUpdateSuccess) throws Exception {
        verify(upgradeCcmService, times(ccmUpgradeSuccess ? 1 : 0)).ccmUpgradeFinished(eq(1L), eq(0L), anyBoolean());
        verify(resourcesApi, times(userDataUpdateSuccess ? 1 : 0)).updateUserData(any(), any(), any(), eq(USER_DATA_MAP));
        verify(upgradeCcmService, times(ccmUpgradeSuccess || !saltUpdateSuccess ? 0 : 1)).ccmUpgradeFailed(any(), anyLong());
        verify(clusterApi, times(saltUpdateSuccess ? 1 : 0)).waitForServer(anyBoolean());
    }

    private void verifyServiceCalls(int calledOnceCount) throws Exception {
        final int[] expected = new int[ALL_CALLED_ONCE];
        Arrays.fill(expected, 0, calledOnceCount, 1);
        int i = 0;
        InOrder inOrder = inOrder(upgradeCcmService, userDataService, resourcesApi, clusterBootstrapper);
        inOrder.verify(clusterBootstrapper, times(expected[i++])).reBootstrapMachines(STACK_ID);
        inOrder.verify(upgradeCcmService, times(expected[i++])).updateTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        inOrder.verify(upgradeCcmService, times(expected[i++])).removeAgent(STACK_ID, CCM);
        inOrder.verify(userDataService, times(expected[i++])).updateJumpgateFlagOnly(STACK_ID);
    }

    private void flowFinished(int numberOfRanFlows) {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2 * numberOfRanFlows)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        String selector = UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> reactorNotifier.notify(STACK_ID, selector,
                        new UpgradeCcmFlowChainTriggerEvent(selector, STACK_ID, 1L, CCM)));
    }

    private void letItFlow() {
        verify(flowChains, timeout(50000).atLeastOnce()).removeFlowChain(anyString(), anyBoolean());
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
            UpgradeCcmFlowEventChainFactory.class,
            UpdateUserDataFlowConfig.class,
            UserDataUpdateActions.class,
            UpdateUserDataHandler.class,
            UpdateUserDataOnProviderHandler.class,
            UpgradeCcmActions.class,
            UpgradeCcmFlowConfig.class,
            UpgradeCcmService.class,
            CcmUpgradeFlowTriggerCondition.class,
            DeregisterAgentHandler.class,
            PushSaltStateHandler.class,
            ReconfigureNginxHandler.class,
            RegisterClusterProxyHandler.class,
            RemoveAgentHandler.class,
            TunnelUpdateHandler.class,
            FlowIntegrationTestConfig.class,
            RevertSaltStatesHandler.class,
            RevertAllHandler.class,
            FinalizeHandler.class
    })
    static class Config {

        @Bean
        public CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any())).thenReturn(
                    Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }
}
