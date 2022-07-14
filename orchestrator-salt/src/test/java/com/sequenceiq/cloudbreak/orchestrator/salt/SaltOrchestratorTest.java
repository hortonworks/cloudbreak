package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.CmAgentStopFlags;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostAndRoleTarget;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.grain.GrainUploader;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrapFactory;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUpload;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ModifyGrainBase;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@ExtendWith(MockitoExtension.class)
class SaltOrchestratorTest {

    private static final String OLD_PASSWORD = "old-password";

    private static final String NEW_PASSWORD = "new-password";

    private GatewayConfig gatewayConfig;

    private Set<Node> targets;

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

    @InjectMocks
    private SaltOrchestrator saltOrchestrator;

    private List<SaltConnector> saltConnectors;

    @BeforeEach
    void setUp() throws Exception {
        gatewayConfig = new GatewayConfig("172.16.252.43", "1.1.1.1", "10.0.0.1", "10-0-0-1", 9443, "instanceid", "servercert", "clientcert", "clientkey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, "privatekey", "publickey", null, null);
        targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", "1.1.1.1", "instanceid1", "hg", "10-0-0-1.example.com", "hg"));
        targets.add(new Node("10.0.0.2", "1.1.1.2", "instanceid2", "hg", "10-0-0-2.example.com", "hg"));
        targets.add(new Node("10.0.0.3", "1.1.1.3", "instanceid3", "hg", "10-0-0-3.example.com", "hg"));

        lenient().when(hostDiscoveryService.determineDomain("test", "test", false)).thenReturn(".example.com");
        lenient().when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
        lenient().when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyInt(), anyBoolean()))
                .thenReturn(callable);
        lenient().when(callable.call()).thenReturn(true);
        lenient().when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(saltConnector);
        saltConnectors = List.of(saltConnector);
        lenient().when(saltService.createSaltConnector(anyCollection())).thenReturn(saltConnectors);
        lenient().when(saltService.getPrimaryGatewayConfig(anyList())).thenReturn(gatewayConfig);
        lenient().when(saltBootstrapFactory.of(any(), any(), anyCollection(), anyList(), anySet(), any())).thenReturn(saltBootstrap);
    }

    @Test
    void bootstrapTest() throws Exception {
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "salt")).thenReturn(new byte[] {});

        BootstrapParams bootstrapParams = mock(BootstrapParams.class);
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);

        saltOrchestrator.bootstrap(allGatewayConfigs, targets, bootstrapParams, exitCriteriaModel);

        verify(saltRunner, times(4)).runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        // salt.zip, master_sign.pem, master_sign.pub
        verify(saltBootstrapFactory, times(1)).of(eq(saltStateService), eq(saltConnector), eq(saltConnectors), eq(allGatewayConfigs), eq(targets),
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
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "salt")).thenReturn(new byte[] {});

        saltOrchestrator.bootstrapNewNodes(Collections.singletonList(gatewayConfig), targets, targets, null, bootstrapParams, exitCriteriaModel);

        verify(saltRunner, times(4)).runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        verify(saltBootstrapFactory, times(1))
                .of(eq(saltStateService), eq(saltConnector), eq(saltConnectors), eq(Collections.singletonList(gatewayConfig)), eq(targets), eq(bootstrapParams));
    }

    @Test
    void runServiceTest() throws Exception {
        SaltConfig saltConfig = new SaltConfig();
        OrchestratorAware orchestratorAware = mock(OrchestratorAware.class);
        when(orchestratorAware.getAllNodes()).thenReturn(Set.of());
        saltOrchestrator.initServiceRun(orchestratorAware, Collections.singletonList(gatewayConfig), targets, targets,
                saltConfig, exitCriteriaModel, "testPlatform");
        saltOrchestrator.runService(Collections.singletonList(gatewayConfig), targets, saltConfig, exitCriteriaModel);

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

        saltOrchestrator.tearDown(null, Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(), null);

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
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);

        Node remainingNode = mock(Node.class);
        when(remainingNode.getPrivateIp()).thenReturn("10.0.0.1");
        ExitCriteriaModel exitCriteriaModel = mock(ExitCriteriaModel.class);
        OrchestratorAware orchestratorAware = mock(OrchestratorAware.class);
        when(orchestratorAware.getAllNodes()).thenReturn(Set.of());

        saltOrchestrator.tearDown(orchestratorAware, Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(remainingNode), exitCriteriaModel);

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
            saltOrchestrator.tearDown(null, Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(), null))
                .hasMessage("message")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void getMissingNodesTest() {
        assertThat(saltOrchestrator.getMissingNodes(gatewayConfig, targets), hasSize(0));
    }

    @Test
    void getAvailableNodesTest() {
        assertThat(saltOrchestrator.getAvailableNodes(gatewayConfig, targets), hasSize(0));
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
        when(saltRunner.runnerWithUsingErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);
        ArgumentCaptor<PillarSave> pillarSaveArgumentCaptor =
                ArgumentCaptor.forClass(PillarSave.class);

        saltOrchestrator.uploadGatewayPillar(Collections.singletonList(gatewayConfig), targets, exitCriteriaModel, saltConfig);

        verify(saltRunner).runnerWithUsingErrorCount(pillarSaveArgumentCaptor.capture(), any(ExitCriteria.class), any(ExitCriteriaModel.class));
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
        when(saltStateService.collectMinionIpAddresses(any(), any())).thenReturn(responsiveAddresses);

        Set<Node> allNodes = new HashSet<>();
        allNodes.add(new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com", "hg", "fqdn3", null));
        allNodes.addAll(downscaleTargets);

        Callable pillarSaveCallable = mock(Callable.class);
        when(saltRunner.runner(any(), any(), any())).thenReturn(pillarSaveCallable);
        when(saltRunner.runner(any(), any(), any(), anyInt(), anyBoolean())).thenReturn(mock(Callable.class));

        OrchestratorAware orchestratorAware = mock(OrchestratorAware.class);
        when(orchestratorAware.getAllNodes()).thenReturn(allNodes);
        saltOrchestrator.stopClusterManagerAgent(orchestratorAware, gatewayConfig, targets, downscaleTargets,
                exitCriteriaModel, new CmAgentStopFlags(false, false, false));

        ArgumentCaptor<ModifyGrainBase> modifyGrainBaseArgumentCaptor = ArgumentCaptor.forClass(ModifyGrainBase.class);
        ArgumentCaptor<PillarSave> pillarSaveArgumentCaptor = ArgumentCaptor.forClass(PillarSave.class);
        verify(pillarSaveCallable, times(1)).call();

        InOrder inOrder = inOrder(saltCommandRunner, saltRunner);

        inOrder.verify(saltCommandRunner).runModifyGrainCommand(any(), modifyGrainBaseArgumentCaptor.capture(), any(), any());
        ModifyGrainBase modifyGrainBase = modifyGrainBaseArgumentCaptor.getValue();
        assertThat(modifyGrainBase, instanceOf(GrainAddRunner.class));
        assertEquals("roles", modifyGrainBase.getKey());
        assertEquals("cloudera_manager_agent_stop", modifyGrainBase.getValue());

        inOrder.verify(saltRunner).runner(pillarSaveArgumentCaptor.capture(), any(), any());
        PillarSave capturedPillarSave = pillarSaveArgumentCaptor.getValue();

        ArgumentCaptor<SaltJobIdTracker> saltJobIdCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);
        inOrder.verify(saltRunner).runner(saltJobIdCaptor.capture(), any(), any(), anyInt(), anyBoolean());

        inOrder.verify(saltCommandRunner).runModifyGrainCommand(any(), modifyGrainBaseArgumentCaptor.capture(), any(), any());
        inOrder.verifyNoMoreInteractions();
        modifyGrainBase = modifyGrainBaseArgumentCaptor.getValue();
        assertThat(modifyGrainBase, instanceOf(GrainRemoveRunner.class));
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
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyInt(), anyBoolean()))
                .thenReturn(callable);
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);

        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway),
                Set.of(primaryNode), exitCriteriaModel);

        verify(saltRunner, times(1)).runner(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean());
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
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);

        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway, replica1Config, replica2Config),
                Set.of(primaryNode, replica1Node, replica2Node), exitCriteriaModel);

        verify(saltRunner, times(2)).runner(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("replica1.example.com", "replica2.example.com"), jobIdTrackers.get(1).getSaltJobRunner().getTargetHostnames());
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

        verify(saltRunner, times(3)).runner(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean());
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

        verify(saltRunner, times(2)).runner(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("new_replica1.example.com", "new_replica2.example.com"), jobIdTrackers.get(1).getSaltJobRunner().getTargetHostnames());
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
        verify(saltCommandRunner, times(2)).runModifyGrainCommand(eq(saltConnector), modifyGrainCaptor.capture(), eq(exitCriteriaModel), eq(exitCriteria));
        List<ModifyGrainBase> modifyGrains = modifyGrainCaptor.getAllValues();
        GrainAddRunner addReplacementRole = (GrainAddRunner) modifyGrains.get(0);
        assertEquals("freeipa_primary_replacement", addReplacementRole.getValue());
        assertEquals(Set.of("replica1.domain"), addReplacementRole.getTargetHostnames());
        GrainRemoveRunner removeReplicaRole = (GrainRemoveRunner) modifyGrains.get(1);
        assertEquals("freeipa_replica", removeReplicaRole.getValue());
        assertEquals(Set.of("replica1.domain"), removeReplicaRole.getTargetHostnames());

        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);
        verify(saltRunner, times(3)).runner(saltJobIdTrackerCaptor.capture(), eq(exitCriteria), eq(exitCriteriaModel), anyInt(), anyBoolean());
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
        verify(saltCommandRunner, times(2)).runModifyGrainCommand(eq(saltConnector), modifyGrainCaptor.capture(), eq(exitCriteriaModel), eq(exitCriteria));
        List<ModifyGrainBase> modifyGrains = modifyGrainCaptor.getAllValues();
        GrainAddRunner addReplacementRole = (GrainAddRunner) modifyGrains.get(0);
        assertEquals("freeipa_primary_replacement", addReplacementRole.getValue());
        assertEquals(Set.of("newInstance.domain"), addReplacementRole.getTargetHostnames());
        GrainRemoveRunner removeReplicaRole = (GrainRemoveRunner) modifyGrains.get(1);
        assertEquals("freeipa_replica", removeReplicaRole.getValue());
        assertEquals(Set.of("newInstance.domain"), removeReplicaRole.getTargetHostnames());

        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);
        verify(saltRunner, times(2)).runner(saltJobIdTrackerCaptor.capture(), eq(exitCriteria), eq(exitCriteriaModel), anyInt(), anyBoolean());
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
        verify(saltRunner).runner(saltUploadCaptor.capture(), eq(exitCriteria), eq(exitCriteriaModel));
        verify(callable).call();
        SaltUpload saltUpload = saltUploadCaptor.getValue();
        assertEquals(Set.of(gatewayConfig.getPrivateAddress()), saltUpload.getTargets());
    }
}
