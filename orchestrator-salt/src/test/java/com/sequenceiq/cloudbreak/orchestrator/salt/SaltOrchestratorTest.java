package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.MineUpdateRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.ModifyGrainBase;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncAllRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;

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

    @InjectMocks
    private SaltOrchestrator saltOrchestrator;

    @Before
    public void setUp() throws Exception {
        gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "172.16.252.43", "10-0-0-1", 9443, "instanceid", "servercert", "clientcert", "clientkey",
                "saltpasswd", "saltbootpassword", "signkey", false, true, "privatekey", "publickey", null, null);
        targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com", "hg"));
        targets.add(new Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com", "hg"));
        targets.add(new Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com", "hg"));

        saltConnector = mock(SaltConnector.class);
        whenNew(SaltConnector.class).withAnyArguments().thenReturn(saltConnector);
        when(hostDiscoveryService.determineDomain("test", "test", false)).thenReturn(".example.com");
        Callable<Boolean> callable = mock(Callable.class);
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class))).thenReturn(callable);
        when(saltRunner.runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), anyInt(), anyBoolean()))
                .thenReturn(callable);
        when(saltService.createSaltConnector(any())).thenReturn(saltConnector);
        when(saltService.getPrimaryGatewayConfig(anyList())).thenReturn(gatewayConfig);
    }

    @Test
    public void bootstrapTest() throws Exception {
        whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));
        whenNew(OrchestratorBootstrapRunner.class)
                .withArguments(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), isNull(), anyInt(), anyInt(), anyInt())
                .thenReturn(mock(OrchestratorBootstrapRunner.class));

        BootstrapParams bootstrapParams = mock(BootstrapParams.class);

        saltOrchestrator.bootstrap(Collections.singletonList(gatewayConfig), targets, bootstrapParams, exitCriteriaModel);

        verify(saltRunner, times(4)).runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        // salt.zip, master_sign.pem, master_sign.pub
        verifyNew(SaltBootstrap.class, times(1)).withArguments(eq(saltConnector), eq(Collections.singletonList(gatewayConfig)), eq(targets),
                eq(bootstrapParams));
    }

    @Test
    public void bootstrapNewNodesTest() throws Exception {
        whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));
        whenNew(OrchestratorBootstrapRunner.class)
                .withArguments(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class), isNull(), anyInt(), anyInt(), anyInt())
                .thenReturn(mock(OrchestratorBootstrapRunner.class));
        BootstrapParams bootstrapParams = mock(BootstrapParams.class);

        saltOrchestrator.bootstrapNewNodes(Collections.singletonList(gatewayConfig), targets, targets, null, bootstrapParams, exitCriteriaModel);

        verify(saltRunner, times(2)).runner(any(OrchestratorBootstrap.class), any(ExitCriteria.class), any(ExitCriteriaModel.class));
        verifyNew(SaltBootstrap.class, times(1)).withArguments(eq(saltConnector),
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

        HighStateAllRunner highStateAllRunner = mock(HighStateAllRunner.class);
        whenNew(HighStateAllRunner.class).withAnyArguments().thenReturn(highStateAllRunner);

        SaltJobIdTracker saltJobIdTracker = mock(SaltJobIdTracker.class);
        whenNew(SaltJobIdTracker.class).withAnyArguments().thenReturn(saltJobIdTracker);

        MineUpdateRunner mineUpdateRunner = mock(MineUpdateRunner.class);
        whenNew(MineUpdateRunner.class).withAnyArguments().thenReturn(mineUpdateRunner);

        SaltCommandTracker mineUpdateRunnerSaltCommandTracker = mock(SaltCommandTracker.class);
        whenNew(SaltCommandTracker.class).withArguments(eq(saltConnector), eq(mineUpdateRunner)).thenReturn(mineUpdateRunnerSaltCommandTracker);

        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.getGrains(any(), any(), any())).thenReturn(new HashMap<>());

        SaltConfig saltConfig = new SaltConfig();
        saltOrchestrator.initServiceRun(Collections.singletonList(gatewayConfig), targets, saltConfig, exitCriteriaModel);
        saltOrchestrator.runService(Collections.singletonList(gatewayConfig), targets, saltConfig, exitCriteriaModel);

        Set<String> allNodes = targets.stream().map(Node::getHostname).collect(Collectors.toSet());

        // verify syncgrains command
        verifyNew(SyncAllRunner.class, times(1)).withArguments(eq(allNodes), eq(targets));

        // verify run new service
        verifyNew(HighStateAllRunner.class, atLeastOnce()).withArguments(eq(allNodes),
                eq(targets));
        verifyNew(SaltJobIdTracker.class, atLeastOnce()).withArguments(eq(saltConnector), eq(highStateAllRunner), eq(true));
        verify(saltCommandRunner, times(2)).runSaltCommand(any(SaltConnector.class), any(BaseSaltJobRunner.class),
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

        mockStatic(SaltStates.class);
        SaltStates.stopMinions(eq(saltConnector), eq(privateIpsByFQDN));
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

        saltOrchestrator.tearDown(Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(), null);

        verify(saltConnector, times(1)).wheel(eq("key.delete"), eq(downNodes), eq(Object.class));
        verifyStatic(SaltStates.class);

        SaltStates.stopMinions(eq(saltConnector), eq(privateIpsByFQDN));
    }

    @Test
    public void tearDownFailTest() throws Exception {
        Map<String, String> privateIpsByFQDN = new HashMap<>();
        privateIpsByFQDN.put("10-0-0-1.example.com", "10.0.0.1");
        privateIpsByFQDN.put("10-0-0-2.example.com", "10.0.0.2");
        privateIpsByFQDN.put("10-0-0-3.example.com", "10.0.0.3");

        mockStatic(SaltStates.class);
        PowerMockito.doThrow(new NullPointerException()).when(SaltStates.class);
        SaltStates.stopMinions(eq(saltConnector), eq(privateIpsByFQDN));

        try {
            saltOrchestrator.tearDown(Collections.singletonList(gatewayConfig), privateIpsByFQDN, Set.of(), null);
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
}