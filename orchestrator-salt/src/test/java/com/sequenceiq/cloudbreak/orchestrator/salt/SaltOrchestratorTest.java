package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.NodeVolumes;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.CmAgentStopFlags;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostAndRoleTarget;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.grain.GrainUploader;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrapFactory;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUpload;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ModifyGrainBase;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.StateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@ExtendWith(MockitoExtension.class)
class SaltOrchestratorTest {

    private static final String OLD_PASSWORD = "old-password";

    private static final String NEW_PASSWORD = "new-password";

    private GatewayConfig gatewayConfig;

    private Set<Node> targets;

    @Mock
    private GenericResponses genericResponses;

    @Mock
    private SaltConnector saltConnector;

    @Mock
    private ExitCriteria exitCriteria;

    @Mock
    private ExitCriteriaModel exitCriteriaModel;

    @Mock
    private HostDiscoveryService hostDiscoveryService;

    @Mock
    private SaltRunner saltRunner;

    @Mock
    private SaltCommandRunner saltCommandRunner;

    @Mock
    private GrainUploader grainUploader;

    @Mock
    private SaltService saltService;

    @Mock
    private Retry retry;

    @Mock
    private CompressUtil compressUtil;

    @Mock
    private Callable<Boolean> callable;

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltBootstrapFactory saltBootstrapFactory;

    @Mock
    private SaltBootstrap saltBootstrap;

    @Mock
    private Optional<DelayedExecutorService> delayedExecutorService;

    @InjectMocks
    private SaltOrchestrator saltOrchestrator;

    @Captor
    private ArgumentCaptor<Target<String>> targetCaptor;

    private List<SaltConnector> saltConnectors;

    @BeforeEach
    void setUp() throws Exception {
        gatewayConfig = new GatewayConfig("172.16.252.43", "1.1.1.1", "10.0.0.1", "10-0-0-1", 9443, "instanceid", "servercert", "clientcert", "clientkey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, "masterPrivateKey", "masterPublicKey", "privatekey", "publickey", null, null,
                null, null);
        targets = new HashSet<>();
        NodeVolumes nodeVolumes = mock(NodeVolumes.class);
        targets.add(new Node("10.0.0.1", "1.1.1.1", "instanceid1", "hg", "10-0-0-1.example.com", "hg",
                nodeVolumes, TemporaryStorage.EPHEMERAL_VOLUMES));
        targets.add(new Node("10.0.0.2", "1.1.1.2", "instanceid2", "hg", "10-0-0-2.example.com", "hg",
                nodeVolumes, TemporaryStorage.EPHEMERAL_VOLUMES));
        targets.add(new Node("10.0.0.3", "1.1.1.3", "instanceid3", "hg", "10-0-0-3.example.com", "hg",
                nodeVolumes, TemporaryStorage.EPHEMERAL_VOLUMES));

        lenient().when(hostDiscoveryService.determineDomain("test", "test", false)).thenReturn(".example.com");
        lenient().when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
        lenient().when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyInt(), anyInt()))
                .thenReturn(callable);
        lenient().when(saltRunner.runnerWithConfiguredErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        lenient().when(saltRunner.runnerWithCalculatedErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class),
                        any(ExitCriteriaModel.class), anyInt()))
                .thenReturn(callable);
        lenient().when(saltRunner.runnerWithConfiguredErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        lenient().when(callable.call()).thenReturn(true);
        lenient().when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(saltConnector);
        saltConnectors = List.of(saltConnector);
        lenient().when(saltService.createSaltConnector(anyCollection())).thenReturn(saltConnectors);
        lenient().when(saltService.getPrimaryGatewayConfig(anyList())).thenReturn(gatewayConfig);
        lenient().when(saltBootstrapFactory.of(any(), anyCollection(), anyList(), anySet(), any())).thenReturn(saltBootstrap);
        lenient().when(saltStateService.bootstrap(eq(saltConnector), any(), any(), eq(targets))).thenReturn(genericResponses);
    }

    @Test
    void bootstrapTest() throws Exception {
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "salt")).thenReturn(new byte[]{});

        BootstrapParams bootstrapParams = mock(BootstrapParams.class);
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);

        saltOrchestrator.bootstrap(allGatewayConfigs, targets, bootstrapParams, exitCriteriaModel, false);

        verify(saltRunner, times(8)).runnerWithConfiguredErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        // salt.zip, master_sign.pem, master_sign.pub
        verify(saltBootstrapFactory, times(1)).of(eq(saltConnector), eq(saltConnectors), eq(allGatewayConfigs), eq(targets),
                eq(bootstrapParams));
    }

    @Test
    void changePasswordTest() throws Exception {
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);

        setupChangePasswordResponsesWithOneSuccessAndOneWithStatusCode(200, NEW_PASSWORD);

        saltOrchestrator.changePassword(allGatewayConfigs, NEW_PASSWORD, OLD_PASSWORD);

        verify(saltStateService).changePassword(eq(saltConnector), eq(Set.of(gatewayConfig.getPrivateAddress())), eq(NEW_PASSWORD));
    }

    @Test
    void changePasswordWebApplicationExceptionTest() throws Exception {
        when(saltStateService.changePassword(any(), any(), any())).thenThrow(new WebApplicationException("500 Internal Server Error"));

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> saltOrchestrator.changePassword(Collections.singletonList(gatewayConfig), NEW_PASSWORD, OLD_PASSWORD));
        assertEquals("Salt-bootstrap responded with error, please check the service status on node 10.0.0.1 and retry the operation. " +
                        "Details: 500 Internal Server Error",
                exception.getMessage());
    }

    @Test
    void changePasswordErrorResponsesTest() throws Exception {
        setupChangePasswordResponsesWithOneSuccessAndOneWithStatusCode(500, NEW_PASSWORD);

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> saltOrchestrator.changePassword(Collections.singletonList(gatewayConfig), NEW_PASSWORD, OLD_PASSWORD));
        assertEquals("Failed to change password on some of the nodes, so you may experience issues with the cluster until the password is fixed. " +
                        "Please check the salt-bootstrap service status on nodes and retry the operation. Details from nodes: 10.0.0.1: HTTP 500 error-text",
                exception.getMessage());
    }

    @Test
    void changePasswordRevertPassword() throws Exception {
        setupChangePasswordResponsesWithOneSuccessAndOneWithStatusCode(500, NEW_PASSWORD);
        setupChangePasswordResponsesWithOneSuccessAndOneWithStatusCode(200, OLD_PASSWORD);

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> saltOrchestrator.changePassword(Collections.singletonList(gatewayConfig), NEW_PASSWORD, OLD_PASSWORD));
        assertEquals("Failed to change password on some of the nodes, but successfully reverted back to old password so cluster health is not affected. " +
                        "Please check the salt-bootstrap service status on nodes and retry the operation. Details from nodes: 10.0.0.1: HTTP 500 error-text",
                exception.getMessage());
    }

    private void setupChangePasswordResponsesWithOneSuccessAndOneWithStatusCode(int statusCode, String password) throws Exception {
        GenericResponses genericResponses = new GenericResponses();
        GenericResponse response = new GenericResponse();
        response.setAddress(gatewayConfig.getPrivateAddress());
        response.setStatusCode(statusCode);
        response.setErrorText("error-text");
        // add a success response to emulate partial success
        GenericResponse successResponse = new GenericResponse();
        successResponse.setAddress("10.0.0.2");
        successResponse.setStatusCode(200);

        genericResponses.setResponses(List.of(response, successResponse));

        when(saltStateService.changePassword(any(), any(), eq(password))).thenReturn(genericResponses);
    }

    @Test
    void bootstrapNewNodesTest() throws Exception {
        BootstrapParams bootstrapParams = mock(BootstrapParams.class);
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "salt")).thenReturn(new byte[]{});

        saltOrchestrator.bootstrapNewNodes(Collections.singletonList(gatewayConfig), targets, targets, null, bootstrapParams, exitCriteriaModel, false);

        verify(saltRunner, times(1)).runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        verify(saltRunner, times(7)).runnerWithConfiguredErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        verify(saltBootstrapFactory, times(1))
                .of(eq(saltConnector), eq(saltConnectors), eq(Collections.singletonList(gatewayConfig)), eq(targets), eq(bootstrapParams));
    }

    @Test
    void reBootstrapExistingNodesTest() throws Exception {
        BootstrapParams bootstrapParams = mock(BootstrapParams.class);
        List<GatewayConfig> gatewayConfigs = List.of(gatewayConfig);

        saltOrchestrator.reBootstrapExistingNodes(gatewayConfigs, targets, bootstrapParams, exitCriteriaModel);

        verify(saltStateService).bootstrap(saltConnector, bootstrapParams, gatewayConfigs, targets);
    }

    @Test
    void reBootstrapExistingNodesFailureTest() throws Exception {
        BootstrapParams bootstrapParams = mock(BootstrapParams.class);
        List<GatewayConfig> gatewayConfigs = List.of(gatewayConfig);
        GenericResponse genericResponse = mock(GenericResponse.class);
        when(genericResponse.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(genericResponse.getAddress()).thenReturn("127.0.0.1");
        when(genericResponses.getResponses()).thenReturn(List.of(genericResponse));

        assertThatThrownBy(() -> saltOrchestrator.reBootstrapExistingNodes(gatewayConfigs, targets, bootstrapParams, exitCriteriaModel))
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasMessage("Failed to rebootstrap existing nodes [127.0.0.1]");
    }

    @Test
    void runServiceTest() throws Exception {
        SaltConfig saltConfig = new SaltConfig();
        OrchestratorAware orchestratorAware = mock(OrchestratorAware.class);
        when(orchestratorAware.getAllNotDeletedNodes()).thenReturn(Set.of());
        saltOrchestrator.initServiceRun(orchestratorAware, Collections.singletonList(gatewayConfig), targets, targets,
                saltConfig, exitCriteriaModel, "testPlatform");
        saltOrchestrator.runService(Collections.singletonList(gatewayConfig), targets, exitCriteriaModel);

        verify(saltCommandRunner, times(1)).runSaltCommand(any(SaltConnector.class), any(BaseSaltJobRunner.class),
                any(ExitCriteriaModel.class), any(ExitCriteria.class));
        verify(saltCommandRunner, times(2)).runModifyGrainCommand(any(SaltConnector.class), any(ModifyGrainBase.class),
                any(ExitCriteriaModel.class), any(ExitCriteria.class));
        verify(grainUploader, times(1)).uploadGrains(anySet(), anyList(), any(ExitCriteriaModel.class), any(SaltConnector.class),
                any(ExitCriteria.class));
    }

    @Test
    void tearDownTest() throws Exception {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");
        Set<String> privateIps = new HashSet<>(privateIpsByFQDN.values());

        List<String> downNodes = Lists.newArrayList("10-0-0-4.example.com", "10-0-0-5.example.com");

        saltOrchestrator.tearDown(null, Collections.singletonList(gatewayConfig), Multimaps.forMap(privateIpsByFQDN), Set.of(), null);

        verify(saltConnector, never()).wheel(eq("key.delete"), eq(downNodes), eq(Object.class));
        verify(saltStateService).stopMinions(eq(saltConnector), eq(privateIps));
    }

    @Test
    void tearDownReusedIpAddressTest() throws Exception {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");

        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        List<String> upNodes = Lists.newArrayList("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com");
        minionStatus.setUp(upNodes);
        List<String> downNodes = Lists.newArrayList("10-0-0-4.example.com", "10-0-0-5.example.com");
        minionStatus.setDown(downNodes);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        when(saltStateService.collectNodeStatus(eq(saltConnector))).thenReturn(minionStatusSaltResponse);
        Callable<Boolean> callable = mock(Callable.class);
        when(saltRunner.runnerWithConfiguredErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);

        Node remainingNode = mock(Node.class);
        when(remainingNode.getPrivateIp()).thenReturn("10.0.0.1");
        ExitCriteriaModel exitCriteriaModel = mock(ExitCriteriaModel.class);
        OrchestratorAware orchestratorAware = mock(OrchestratorAware.class);
        when(orchestratorAware.getAllNotDeletedNodes()).thenReturn(Set.of());

        saltOrchestrator.tearDown(orchestratorAware, Collections.singletonList(gatewayConfig), Multimaps.forMap(privateIpsByFQDN), Set.of(remainingNode),
                exitCriteriaModel);

        verify(saltConnector, times(1)).wheel(eq("key.delete"), eq(downNodes), eq(Object.class));

        verify(saltStateService).stopMinions(eq(saltConnector), eq(Set.of("10.0.0.2", "10.0.0.3")));
    }

    @Test
    void tearDownFailTest() throws Exception {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");
        Set<String> privateIps = new HashSet<>(privateIpsByFQDN.values());

        doThrow(new NullPointerException("message")).when(saltStateService).stopMinions(eq(saltConnector), eq(privateIps));

        assertThatThrownBy(() ->
                saltOrchestrator.tearDown(null, Collections.singletonList(gatewayConfig), Multimaps.forMap(privateIpsByFQDN), Set.of(), null))
                .hasMessage("message")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void getMissingNodesTest() {
        assertThat(saltOrchestrator.getMissingNodes(gatewayConfig, targets)).hasSize(0);
    }

    @Test
    void getAvailableNodesTest() {
        assertThat(saltOrchestrator.getAvailableNodes(gatewayConfig, targets)).hasSize(0);
    }

    @Test
    void isBootstrapApiAvailableTest() {
        GenericResponse response = new GenericResponse();
        response.setStatusCode(200);
        when(saltConnector.health()).thenReturn(response);

        boolean bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig);
        assertTrue(bootstrapApiAvailable);
    }

    @Test
    void isBootstrapApiAvailableFailTest() {
        GenericResponse response = new GenericResponse();
        response.setStatusCode(404);
        when(saltConnector.health()).thenReturn(response);

        boolean bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig);
        assertFalse(bootstrapApiAvailable);
    }

    @Test
    void testUploadGatewayPillarShouldSaveGatewayPillarProperties() throws CloudbreakOrchestratorFailedException {
        SaltConfig saltConfig = mock(SaltConfig.class, RETURNS_DEEP_STUBS);
        String gatewayPillarPath = "gatewaypath";
        SaltPillarProperties gatewayPath = new SaltPillarProperties(gatewayPillarPath, Map.of());
        Callable<Boolean> callable = mock(Callable.class);
        when(saltConfig.getServicePillarConfig().get("gateway")).thenReturn(gatewayPath);
        when(saltRunner.runnerWithConfiguredErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        ArgumentCaptor<PillarSave> pillarSaveArgumentCaptor =
                ArgumentCaptor.forClass(PillarSave.class);

        saltOrchestrator.uploadGatewayPillar(Collections.singletonList(gatewayConfig), targets, exitCriteriaModel, saltConfig);

        verify(saltRunner).runnerWithConfiguredErrorCount(pillarSaveArgumentCaptor.capture(), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        PillarSave capturedPillarSave = pillarSaveArgumentCaptor.getValue();
        Pillar pillar = (Pillar) ReflectionTestUtils.getField(capturedPillarSave, "pillar");
        assertEquals(gatewayPillarPath, pillar.getPath());
    }

    @Test
    void testUploadGatewayPillarShouldThrowExceptionWhenThereIsNoGatewayPillar() {
        SaltConfig saltConfig = mock(SaltConfig.class, RETURNS_DEEP_STUBS);
        when(saltConfig.getServicePillarConfig().get("gateway")).thenReturn(null);
        assertThrows(CloudbreakOrchestratorFailedException.class, () ->
                saltOrchestrator.uploadGatewayPillar(Collections.singletonList(gatewayConfig), targets, exitCriteriaModel, saltConfig));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testStopClusterManagerAgent() throws Exception {
        Set<Node> downscaleTargets = new HashSet<>();
        downscaleTargets.add(new Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com", "hg", "fqdn2", null));
        downscaleTargets.add(new Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com", "hg", "fqdn3", null));

        Set<String> responsiveAddresses = new HashSet<>();
        responsiveAddresses.add("10.0.0.1");
        responsiveAddresses.add("10.0.0.2");
        responsiveAddresses.add("10.0.0.3");
        when(saltStateService.collectMinionIpAddresses(any(), any(Retry.class), any())).thenReturn(responsiveAddresses);

        Set<Node> allNodes = new HashSet<>();
        allNodes.add(new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com", "hg", "fqdn3", null));
        allNodes.addAll(downscaleTargets);

        when(saltRunner.runnerWithConfiguredErrorCount(any(SaltJobIdTracker.class), any(), any())).thenReturn(mock(Callable.class));
        Callable pillarSaveCallable = mock(Callable.class);
        when(saltRunner.runnerWithConfiguredErrorCount(any(PillarSave.class), any(), any())).thenReturn(pillarSaveCallable);
        when(saltRunner.runnerWithCalculatedErrorCount(any(), any(), any(), anyInt())).thenReturn(mock(Callable.class));

        OrchestratorAware orchestratorAware = mock(OrchestratorAware.class);
        when(orchestratorAware.getAllNotDeletedNodes()).thenReturn(allNodes);
        saltOrchestrator.stopClusterManagerAgent(orchestratorAware, gatewayConfig, targets, downscaleTargets,
                exitCriteriaModel, new CmAgentStopFlags(false, false, false));

        ArgumentCaptor<ModifyGrainBase> modifyGrainBaseArgumentCaptor = ArgumentCaptor.forClass(ModifyGrainBase.class);
        ArgumentCaptor<PillarSave> pillarSaveArgumentCaptor = ArgumentCaptor.forClass(PillarSave.class);
        verify(pillarSaveCallable, times(1)).call();

        InOrder inOrder = inOrder(saltCommandRunner, saltRunner);

        inOrder.verify(saltCommandRunner).runModifyGrainCommand(any(), modifyGrainBaseArgumentCaptor.capture(), any(), any());
        ModifyGrainBase modifyGrainBase = modifyGrainBaseArgumentCaptor.getValue();
        assertThat(modifyGrainBase).isInstanceOf(GrainAddRunner.class);
        assertEquals("roles", modifyGrainBase.getKey());
        assertEquals("cloudera_manager_agent_stop", modifyGrainBase.getValue());

        inOrder.verify(saltRunner).runnerWithConfiguredErrorCount(any(SaltJobIdTracker.class), any(), any());
        inOrder.verify(saltRunner).runnerWithConfiguredErrorCount(pillarSaveArgumentCaptor.capture(), any(), any());
        PillarSave capturedPillarSave = pillarSaveArgumentCaptor.getValue();

        ArgumentCaptor<SaltJobIdTracker> saltJobIdCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);
        inOrder.verify(saltRunner).runnerWithCalculatedErrorCount(saltJobIdCaptor.capture(), any(), any(), anyInt());

        inOrder.verify(saltCommandRunner).runModifyGrainCommand(any(), modifyGrainBaseArgumentCaptor.capture(), any(), any());
        inOrder.verifyNoMoreInteractions();
        modifyGrainBase = modifyGrainBaseArgumentCaptor.getValue();
        assertThat(modifyGrainBase).isInstanceOf(GrainRemoveRunner.class);
        assertEquals("roles", modifyGrainBase.getKey());
        assertEquals("cloudera_manager_agent_stop", modifyGrainBase.getValue());

        Set<String> targets = (Set<String>) ReflectionTestUtils.getField(capturedPillarSave, "targets");
        assertTrue(targets.contains("10.0.0.1"));

        Pillar pillar = (Pillar) ReflectionTestUtils.getField(capturedPillarSave, "pillar");
        Map<String, Map> hosts = (Map) ((Map) pillar.getJson()).get("hosts");
        assertTrue(hosts.containsKey("10.0.0.1"));
        assertTrue(hosts.containsKey("10.0.0.2"));
        assertTrue(hosts.containsKey("10.0.0.3"));
    }

    @Test
    void testInstallFreeIpa() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Callable<Boolean> callable = mock(Callable.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        when(saltStateService.getGrains(any(), any(), any()))
                .thenReturn(Map.of())
                .thenReturn(Map.of())
                .thenReturn(Map.of());
        when(saltRunner.runnerWithCalculatedErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyInt()))
                .thenReturn(callable);
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);

        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway),
                Set.of(primaryNode), exitCriteriaModel);

        verify(saltRunner, times(1)).runnerWithCalculatedErrorCount(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
    }

    @Test
    void testInstallFreeIpaHa() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        GatewayConfig replica1Config = mock(GatewayConfig.class);
        GatewayConfig replica2Config = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Node replica1Node = mock(Node.class);
        Node replica2Node = mock(Node.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(replica1Node.getHostname()).thenReturn("replica1.example.com");
        when(replica2Node.getHostname()).thenReturn("replica2.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        when(replica1Config.getHostname()).thenReturn("replica1.example.com");
        when(replica2Config.getHostname()).thenReturn("replica2.example.com");
        when(saltStateService.getGrains(any(), any(), any()))
                .thenReturn(Map.of())
                .thenReturn(Map.of())
                .thenReturn(Map.of());
        when(delayedExecutorService.isPresent()).thenReturn(Boolean.TRUE);
        DelayedExecutorService executorService = mock(DelayedExecutorService.class);
        when(delayedExecutorService.get()).thenReturn(executorService);
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);

        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway, replica1Config, replica2Config),
                Set.of(primaryNode, replica1Node, replica2Node), exitCriteriaModel);

        verify(saltRunner, times(3)).runnerWithCalculatedErrorCount(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("replica1.example.com"), jobIdTrackers.get(1).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("replica2.example.com"), jobIdTrackers.get(2).getSaltJobRunner().getTargetHostnames());
        verify(executorService, times(2)).runWithDelay(any(Callable.class), anyLong(), any());
    }

    @Test
    void testInstallFreeIpaHaRepairOneInstance() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        GatewayConfig replicaConfig = mock(GatewayConfig.class);
        GatewayConfig newReplicaConfig = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Node replicaNode = mock(Node.class);
        Node newReplicaNode = mock(Node.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(replicaNode.getHostname()).thenReturn("replica.example.com");
        when(newReplicaNode.getHostname()).thenReturn("new_replica.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        when(replicaConfig.getHostname()).thenReturn("replica.example.com");
        when(newReplicaConfig.getHostname()).thenReturn("new_replica.example.com");
        when(saltStateService.getGrains(any(), any(), any()))
                .thenReturn(Map.of("primary.example.com", mock(JsonNode.class)))
                .thenReturn(Map.of())
                .thenReturn(Map.of("replica.example.com", mock(JsonNode.class)));
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);

        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway, replicaConfig, newReplicaConfig),
                Set.of(primaryNode, replicaNode, newReplicaNode), exitCriteriaModel);

        verify(saltRunner, times(3)).runnerWithCalculatedErrorCount(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("replica.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(1).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("new_replica.example.com"), jobIdTrackers.get(2).getSaltJobRunner().getTargetHostnames());
    }

    @Test
    void testInstallFreeIpaHaRepairTwoInstance() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        GatewayConfig newReplica1Config = mock(GatewayConfig.class);
        GatewayConfig newReplica2Config = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Node newReplica1Node = mock(Node.class);
        Node newReplica2Node = mock(Node.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(newReplica1Node.getHostname()).thenReturn("new_replica1.example.com");
        when(newReplica2Node.getHostname()).thenReturn("new_replica2.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        when(newReplica1Config.getHostname()).thenReturn("new_replica1.example.com");
        when(newReplica2Config.getHostname()).thenReturn("new_replica2.example.com");
        when(saltStateService.getGrains(any(), any(), any()))
                .thenReturn(Map.of("primary.example.com", mock(JsonNode.class)))
                .thenReturn(Map.of())
                .thenReturn(Map.of());
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);

        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway, newReplica1Config, newReplica2Config),
                Set.of(primaryNode, newReplica1Node, newReplica2Node), exitCriteriaModel);

        verify(saltRunner, times(3)).runnerWithCalculatedErrorCount(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("new_replica2.example.com"), jobIdTrackers.get(1).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("new_replica1.example.com"), jobIdTrackers.get(2).getSaltJobRunner().getTargetHostnames());
        Target<String> hostRoleAndTarget1 =
                new HostAndRoleTarget("freeipa_primary", Set.of("new_replica2.example.com", "primary.example.com", "new_replica1.example.com"));
        Target<String> hostRoleAndTarget2 =
                new HostAndRoleTarget("freeipa_primary_replacement", Set.of("new_replica2.example.com", "primary.example.com", "new_replica1.example.com"));
        Target<String> hostRoleAndTarget3 =
                new HostAndRoleTarget("freeipa_replica", Set.of("new_replica2.example.com", "primary.example.com", "new_replica1.example.com"));
        verify(saltStateService).getGrains(eq(saltConnector), eq(hostRoleAndTarget1), eq("roles"));
        verify(saltStateService).getGrains(eq(saltConnector), eq(hostRoleAndTarget2), eq("roles"));
        verify(saltStateService).getGrains(eq(saltConnector), eq(hostRoleAndTarget3), eq("roles"));
    }

    @Test
    void testExistingIsPrimaryReplacement() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        GatewayConfig replica1 = mock(GatewayConfig.class);
        GatewayConfig replica2 = mock(GatewayConfig.class);
        Node replicaNode1 = mock(Node.class);
        Node replicaNode2 = mock(Node.class);

        when(replica1.getHostname()).thenReturn("replica1.domain");
        when(replica2.getHostname()).thenReturn("replica2.domain");
        when(replicaNode1.getHostname()).thenReturn("replica1.domain");
        when(replicaNode2.getHostname()).thenReturn("replica2.domain");

        HostAndRoleTarget replicaTarget = new HostAndRoleTarget("freeipa_replica", Set.of("replica1.domain", "replica2.domain"));
        ArrayNode replicaRole = mapper.createArrayNode();
        replicaRole.add("freeipa_replica");
        when(saltStateService.getGrains(eq(saltConnector), eq(replicaTarget), eq("roles")))
                .thenReturn(Map.of("replica1.domain", replicaRole,
                        "replica2.domain", replicaRole));

        HostAndRoleTarget freeIpaReplacementTarget =
                new HostAndRoleTarget("freeipa_primary_replacement", Set.of("replica1.domain", "replica2.domain"));
        replicaRole.add("freeipa_primary_replacement");
        when(saltStateService.getGrains(eq(saltConnector), eq(freeIpaReplacementTarget), eq("roles")))
                .thenReturn(Map.of());

        HostAndRoleTarget freeIpaMasterTarget = new HostAndRoleTarget("freeipa_primary", Set.of("replica1.domain", "replica2.domain"));
        replicaRole.add("freeipa_primary");
        when(saltStateService.getGrains(eq(saltConnector), eq(freeIpaMasterTarget), eq("roles")))
                .thenReturn(Map.of());

        saltOrchestrator.installFreeIpa(replica1, List.of(replica1, replica2), Set.of(replicaNode1, replicaNode2), exitCriteriaModel);

        ArgumentCaptor<ModifyGrainBase> modifyGrainCaptor = ArgumentCaptor.forClass(ModifyGrainBase.class);
        verify(saltCommandRunner, times(3)).runModifyGrainCommand(eq(saltConnector), modifyGrainCaptor.capture(), eq(exitCriteriaModel), eq(exitCriteria));
        List<ModifyGrainBase> modifyGrains = modifyGrainCaptor.getAllValues();
        GrainAddRunner addReplacementRole = (GrainAddRunner) modifyGrains.get(0);
        assertEquals("freeipa_primary_replacement", addReplacementRole.getValue());
        assertEquals(Set.of("replica1.domain"), addReplacementRole.getTargetHostnames());
        GrainRemoveRunner removeReplicaRole = (GrainRemoveRunner) modifyGrains.get(1);
        assertEquals("freeipa_replica", removeReplicaRole.getValue());
        assertEquals(Set.of("replica1.domain"), removeReplicaRole.getTargetHostnames());

        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);
        verify(saltRunner, times(3)).runnerWithCalculatedErrorCount(saltJobIdTrackerCaptor.capture(), eq(exitCriteria),
                eq(exitCriteriaModel), anyInt());
        List<SaltJobIdTracker> saltJobIdTrackers = saltJobIdTrackerCaptor.getAllValues();
        HighStateRunner highStateRunner = (HighStateRunner) saltJobIdTrackers.get(0).getSaltJobRunner();
        assertTrue(highStateRunner.getTargetHostnames().contains("replica1.domain")
                || highStateRunner.getTargetHostnames().contains("replica2.domain"));
        assertEquals(1, highStateRunner.getTargetHostnames().size());
        HighStateRunner highStateRunner2 = (HighStateRunner) saltJobIdTrackers.get(1).getSaltJobRunner();
        assertTrue(highStateRunner2.getTargetHostnames().contains("replica2.domain")
                || highStateRunner2.getTargetHostnames().contains("replica1.domain"));
        assertEquals(1, highStateRunner2.getTargetHostnames().size());
        assertFalse(highStateRunner.getTargetHostnames().containsAll(highStateRunner2.getTargetHostnames()));
        HighStateRunner highStateRunner3 = (HighStateRunner) saltJobIdTrackers.get(2).getSaltJobRunner();
        assertTrue(highStateRunner3.getTargetHostnames().contains("replica1.domain"));
        assertEquals(1, highStateRunner3.getTargetHostnames().size());
    }

    @Test
    void testNewInstanceIsPrimaryReplacement() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        GatewayConfig newInstance = mock(GatewayConfig.class);
        GatewayConfig replica2 = mock(GatewayConfig.class);
        Node newInstanceNode = mock(Node.class);
        Node replicaNode2 = mock(Node.class);

        when(newInstance.getHostname()).thenReturn("newInstance.domain");
        when(replica2.getHostname()).thenReturn("replica2.domain");
        when(newInstanceNode.getHostname()).thenReturn("newInstance.domain");
        when(replicaNode2.getHostname()).thenReturn("replica2.domain");

        HostAndRoleTarget replicaTarget = new HostAndRoleTarget("freeipa_replica", Set.of("newInstance.domain", "replica2.domain"));
        ArrayNode replicaRole = mapper.createArrayNode();
        replicaRole.add("freeipa_replica");
        when(saltStateService.getGrains(eq(saltConnector), eq(replicaTarget), eq("roles")))
                .thenReturn(Map.of("replica2.domain", replicaRole));

        HostAndRoleTarget freeIpaReplacementTarget =
                new HostAndRoleTarget("freeipa_primary_replacement", Set.of("newInstance.domain", "replica2.domain"));
        replicaRole.add("freeipa_primary_replacement");
        when(saltStateService.getGrains(eq(saltConnector), eq(freeIpaReplacementTarget), eq("roles")))
                .thenReturn(Map.of());

        HostAndRoleTarget freeIpaMasterTarget = new HostAndRoleTarget("freeipa_primary", Set.of("newInstance.domain", "replica2.domain"));
        replicaRole.add("freeipa_primary");
        when(saltStateService.getGrains(eq(saltConnector), eq(freeIpaMasterTarget), eq("roles")))
                .thenReturn(Map.of());

        saltOrchestrator.installFreeIpa(newInstance, List.of(newInstance, replica2), Set.of(newInstanceNode, replicaNode2), exitCriteriaModel);

        ArgumentCaptor<ModifyGrainBase> modifyGrainCaptor = ArgumentCaptor.forClass(ModifyGrainBase.class);
        verify(saltCommandRunner, times(3)).runModifyGrainCommand(eq(saltConnector), modifyGrainCaptor.capture(), eq(exitCriteriaModel), eq(exitCriteria));
        List<ModifyGrainBase> modifyGrains = modifyGrainCaptor.getAllValues();
        GrainAddRunner addReplacementRole = (GrainAddRunner) modifyGrains.get(0);
        assertEquals("freeipa_primary_replacement", addReplacementRole.getValue());
        assertEquals(Set.of("newInstance.domain"), addReplacementRole.getTargetHostnames());
        GrainRemoveRunner removeReplicaRole = (GrainRemoveRunner) modifyGrains.get(1);
        assertEquals("freeipa_replica", removeReplicaRole.getValue());
        assertEquals(Set.of("newInstance.domain"), removeReplicaRole.getTargetHostnames());

        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);
        verify(saltRunner, times(2)).runnerWithCalculatedErrorCount(saltJobIdTrackerCaptor.capture(), eq(exitCriteria), eq(exitCriteriaModel), anyInt());
        List<SaltJobIdTracker> saltJobIdTrackers = saltJobIdTrackerCaptor.getAllValues();
        HighStateRunner highStateRunner = (HighStateRunner) saltJobIdTrackers.get(0).getSaltJobRunner();
        assertTrue(highStateRunner.getTargetHostnames().contains("replica2.domain"));
        assertEquals(1, highStateRunner.getTargetHostnames().size());
        HighStateRunner highStateRunner2 = (HighStateRunner) saltJobIdTrackers.get(1).getSaltJobRunner();
        assertTrue(highStateRunner2.getTargetHostnames().contains("newInstance.domain"));
        assertEquals(1, highStateRunner2.getTargetHostnames().size());
    }

    @Test
    void testRemoveDeadSaltMinions() throws Exception {
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        List<String> upNodes = Lists.newArrayList("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com");
        minionStatus.setUp(upNodes);
        List<String> downNodes = Lists.newArrayList("10-0-0-4.example.com", "10-0-0-5.example.com");
        minionStatus.setDown(downNodes);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        when(saltStateService.collectNodeStatus(eq(saltConnector))).thenReturn(minionStatusSaltResponse);
        saltOrchestrator.removeDeadSaltMinions(gatewayConfig);
        verify(saltConnector, times(1)).wheel("key.delete", downNodes, Object.class);
    }

    @Test
    void testDontRemoveDeadSaltMinions() throws Exception {
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        List<String> upNodes = Lists.newArrayList("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com");
        minionStatus.setUp(upNodes);
        List<String> downNodes = new ArrayList<>();
        minionStatus.setDown(downNodes);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        when(saltStateService.collectNodeStatus(eq(saltConnector))).thenReturn(minionStatusSaltResponse);
        saltOrchestrator.removeDeadSaltMinions(gatewayConfig);
        verify(saltConnector, never()).wheel("key.delete", downNodes, Object.class);
    }

    @Test
    void testCannotRemoveDeadMinions() {
        when(saltStateService.collectNodeStatus(eq(saltConnector))).thenThrow(new RuntimeException("connection failed"));
        assertThrows(CloudbreakOrchestratorFailedException.class, () ->
                saltOrchestrator.removeDeadSaltMinions(gatewayConfig));
    }

    @Test
    void testUploadStates() throws Exception {
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);
        saltOrchestrator.uploadStates(allGatewayConfigs, exitCriteriaModel);
        ArgumentCaptor<SaltUpload> saltUploadCaptor = ArgumentCaptor.forClass(SaltUpload.class);
        verify(saltRunner).runnerWithConfiguredErrorCount(saltUploadCaptor.capture(), eq(exitCriteria), eq(exitCriteriaModel));
        verify(callable).call();
        SaltUpload saltUpload = saltUploadCaptor.getValue();
        assertEquals(Set.of(gatewayConfig.getPrivateAddress()), saltUpload.getTargets());
    }

    @Test
    void testRunCommandOnHosts() throws Exception {
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);
        Map<String, String> response = new HashMap<>();
        response.put("host1", "sample");
        String command = "echo sample";
        when(saltStateService.runCommandOnHosts(eq(retry), eq(saltConnector), any(), eq(command))).thenReturn(response);
        Map<String, String> result = saltOrchestrator.runCommandOnHosts(allGatewayConfigs,
                targets.stream().map(Node::getHostname).collect(Collectors.toSet()), command);
        assertEquals("sample", result.get("host1"));
        verify(saltStateService).runCommandOnHosts(eq(retry), eq(saltConnector), targetCaptor.capture(), eq(command));
        Target<String> target = targetCaptor.getValue();
        assertEquals("10-0-0-1.example.com,10-0-0-2.example.com,10-0-0-3.example.com", target.getTarget());
    }

    @Test
    void testFormatAndMountDisksAfterModifyingVolumesOnNodes() throws Exception {
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);
        Map<String, String> response = new HashMap<>();
        response.put("10-0-0-2.example.com", "uuid");
        Map<String, String> responseFstab = new HashMap<>();
        responseFstab.put("10-0-0-2.example.com", "fstab");
        when(saltStateService.runCommandOnHosts(any(), any(), any(), any())).thenReturn(responseFstab);
        when(saltStateService.getUuidList(eq(saltConnector))).thenReturn(response);
        OrchestratorAware stack = mock(OrchestratorAware.class);
        Map<String, Map<String, String>> result = saltOrchestrator.formatAndMountDisksAfterModifyingVolumesOnNodes(allGatewayConfigs,
                targets, targets, exitCriteriaModel);
        assertEquals("fstab", result.get("10-0-0-2.example.com").get("fstab"));
        verify(saltStateService).runCommandOnHosts(any(), any(), targetCaptor.capture(), any());
        verify(saltStateService).getUuidList(eq(saltConnector));
        Target<String> target = targetCaptor.getValue();
        assertEquals("10-0-0-1.example.com,10-0-0-2.example.com,10-0-0-3.example.com", target.getTarget());
    }

    @Test
    void testGetPasswordExpiryDate() throws Exception {
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);
        String user = "saltuser";
        when(saltStateService.runCommandOnHosts(any(), any(), any(), anyString())).thenReturn(Map.of(
                "host1", " Jan 01, 2022",
                "host2", " Mar 10, 2022",
                "host3", " never"
        ));

        LocalDate result = saltOrchestrator.getPasswordExpiryDate(allGatewayConfigs, user);

        ArgumentCaptor<HostList> hostListCaptor = ArgumentCaptor.forClass(HostList.class);
        verify(saltStateService).runCommandOnHosts(eq(retry), eq(saltConnector), hostListCaptor.capture(), startsWith("chage -l saltuser"));
        assertEquals(gatewayConfig.getHostname(), hostListCaptor.getValue().getTarget());
        assertEquals(2022, result.getYear());
        assertEquals(Month.JANUARY, result.getMonth());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void testCreateCronForUserHomeCreationWhenStateExists() throws Exception {
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);
        when(saltStateService.stateSlsExists(any(), any(), eq("cloudera.createuserhome"))).thenReturn(Boolean.TRUE);
        saltOrchestrator.createCronForUserHomeCreation(allGatewayConfigs, Set.of("fqdn"), exitCriteriaModel);

        verify(callable).call();
        ArgumentCaptor<OrchestratorBootstrap> saltJobIdTrackerCaptor = ArgumentCaptor.forClass(OrchestratorBootstrap.class);
        verify(saltRunner, times(1)).runnerWithConfiguredErrorCount(saltJobIdTrackerCaptor.capture(), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        assertThat(saltJobIdTrackerCaptor.getValue()).isInstanceOf(SaltJobIdTracker.class);
        SaltJobIdTracker saltJobIdTracker = (SaltJobIdTracker) saltJobIdTrackerCaptor.getValue();
        SaltJobRunner saltJobRunner = saltJobIdTracker.getSaltJobRunner();
        assertEquals(Set.of("fqdn"), saltJobRunner.getTargetHostnames());
        assertThat(saltJobRunner).isInstanceOf(StateRunner.class);
        StateRunner stateRunner = (StateRunner) saltJobRunner;
        assertEquals("cloudera.createuserhome", stateRunner.getState());
    }

    @Test
    void testCreateCronForUserHomeCreationWhenStateNotExists() throws Exception {
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);
        when(saltStateService.stateSlsExists(any(), any(), eq("cloudera.createuserhome"))).thenReturn(Boolean.FALSE);
        saltOrchestrator.createCronForUserHomeCreation(allGatewayConfigs, Set.of("fqdn"), exitCriteriaModel);

        verify(callable, never()).call();
        verify(saltRunner, never()).runner(any(), any(ExitCriteria.class), any(ExitCriteriaModel.class));
    }

    @Test
    void testStartClusterManagerWithItsAgents() throws Exception {
        // GIVEN In setup
        // WHEN
        saltOrchestrator.startClusterManagerWithItsAgents(gatewayConfig, targets, exitCriteriaModel);
        // THEN
        ArgumentCaptor<ModifyGrainBase> roleCaptor = ArgumentCaptor.forClass(ModifyGrainBase.class);
        verify(saltCommandRunner, times(2)).runModifyGrainCommand(any(SaltConnector.class), roleCaptor.capture(),
                any(ExitCriteriaModel.class), any(ExitCriteria.class));
        assertEquals("roles", roleCaptor.getAllValues().get(0).getKey());
        assertEquals("cloudera_manager_full_start", roleCaptor.getAllValues().get(0).getValue());
        assertThat(roleCaptor.getAllValues().get(0)).isInstanceOf(GrainAddRunner.class);
        assertEquals("roles", roleCaptor.getAllValues().get(1).getKey());
        assertEquals("cloudera_manager_full_start", roleCaptor.getAllValues().get(1).getValue());
        assertThat(roleCaptor.getAllValues().get(1)).isInstanceOf(GrainRemoveRunner.class);
    }

    @Test
    void testStopClusterManagerWithItsAgents() throws Exception {
        // GIVEN In setup
        // WHEN
        saltOrchestrator.stopClusterManagerWithItsAgents(gatewayConfig, targets, exitCriteriaModel);
        // THEN
        ArgumentCaptor<ModifyGrainBase> roleCaptor = ArgumentCaptor.forClass(ModifyGrainBase.class);
        verify(saltCommandRunner, times(2)).runModifyGrainCommand(any(SaltConnector.class), roleCaptor.capture(),
                any(ExitCriteriaModel.class), any(ExitCriteria.class));
        assertEquals("roles", roleCaptor.getAllValues().get(0).getKey());
        assertEquals("cloudera_manager_full_stop", roleCaptor.getAllValues().get(0).getValue());
        assertThat(roleCaptor.getAllValues().get(0)).isInstanceOf(GrainAddRunner.class);
        assertEquals("roles", roleCaptor.getAllValues().get(1).getKey());
        assertEquals("cloudera_manager_full_stop", roleCaptor.getAllValues().get(1).getValue());
        assertThat(roleCaptor.getAllValues().get(1)).isInstanceOf(GrainRemoveRunner.class);
    }

    @Test
    void testStartClusterManagerWithItsAgentsThrowsException() throws Exception {
        // GIVEN In setup
        doThrow(new Exception("errorMsg")).when(saltCommandRunner).runModifyGrainCommand(any(SaltConnector.class), any(ModifyGrainBase.class),
                any(ExitCriteriaModel.class), any(ExitCriteria.class));
        // WHEN
        assertThatThrownBy(() -> saltOrchestrator.stopClusterManagerWithItsAgents(gatewayConfig, targets, exitCriteriaModel))
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasMessage("errorMsg");
    }

    @Test
    void testApplyOrchestratorState() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        Set<String> targetHostNames = Set.of("hostname");
        String state = "state";
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setPrimaryGatewayConfig(primaryGateway);
        stateParams.setTargetHostNames(targetHostNames);
        stateParams.setState(state);

        List<Map<String, JsonNode>> responseResult = List.of(Map.of("key", mock(JsonNode.class)));
        ApplyResponse response = new ApplyResponse();
        response.setResult(responseResult);
        when(saltStateService.applyStateSync(eq(saltConnector), eq(state), any())).thenReturn(response);

        List<Map<String, JsonNode>> result = saltOrchestrator.applyOrchestratorState(stateParams);

        assertEquals(responseResult, result);
        verify(saltStateService).applyStateSync(eq(saltConnector), eq(state), targetCaptor.capture());
        assertEquals("hostname", targetCaptor.getValue().getTarget());
    }

    @Test
    void testExecuteSaltState() throws Exception {
        when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(saltConnector);
        when(saltRunner.runnerWithConfiguredErrorCount(any(), any(), any())).thenReturn(callable);
        when(callable.call()).thenReturn(true);

        saltOrchestrator.executeSaltState(gatewayConfig, null, List.of("state"), null,
                Optional.empty(), Optional.empty());

        verify(saltRunner).runnerWithConfiguredErrorCount(any(), any(), any());
    }

    @Test
    void testExecuteSaltStateWithRetry() throws Exception {
        when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(saltConnector);
        when(saltRunner.runnerWithCalculatedErrorCount(any(), any(), any(), anyInt())).thenReturn(callable);
        when(callable.call()).thenReturn(true);

        saltOrchestrator.executeSaltState(gatewayConfig, null, List.of("state"), null,
                Optional.of(3), Optional.empty());

        verify(saltRunner).runnerWithCalculatedErrorCount(any(), any(), any(), anyInt());
    }

    @Test
    void testExecuteSaltStateWithBothRetry() throws Exception {
        when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(saltConnector);
        when(saltRunner.runner(any(), any(), any(), anyInt(), anyInt())).thenReturn(callable);
        when(callable.call()).thenReturn(true);

        saltOrchestrator.executeSaltState(gatewayConfig, null, List.of("state"), null,
                Optional.of(3), Optional.of(3));

        verify(saltRunner).runner(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void testPing() throws CloudbreakOrchestratorFailedException {
        when(saltStateService.ping(any(), any())).thenReturn(new PingResponse());

        saltOrchestrator.ping(Set.of(), gatewayConfig);

        verify(saltStateService).ping(any(), any());
    }

    @Test
    void testPingIfFails() throws CloudbreakOrchestratorFailedException {
        when(saltStateService.ping(any(), any())).thenThrow(new CloudbreakServiceException("pingouch"));

        assertThrows(CloudbreakOrchestratorFailedException.class, () -> saltOrchestrator.ping(Set.of(), gatewayConfig));

        verify(saltStateService).ping(any(), any());
    }

    @Test
    void testResizeDisksOnNodes() throws Exception {
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        saltOrchestrator.resizeDisksOnNodes(Collections.singletonList(gatewayConfig), targets, targets, exitCriteriaModel);
        verify(callable, times(1)).call();
    }

    @Test
    void testResizeDisksOnNodesShouldThrowException() throws Exception {
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        when(callable.call()).thenThrow(new CloudbreakOrchestratorFailedException("TEST SALT RUNNER EXCEPTION"));
        CloudbreakOrchestratorFailedException ex = assertThrows(CloudbreakOrchestratorFailedException.class, () ->
                saltOrchestrator.resizeDisksOnNodes(Collections.singletonList(gatewayConfig), targets, targets, exitCriteriaModel));

        assertEquals("TEST SALT RUNNER EXCEPTION", ex.getMessage());
    }

    @Test
    void testDeleteStorageVolumesAndMountDisksOnNodes() throws Exception {
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        saltOrchestrator.unmountBlockStorageDisks(Collections.singletonList(gatewayConfig), targets, targets, exitCriteriaModel);
        verify(callable, times(1)).call();
        verify(saltCommandRunner, times(2)).runModifyGrainCommand(any(), any(), any(), any());
    }

    @Test
    void testDeleteStorageVolumesAndMountDisksOnNodesShouldThrowException() throws Exception {
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        when(callable.call()).thenThrow(new CloudbreakOrchestratorFailedException("TEST SALT RUNNER EXCEPTION"));
        CloudbreakOrchestratorFailedException ex = assertThrows(CloudbreakOrchestratorFailedException.class, () ->
                saltOrchestrator.unmountBlockStorageDisks(Collections.singletonList(gatewayConfig), targets, targets, exitCriteriaModel));

        assertEquals("TEST SALT RUNNER EXCEPTION", ex.getMessage());
    }

    @Test
    void testRestartClusterManagerAgents() throws CloudbreakOrchestratorException {
        saltOrchestrator.restartClusterManagerAgents(gatewayConfig, targets.stream().map(Node::getHostname).collect(Collectors.toSet()),
                exitCriteriaModel);
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);
        verify(saltRunner, times(2))
                .runnerWithConfiguredErrorCount(saltJobIdTrackerArgumentCaptor.capture(), eq(exitCriteria), eq(exitCriteriaModel));
        List<SaltJobIdTracker> allValues = saltJobIdTrackerArgumentCaptor.getAllValues();
        StateRunner stopAgentStateRunner = (StateRunner) allValues.get(0).getSaltJobRunner();
        assertEquals("cloudera.agent.agent-stop", stopAgentStateRunner.getState());
        StateRunner startAgentStateRunner = (StateRunner) allValues.get(1).getSaltJobRunner();
        assertEquals("cloudera.agent.start", startAgentStateRunner.getState());
    }

    @Test
    void testSwitchFreeIpaMasterToPrimaryGateway() throws Exception {
        // Given
        when(saltService.createSaltConnector(gatewayConfig)).thenReturn(saltConnector);

        // When
        saltOrchestrator.switchFreeIpaMasterToPrimaryGateway(gatewayConfig, targets, exitCriteriaModel);

        // Then
        verify(saltService).createSaltConnector(gatewayConfig);
        verify(saltCommandRunner, times(3)).runModifyGrainCommand(eq(saltConnector), any(ModifyGrainBase.class), eq(exitCriteriaModel), eq(exitCriteria));
        verify(saltRunner).runner(any(SaltJobIdTracker.class), eq(exitCriteria), eq(exitCriteriaModel), anyInt(), anyInt());
        verify(callable).call();
    }

    @Test
    void testSwitchFreeIpaMasterToPrimaryGatewayThrowsExceptionWhenSaltConnectorFails() throws Exception {
        // Given
        when(saltService.createSaltConnector(gatewayConfig)).thenThrow(new RuntimeException("Salt connector creation failed"));

        // When & Then
        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> saltOrchestrator.switchFreeIpaMasterToPrimaryGateway(gatewayConfig, targets, exitCriteriaModel));

        assertEquals("Salt connector creation failed", exception.getMessage());
        verify(saltService).createSaltConnector(gatewayConfig);
    }

    @Test
    void testSwitchFreeIpaMasterToPrimaryGatewayThrowsExceptionWhenGrainModificationFails() throws Exception {
        // Given
        when(saltService.createSaltConnector(gatewayConfig)).thenReturn(saltConnector);
        doThrow(new RuntimeException("Grain modification failed"))
                .when(saltCommandRunner).runModifyGrainCommand(eq(saltConnector), any(ModifyGrainBase.class), eq(exitCriteriaModel), eq(exitCriteria));

        // When & Then
        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> saltOrchestrator.switchFreeIpaMasterToPrimaryGateway(gatewayConfig, targets, exitCriteriaModel));

        assertEquals("Grain modification failed", exception.getMessage());
        verify(saltService).createSaltConnector(gatewayConfig);
    }

    @Test
    void testSwitchFreeIpaMasterToPrimaryGatewayThrowsExceptionWhenStateExecutionFails() throws Exception {
        // Given
        when(saltService.createSaltConnector(gatewayConfig)).thenReturn(saltConnector);
        when(callable.call()).thenThrow(new RuntimeException("State execution failed"));

        // When & Then
        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> saltOrchestrator.switchFreeIpaMasterToPrimaryGateway(gatewayConfig, targets, exitCriteriaModel));

        assertEquals("State execution failed", exception.getMessage());
        verify(saltService).createSaltConnector(gatewayConfig);
        verify(saltCommandRunner, times(3)).runModifyGrainCommand(eq(saltConnector), any(ModifyGrainBase.class), eq(exitCriteriaModel), eq(exitCriteria));
    }
}
