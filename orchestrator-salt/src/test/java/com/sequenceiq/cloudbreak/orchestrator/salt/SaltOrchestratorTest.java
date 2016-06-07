package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.PillarSave;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltCommandTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltJobIdTracker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.HighStateChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SimpleAddRoleChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.SyncGrainsChecker;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SaltOrchestrator.class, SaltStates.class})
public class SaltOrchestratorTest {

    private GatewayConfig gatewayConfig;
    private Set<Node> targets;
    private ExitCriteria exitCriteria;
    private ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner;
    private SaltConnector saltConnector;

    @Captor
    private ArgumentCaptor<Set<String>> ipSet;
    private ExitCriteriaModel exitCriteriaModel;

    @Before
    public void setUp() throws Exception {
        gatewayConfig = new GatewayConfig("1.1.1.1", "10.0.0.1", "10-0-0-1", 9443, "/certdir", "servercert", "clientcert", "clientkey");
        targets = new HashSet<>();
        targets.add(new Node("10.0.0.1", "1.1.1.1", "10-0-0-1.example.com"));
        targets.add(new Node("10.0.0.2", "1.1.1.2", "10-0-0-2.example.com"));
        targets.add(new Node("10.0.0.3", "1.1.1.3", "10-0-0-3.example.com"));

        saltConnector = mock(SaltConnector.class);
        PowerMockito.whenNew(SaltConnector.class).withAnyArguments().thenReturn(saltConnector);
        parallelOrchestratorComponentRunner = mock(ParallelOrchestratorComponentRunner.class);
        when(parallelOrchestratorComponentRunner.submit(any())).thenReturn(CompletableFuture.completedFuture(true));
        exitCriteria = mock(ExitCriteria.class);
        exitCriteriaModel = mock(ExitCriteriaModel.class);
    }

    @Test
    public void bootstrapTest() throws Exception {
        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);

        PowerMockito.whenNew(OrchestratorBootstrapRunner.class).withAnyArguments().thenReturn(mock(OrchestratorBootstrapRunner.class));
        PowerMockito.whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));

        saltOrchestrator.bootstrap(gatewayConfig, targets, 0, exitCriteriaModel);

        verify(parallelOrchestratorComponentRunner, times(2)).submit(any(OrchestratorBootstrapRunner.class));

        verifyNew(OrchestratorBootstrapRunner.class, times(2))
                .withArguments(any(PillarSave.class), eq(exitCriteria), eq(exitCriteriaModel), any(), anyInt(), anyInt());
        verifyNew(OrchestratorBootstrapRunner.class, times(2))
                .withArguments(any(SaltBootstrap.class), eq(exitCriteria), eq(exitCriteriaModel), any(), anyInt(), anyInt());

        Set<String> ips = new HashSet<>();
        ips.add("10.0.0.1");
        ips.add("10.0.0.2");
        ips.add("10.0.0.3");
        verifyNew(SaltBootstrap.class, times(1)).withArguments(eq(saltConnector), eq(gatewayConfig), eq(ips));
    }

    @Test
    public void bootstrapNewNodesTest() throws Exception {
        PowerMockito.whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));
        PowerMockito.whenNew(OrchestratorBootstrapRunner.class).withAnyArguments().thenReturn(mock(OrchestratorBootstrapRunner.class));

        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);

        saltOrchestrator.bootstrapNewNodes(gatewayConfig, targets, exitCriteriaModel);

        verifyNew(OrchestratorBootstrapRunner.class, times(1))
                .withArguments(any(SaltBootstrap.class), eq(exitCriteria), eq(exitCriteriaModel), any(), anyInt(), anyInt());

        Set<String> ips = new HashSet<>();
        ips.add("10.0.0.1");
        ips.add("10.0.0.2");
        ips.add("10.0.0.3");
        verifyNew(SaltBootstrap.class, times(1)).withArguments(eq(saltConnector), eq(gatewayConfig), eq(ips));
    }

    @Test
    public void runServiceTest() throws Exception {
        PowerMockito.whenNew(SaltBootstrap.class).withAnyArguments().thenReturn(mock(SaltBootstrap.class));
        PowerMockito.whenNew(OrchestratorBootstrapRunner.class).withAnyArguments().thenReturn(mock(OrchestratorBootstrapRunner.class));
        PillarSave pillarSave = mock(PillarSave.class);
        whenNew(PillarSave.class).withAnyArguments().thenReturn(pillarSave);

        SimpleAddRoleChecker simpleAddRoleChecker = mock(SimpleAddRoleChecker.class);
        whenNew(SimpleAddRoleChecker.class).withAnyArguments().thenReturn(simpleAddRoleChecker);

        SaltCommandTracker roleCheckerSaltCommandTracker = mock(SaltCommandTracker.class);
        whenNew(SaltCommandTracker.class).withArguments(eq(saltConnector), eq(simpleAddRoleChecker)).thenReturn(roleCheckerSaltCommandTracker);

        SyncGrainsChecker syncGrainsChecker = mock(SyncGrainsChecker.class);
        whenNew(SyncGrainsChecker.class).withAnyArguments().thenReturn(syncGrainsChecker);

        SaltCommandTracker syncGrainsCheckerSaltCommandTracker = mock(SaltCommandTracker.class);
        whenNew(SaltCommandTracker.class).withArguments(eq(saltConnector), eq(syncGrainsChecker)).thenReturn(syncGrainsCheckerSaltCommandTracker);

        HighStateChecker highStateChecker = mock(HighStateChecker.class);
        whenNew(HighStateChecker.class).withAnyArguments().thenReturn(highStateChecker);

        SaltJobIdTracker saltJobIdTracker = mock(SaltJobIdTracker.class);
        whenNew(SaltJobIdTracker.class).withAnyArguments().thenReturn(saltJobIdTracker);

        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);

        SaltPillarConfig saltPillarConfig = new SaltPillarConfig();
        saltOrchestrator.runService(gatewayConfig, targets, saltPillarConfig, exitCriteriaModel);

        // verify pillar save
        verifyNew(OrchestratorBootstrapRunner.class, times(1))
                .withArguments(eq(pillarSave), eq(exitCriteria), eq(exitCriteriaModel), any(), anyInt(), anyInt());

        // verify ambari server role
        verifyNew(SimpleAddRoleChecker.class, times(1)).withArguments(eq(Sets.newHashSet(gatewayConfig.getPrivateAddress())),
                eq(targets), eq("ambari_server"));

        // verify ambari agent role
        Set<String> allNodes = targets.stream().map(Node::getPrivateIp).collect(Collectors.toSet());
        verifyNew(SimpleAddRoleChecker.class, times(1)).withArguments(eq(allNodes),
                eq(targets), eq("ambari_agent"));
        // verify two role command (amabari server, ambari agent)
        verifyNew(SaltCommandTracker.class, times(2)).withArguments(eq(saltConnector), eq(simpleAddRoleChecker));
        // verify two OrchestratorBootstrapRunner call with rolechecker command tracker
        verifyNew(OrchestratorBootstrapRunner.class, times(2))
                .withArguments(eq(roleCheckerSaltCommandTracker), eq(exitCriteria), eq(exitCriteriaModel), any(), anyInt(), anyInt());

        // verify syncgrains command
        verifyNew(SyncGrainsChecker.class, times(1)).withArguments(eq(allNodes), eq(targets));
        verifyNew(SaltCommandTracker.class, times(1)).withArguments(eq(saltConnector), eq(syncGrainsChecker));
        verifyNew(OrchestratorBootstrapRunner.class, times(1))
                .withArguments(eq(syncGrainsCheckerSaltCommandTracker), eq(exitCriteria), eq(exitCriteriaModel), any(), anyInt(), anyInt());

        // verify run new service
        verifyNew(HighStateChecker.class, atLeastOnce()).withArguments(eq(allNodes),
                eq(targets));
        verifyNew(SaltJobIdTracker.class, atLeastOnce()).withArguments(eq(saltConnector), eq(highStateChecker));
    }

    @Test
    public void tearDownTest() throws Exception {
        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);

        List<String> hostNames = new ArrayList<>();
        hostNames.add("10-0-0-1.example.com");
        hostNames.add("10-0-0-1.example.com");
        hostNames.add("10-0-0-1.example.com");

        mockStatic(SaltStates.class);
        SaltStates.removeMinions(eq(saltConnector), eq(hostNames));

        saltOrchestrator.tearDown(gatewayConfig, hostNames);

        verifyStatic();
        SaltStates.removeMinions(eq(saltConnector), eq(hostNames));
    }

    @Test
    public void tearDownFailTest() throws Exception {
        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);

        List<String> hostNames = new ArrayList<>();
        hostNames.add("10-0-0-1.example.com");
        hostNames.add("10-0-0-1.example.com");
        hostNames.add("10-0-0-1.example.com");

        mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.removeMinions(eq(saltConnector), eq(hostNames))).thenThrow(new NullPointerException());

        try {
            saltOrchestrator.tearDown(gatewayConfig, hostNames);
            fail();
        } catch (CloudbreakOrchestratorFailedException e) {
            assertTrue(NullPointerException.class.isInstance(e.getCause()));
        }
    }

    @Test
    public void getMissingNodesTest() throws Exception {
        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);
        assertThat(saltOrchestrator.getMissingNodes(gatewayConfig, targets), hasSize(0));
    }

    @Test
    public void getAvailableNodesTest() throws Exception {
        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);
        assertThat(saltOrchestrator.getAvailableNodes(gatewayConfig, targets), hasSize(0));
    }

    @Test
    public void isBootstrapApiAvailableTest() throws Exception {
        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);

        GenericResponse response = new GenericResponse();
        response.setStatusCode(200);
        when(saltConnector.health()).thenReturn(response);

        boolean bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig);
        assertTrue(bootstrapApiAvailable);
    }

    @Test
    public void isBootstrapApiAvailableFailTest() throws Exception {
        SaltOrchestrator saltOrchestrator = new SaltOrchestrator();
        saltOrchestrator.init(parallelOrchestratorComponentRunner, exitCriteria);

        GenericResponse response = new GenericResponse();
        response.setStatusCode(404);
        when(saltConnector.health()).thenReturn(response);

        boolean bootstrapApiAvailable = saltOrchestrator.isBootstrapApiAvailable(gatewayConfig);
        assertFalse(bootstrapApiAvailable);
    }
}