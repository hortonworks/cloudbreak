package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestrationNode;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.CmAgentStopFlags;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainRemoveRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.MineUpdateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ModifyGrainBase;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.CompressUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SaltOrchestrator.class, SaltStates.class})
public class SaltOrchestratorTest {

    private GatewayConfig gatewayConfig;

    private Set<Node> targets;

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

    @InjectMocks
    private SaltOrchestrator saltOrchestrator;

    private List<SaltConnector> saltConnectors;

    @Before
    public void setUp() throws Exception {
        gatewayConfig = new GatewayConfig("172.16.252.43", "1.1.1.1", "10.0.0.1", "10-0-0-1", 9443, "instanceid", "servercert", "clientcert", "clientkey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, "privatekey", "publickey", null, null);
        targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", "1.1.1.1", "instanceid1", "hg", "10-0-0-1.example.com", "hg"));
        targets.add(new Node("10.0.0.2", "1.1.1.2", "instanceid2", "hg", "10-0-0-2.example.com", "hg"));
        targets.add(new Node("10.0.0.3", "1.1.1.3", "instanceid3", "hg", "10-0-0-3.example.com", "hg"));

        saltConnector = mock(SaltConnector.class);
        whenNew(SaltConnector.class).withAnyArguments().thenReturn(saltConnector);
        when(hostDiscoveryService.determineDomain("test", "test", false)).thenReturn(".example.com");
        Callable<Boolean> callable = mock(Callable.class);
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyInt(), anyBoolean()))
                .thenReturn(callable);
        when(saltService.createSaltConnector(any(GatewayConfig.class))).thenReturn(saltConnector);
        saltConnectors = List.of(saltConnector);
        when(saltService.createSaltConnector(anyCollection())).thenReturn(saltConnectors);
        when(saltService.getPrimaryGatewayConfig(anyList())).thenReturn(gatewayConfig);
    }

    @Test
    public void bootstrapTest() throws Exception {
        whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));
        whenNew(OrchestratorBootstrapRunner.class)
                .withArguments(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), isNull(), anyInt(), anyInt(), anyInt())
                .thenReturn(mock(OrchestratorBootstrapRunner.class));
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "salt")).thenReturn(new byte[]{});

        BootstrapParams bootstrapParams = mock(BootstrapParams.class);
        List<GatewayConfig> allGatewayConfigs = Collections.singletonList(gatewayConfig);

        saltOrchestrator.bootstrap(allGatewayConfigs, targets, bootstrapParams, exitCriteriaModel);

        verify(saltRunner, times(4)).runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        // salt.zip, master_sign.pem, master_sign.pub
        verifyNew(SaltBootstrap.class, times(1)).withArguments(eq(saltConnector), eq(saltConnectors), eq(allGatewayConfigs), eq(targets),
                eq(bootstrapParams));
    }

    @Test
    public void bootstrapNewNodesTest() throws Exception {
        whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));
        whenNew(OrchestratorBootstrapRunner.class)
                .withArguments(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), isNull(), anyInt(), anyInt(), anyInt())
                .thenReturn(mock(OrchestratorBootstrapRunner.class));
        BootstrapParams bootstrapParams = mock(BootstrapParams.class);
        when(compressUtil.generateCompressedOutputFromFolders("salt-common", "salt")).thenReturn(new byte[]{});

        saltOrchestrator.bootstrapNewNodes(Collections.singletonList(gatewayConfig), targets, targets, null, bootstrapParams, exitCriteriaModel);

        verify(saltRunner, times(4)).runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        verifyNew(SaltBootstrap.class, times(1)).withArguments(eq(saltConnector), eq(saltConnectors),
                eq(Collections.singletonList(gatewayConfig)), eq(targets), eq(bootstrapParams));
    }

    @Test
    public void runServiceTest() throws Exception {
        whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));
        whenNew(OrchestratorBootstrapRunner.class)
                .withArguments(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), isNull(), anyInt(), anyInt(), anyInt())
                .thenReturn(mock(OrchestratorBootstrapRunner.class));
        PillarSave pillarSave = mock(PillarSave.class);
        whenNew(PillarSave.class).withAnyArguments().thenReturn(pillarSave);

        GrainAddRunner addRemoveGrainRunner = mock(GrainAddRunner.class);
        whenNew(GrainAddRunner.class).withAnyArguments().thenReturn(addRemoveGrainRunner);

        SaltCommandTracker roleCheckerSaltCommandTracker = mock(SaltCommandTracker.class);
        whenNew(SaltCommandTracker.class).withArguments(eq(saltConnector), eq(addRemoveGrainRunner)).thenReturn(roleCheckerSaltCommandTracker);

        SyncAllRunner syncAllRunner = mock(SyncAllRunner.class);
        whenNew(SyncAllRunner.class).withAnyArguments().thenReturn(syncAllRunner);

        SaltCommandTracker syncGrainsCheckerSaltCommandTracker = mock(SaltCommandTracker.class);
        whenNew(SaltCommandTracker.class).withArguments(eq(saltConnector), eq(syncAllRunner)).thenReturn(syncGrainsCheckerSaltCommandTracker);

        HighStateRunner highStateRunner = mock(HighStateRunner.class);
        whenNew(HighStateRunner.class).withAnyArguments().thenReturn(highStateRunner);

        SaltJobIdTracker saltJobIdTracker = mock(SaltJobIdTracker.class);
        whenNew(SaltJobIdTracker.class).withAnyArguments().thenReturn(saltJobIdTracker);

        MineUpdateRunner mineUpdateRunner = mock(MineUpdateRunner.class);
        whenNew(MineUpdateRunner.class).withAnyArguments().thenReturn(mineUpdateRunner);

        SaltCommandTracker mineUpdateRunnerSaltCommandTracker = mock(SaltCommandTracker.class);
        whenNew(SaltCommandTracker.class).withArguments(eq(saltConnector), eq(mineUpdateRunner)).thenReturn(mineUpdateRunnerSaltCommandTracker);

        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any())).thenReturn(new HashMap<>());

        Set<String> allNodes = targets.stream().map(Node::getHostname).collect(Collectors.toSet());

        SaltConfig saltConfig = new SaltConfig();
        saltOrchestrator.initServiceRun(() -> Set.of(), Collections.singletonList(gatewayConfig), targets, targets,
                saltConfig, exitCriteriaModel, "testPlatform");

        verifyNew(SyncAllRunner.class, times(1)).withArguments(eq(allNodes), eq(targets));

        saltOrchestrator.runService(Collections.singletonList(gatewayConfig), targets, saltConfig, exitCriteriaModel);

        // verify syncgrains command
        verifyNew(SyncAllRunner.class, times(2)).withArguments(eq(allNodes), eq(targets));

        // verify run new service
        verifyNew(HighStateRunner.class, atLeastOnce()).withArguments(eq(allNodes),
                eq(targets));
        verifyNew(SaltJobIdTracker.class, atLeastOnce()).withArguments(eq(saltConnector), eq(highStateRunner), eq(true));
        verify(saltCommandRunner, times(1)).runSaltCommand(any(SaltConnector.class), any(BaseSaltJobRunner.class),
                any(ExitCriteriaModel.class), any(ExitCriteria.class));
        verify(saltCommandRunner, times(2)).runModifyGrainCommand(any(SaltConnector.class), any(ModifyGrainBase.class),
                any(ExitCriteriaModel.class), any(ExitCriteria.class));
        verify(grainUploader, times(1)).uploadGrains(anySet(), anyList(), any(ExitCriteriaModel.class), any(SaltConnector.class),
                any(ExitCriteria.class));
    }

    @Test
    public void tearDownTest() throws Exception {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");
        Set<String> privateIps = privateIpsByFQDN.values().stream().collect(Collectors.toSet());

        mockStatic(SaltStates.class);
        SaltStates.stopMinions(eq(saltConnector), eq(privateIps));
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        List<String> upNodes = Lists.newArrayList("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com");
        minionStatus.setUp(upNodes);
        List<String> downNodes = Lists.newArrayList("10-0-0-4.example.com", "10-0-0-5.example.com");
        minionStatus.setDown(downNodes);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        when(SaltStates.collectNodeStatus(eq(saltConnector))).thenReturn(minionStatusSaltResponse);

        saltOrchestrator.tearDown(null, Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(), null);

        verify(saltConnector, never()).wheel(eq("key.delete"), eq(downNodes), eq(Object.class));
        verifyStatic(SaltStates.class);

        SaltStates.stopMinions(eq(saltConnector), eq(privateIps));
    }

    @Test
    public void tearDownReusedIpAddressTest() throws Exception {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");
        Set<String> privateIps = privateIpsByFQDN.values().stream().collect(Collectors.toSet());

        mockStatic(SaltStates.class);
        SaltStates.stopMinions(eq(saltConnector), eq(privateIps));
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        List<String> upNodes = Lists.newArrayList("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com");
        minionStatus.setUp(upNodes);
        List<String> downNodes = Lists.newArrayList("10-0-0-4.example.com", "10-0-0-5.example.com");
        minionStatus.setDown(downNodes);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        when(SaltStates.collectNodeStatus(eq(saltConnector))).thenReturn(minionStatusSaltResponse);
        Callable<Boolean> callable = mock(Callable.class);
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);

        Node remainingNode = mock(Node.class);
        when(remainingNode.getPrivateIp()).thenReturn("10.0.0.1");
        when(remainingNode.getHostname()).thenReturn("hostname");
        ExitCriteriaModel exitCriteriaModel = mock(ExitCriteriaModel.class);

        saltOrchestrator.tearDown(() -> Set.of(), Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(remainingNode), exitCriteriaModel);

        verify(saltConnector, times(1)).wheel(eq("key.delete"), eq(downNodes), eq(Object.class));
        verifyStatic(SaltStates.class);

        SaltStates.stopMinions(eq(saltConnector), eq(Set.of("10.0.0.2", "10.0.0.3")));
    }

    @Test
    public void tearDownFailTest() throws Exception {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");
        Set<String> privateIps = privateIpsByFQDN.values().stream().collect(Collectors.toSet());

        mockStatic(SaltStates.class);
        PowerMockito.doThrow(new NullPointerException()).when(SaltStates.class);
        SaltStates.stopMinions(eq(saltConnector), eq(privateIps));

        try {
            saltOrchestrator.tearDown(null, Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(), null);
            fail();
        } catch (CloudbreakOrchestratorFailedException e) {
            assertThat(e.getCause(), IsInstanceOf.instanceOf(NullPointerException.class));
        }
    }

    @Test
    public void getMissingNodesTest() {
        assertThat(saltOrchestrator.getMissingNodes(gatewayConfig, targets), hasSize(0));
    }

    @Test
    public void getAvailableNodesTest() {
        assertThat(saltOrchestrator.getAvailableNodes(gatewayConfig, targets), hasSize(0));
    }

    @Test
    public void isBootstrapApiAvailableTest() {
        GenericResponse response = new GenericResponse();
        response.setStatusCode(200);
        when(saltConnector.health()).thenReturn(response);

        boolean bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig);
        assertTrue(bootstrapApiAvailable);
    }

    @Test
    public void isBootstrapApiAvailableFailTest() {
        GenericResponse response = new GenericResponse();
        response.setStatusCode(404);
        when(saltConnector.health()).thenReturn(response);

        boolean bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig);
        assertFalse(bootstrapApiAvailable);
    }

    @Test
    public void testUploadGatewayPillarShouldSaveGatewayPillarProperties() throws CloudbreakOrchestratorFailedException {
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
        Field field = ReflectionUtils.findField(PillarSave.class, "pillar");
        field.setAccessible(true);
        Pillar pillar = (Pillar) ReflectionUtils.getField(field, capturedPillarSave);
        Assert.assertEquals(gatewayPillarPath, pillar.getPath());
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void testUploadGatewayPillarShouldThrowExceptionWhenThereIsNowGatewayPillar() throws CloudbreakOrchestratorFailedException {
        SaltConfig saltConfig = mock(SaltConfig.class, RETURNS_DEEP_STUBS);
        Callable<Boolean> callable = mock(Callable.class);
        when(saltConfig.getServicePillarConfig().get("gateway")).thenReturn(null);
        when(saltRunner.runnerWithUsingErrorCount(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class)))
                .thenReturn(callable);

        saltOrchestrator.uploadGatewayPillar(Collections.singletonList(gatewayConfig), targets, exitCriteriaModel, saltConfig);
    }

    @Test
    public void testStopClusterManagerAgent() throws Exception {
        Set<Node> downscaleTargets = new HashSet<>();
        downscaleTargets.add(new Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com", "hg", "fqdn2", null));
        downscaleTargets.add(new Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com", "hg", "fqdn3", null));

        PowerMockito.mockStatic(SaltStates.class);
        Set<String> responsiveAddresses = new HashSet<>();
        responsiveAddresses.add("10.0.0.1");
        responsiveAddresses.add("10.0.0.2");
        responsiveAddresses.add("10.0.0.3");
        PowerMockito.when(SaltStates.collectMinionIpAddresses(any(), any())).thenReturn(responsiveAddresses);

        Set<OrchestrationNode> allNodes = new HashSet<>();
        allNodes.add(() -> new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com", "hg", "fqdn3", null));
        allNodes.addAll(downscaleTargets.stream().map(node -> (OrchestrationNode) () -> node).collect(Collectors.toSet()));

        Callable pillarSaveCallable = mock(Callable.class);
        when(saltRunner.runner(any(), any(), any())).thenReturn(pillarSaveCallable);
        when(saltRunner.runner(any(), any(), any(), anyInt(), anyBoolean())).thenReturn(mock(Callable.class));

        saltOrchestrator.stopClusterManagerAgent(() -> allNodes, gatewayConfig, targets, downscaleTargets,
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

        Set<String> targets = Whitebox.getInternalState(capturedPillarSave, "targets");
        assertTrue(targets.contains("10.0.0.1"));

        Pillar pillar = Whitebox.getInternalState(capturedPillarSave, "pillar");
        Map<String, Map> hosts = (Map) ((Map) pillar.getJson()).get("hosts");
        assertTrue(hosts.keySet().contains("10.0.0.1"));
        assertTrue(hosts.keySet().contains("10.0.0.2"));
        assertTrue(hosts.keySet().contains("10.0.0.3"));
    }

    @Test
    public void testInstallFreeIpa() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Callable<Boolean> callable = mock(Callable.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any()))
                .thenReturn(Map.of())
                .thenReturn(Map.of())
                .thenReturn(Map.of());
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);


        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway),
                Set.of(primaryNode), exitCriteriaModel);

        verify(saltRunner, times(1)).runner(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
    }

    @Test
    public void testInstallFreeIpaHa() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        GatewayConfig replica1Config = mock(GatewayConfig.class);
        GatewayConfig replica2Config = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Node replica1Node = mock(Node.class);
        Node replica2Node = mock(Node.class);
        Callable<Boolean> callable = mock(Callable.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(replica1Node.getHostname()).thenReturn("replica1.example.com");
        when(replica2Node.getHostname()).thenReturn("replica2.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        when(replica1Config.getHostname()).thenReturn("replica1.example.com");
        when(replica2Config.getHostname()).thenReturn("replica2.example.com");
        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any()))
                .thenReturn(Map.of())
                .thenReturn(Map.of())
                .thenReturn(Map.of());
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);


        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway, replica1Config, replica2Config),
                Set.of(primaryNode, replica1Node, replica2Node), exitCriteriaModel);

        verify(saltRunner, times(2)).runner(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("replica1.example.com", "replica2.example.com"), jobIdTrackers.get(1).getSaltJobRunner().getTargetHostnames());
    }

    @Test
    public void testInstallFreeIpaHaRepairOneInstance() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        GatewayConfig replicaConfig = mock(GatewayConfig.class);
        GatewayConfig newReplicaConfig = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Node replicaNode = mock(Node.class);
        Node newReplicaNode = mock(Node.class);
        Callable<Boolean> callable = mock(Callable.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(replicaNode.getHostname()).thenReturn("replica.example.com");
        when(newReplicaNode.getHostname()).thenReturn("new_replica.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        when(replicaConfig.getHostname()).thenReturn("replica.example.com");
        when(newReplicaConfig.getHostname()).thenReturn("new_replica.example.com");
        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any()))
                .thenReturn(Map.of("primary.example.com", mock(JsonNode.class)))
                .thenReturn(Map.of())
                .thenReturn(Map.of("replica.example.com", mock(JsonNode.class)));
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
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
    public void testInstallFreeIpaHaRepairTwoInstance() throws Exception {
        GatewayConfig primaryGateway = mock(GatewayConfig.class);
        GatewayConfig newReplica1Config = mock(GatewayConfig.class);
        GatewayConfig newReplica2Config = mock(GatewayConfig.class);
        Node primaryNode = mock(Node.class);
        Node newReplica1Node = mock(Node.class);
        Node newReplica2Node = mock(Node.class);
        Callable<Boolean> callable = mock(Callable.class);

        when(primaryNode.getHostname()).thenReturn("primary.example.com");
        when(newReplica1Node.getHostname()).thenReturn("new_replica1.example.com");
        when(newReplica2Node.getHostname()).thenReturn("new_replica2.example.com");
        when(primaryGateway.getHostname()).thenReturn("primary.example.com");
        when(newReplica1Config.getHostname()).thenReturn("new_replica1.example.com");
        when(newReplica2Config.getHostname()).thenReturn("new_replica2.example.com");
        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any()))
                .thenReturn(Map.of("primary.example.com", mock(JsonNode.class)))
                .thenReturn(Map.of())
                .thenReturn(Map.of());
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
        ArgumentCaptor<SaltJobIdTracker> saltJobIdTrackerArgumentCaptor = ArgumentCaptor.forClass(SaltJobIdTracker.class);


        saltOrchestrator.installFreeIpa(primaryGateway, List.of(primaryGateway, newReplica1Config, newReplica2Config),
                Set.of(primaryNode, newReplica1Node, newReplica2Node), exitCriteriaModel);

        verify(saltRunner, times(2)).runner(saltJobIdTrackerArgumentCaptor.capture(), any(), any(), anyInt(), anyBoolean());
        List<SaltJobIdTracker> jobIdTrackers = saltJobIdTrackerArgumentCaptor.getAllValues();
        assertEquals(Set.of("primary.example.com"), jobIdTrackers.get(0).getSaltJobRunner().getTargetHostnames());
        assertEquals(Set.of("new_replica1.example.com", "new_replica2.example.com"), jobIdTrackers.get(1).getSaltJobRunner().getTargetHostnames());
        PowerMockito.verifyStatic(SaltStates.class, times(3));
        ArgumentCaptor<Target<String>> targetArgumentCaptor = ArgumentCaptor.forClass(Target.class);
        SaltStates.getGrains(any(), targetArgumentCaptor.capture(), any());
        List<Target<String>> targets = targetArgumentCaptor.getAllValues();
        String target1 = targets.get(0).getTarget();
        assertTrue(target1.contains("primary.example.com"));
        assertTrue(target1.contains("new_replica1.example.com"));
        assertTrue(target1.contains("new_replica2.example.com"));
        String target2 = targets.get(1).getTarget();
        assertTrue(target2.contains("primary.example.com"));
        assertTrue(target2.contains("new_replica1.example.com"));
        assertTrue(target2.contains("new_replica2.example.com"));
        String target3 = targets.get(2).getTarget();
        assertTrue(target3.startsWith("G@roles:freeipa_replica and L@"));
        assertTrue(target3.contains("primary.example.com"));
        assertTrue(target3.contains("new_replica1.example.com"));
        assertTrue(target3.contains("new_replica2.example.com"));
    }

    @Test
    public void testExistingIsPrimaryReplacement() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        GatewayConfig replica1 = mock(GatewayConfig.class);
        GatewayConfig replica2 = mock(GatewayConfig.class);
        Node replicaNode1 = mock(Node.class);
        Node replicaNode2 = mock(Node.class);

        when(replica1.getHostname()).thenReturn("replica1.domain");
        when(replica2.getHostname()).thenReturn("replica2.domain");
        when(replicaNode1.getHostname()).thenReturn("replica1.domain");
        when(replicaNode2.getHostname()).thenReturn("replica2.domain");

        PowerMockito.mockStatic(SaltStates.class);
        HostAndRoleTarget replicaTarget = new HostAndRoleTarget("freeipa_replica", Set.of("replica1.domain", "replica2.domain"));
        ArrayNode replicaRole = mapper.createArrayNode();
        replicaRole.add("freeipa_replica");
        PowerMockito.when(SaltStates.getGrains(eq(saltConnector), eq(replicaTarget), eq("roles")))
                .thenReturn(Map.of("replica1.domain", replicaRole,
                        "replica2.domain", replicaRole));

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
    public void testNewInstanceIsPrimaryReplacement() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        GatewayConfig newInstance = mock(GatewayConfig.class);
        GatewayConfig replica2 = mock(GatewayConfig.class);
        Node newInstanceNode = mock(Node.class);
        Node replicaNode2 = mock(Node.class);

        when(newInstance.getHostname()).thenReturn("newInstance.domain");
        when(replica2.getHostname()).thenReturn("replica2.domain");
        when(newInstanceNode.getHostname()).thenReturn("newInstance.domain");
        when(replicaNode2.getHostname()).thenReturn("replica2.domain");

        PowerMockito.mockStatic(SaltStates.class);
        HostAndRoleTarget replicaTarget = new HostAndRoleTarget("freeipa_replica", Set.of("newInstance.domain", "replica2.domain"));
        ArrayNode replicaRole = mapper.createArrayNode();
        replicaRole.add("freeipa_replica");
        PowerMockito.when(SaltStates.getGrains(eq(saltConnector), eq(replicaTarget), eq("roles")))
                .thenReturn(Map.of("replica2.domain", replicaRole));

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
    public void testRemoveDeadSaltMinions() throws Exception {
        PowerMockito.mockStatic(SaltStates.class);
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        List<String> upNodes = Lists.newArrayList("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com");
        minionStatus.setUp(upNodes);
        List<String> downNodes = Lists.newArrayList("10-0-0-4.example.com", "10-0-0-5.example.com");
        minionStatus.setDown(downNodes);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        when(SaltStates.collectNodeStatus(eq(saltConnector))).thenReturn(minionStatusSaltResponse);
        saltOrchestrator.removeDeadSaltMinions(gatewayConfig);
        verify(saltConnector, times(1)).wheel("key.delete", downNodes, Object.class);
    }

    @Test
    public void testDontRemoveDeadSaltMinions() throws Exception {
        PowerMockito.mockStatic(SaltStates.class);
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        List<String> upNodes = Lists.newArrayList("10-0-0-1.example.com", "10-0-0-2.example.com", "10-0-0-3.example.com");
        minionStatus.setUp(upNodes);
        List<String> downNodes = new ArrayList<>();
        minionStatus.setDown(downNodes);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        when(SaltStates.collectNodeStatus(eq(saltConnector))).thenReturn(minionStatusSaltResponse);
        saltOrchestrator.removeDeadSaltMinions(gatewayConfig);
        verify(saltConnector, never()).wheel("key.delete", downNodes, Object.class);
    }

    @Test(expected = CloudbreakOrchestratorFailedException.class)
    public void testCannotRemoveDeadMinions() throws Exception {
        PowerMockito.mockStatic(SaltStates.class);

        when(SaltStates.collectNodeStatus(eq(saltConnector))).thenThrow(new RuntimeException("connection failed"));
        saltOrchestrator.removeDeadSaltMinions(gatewayConfig);
    }
}