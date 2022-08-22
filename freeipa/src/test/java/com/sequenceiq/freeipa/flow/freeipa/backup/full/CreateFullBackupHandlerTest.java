package com.sequenceiq.freeipa.flow.freeipa.backup.full;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.event.CreateFullBackupEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class CreateFullBackupHandlerTest {

    @Mock
    private HostOrchestrator orchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaNodeUtilService nodeService;

    @InjectMocks
    private CreateFullBackupHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(CreateFullBackupEvent.class), underTest.selector());
    }

    @Test
    public void testDefaultFailureEvent() {
        Exception e = new Exception();

        StackFailureEvent result = (StackFailureEvent) underTest.defaultFailureEvent(2L, e, new Event<>(new CreateFullBackupEvent(3L)));

        assertEquals(2L, result.getResourceId());
        assertEquals(e, result.getException());
        assertEquals(FullBackupEvent.FULL_BACKUP_FAILED_EVENT.event(), result.selector());
    }

    @Test
    public void testBackupSuccessful() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(2L)).thenReturn(stack);
        Set<InstanceMetaData> metaDataSet = Set.of();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(metaDataSet);
        Node node1 = createNode("node1");
        Node node2 = createNode("node2");
        Set<Node> nodes = Set.of(node1, node2);
        when(nodeService.mapInstancesToNodes(metaDataSet)).thenReturn(nodes);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);

        StackEvent result = (StackEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(new CreateFullBackupEvent(2L))));

        ArgumentCaptor<OrchestratorStateParams> captor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(orchestrator, times(2)).runOrchestratorState(captor.capture());
        List<OrchestratorStateParams> stateParams = captor.getAllValues();
        assertThat(stateParams, everyItem(allOf(
                hasProperty("primaryGatewayConfig", is(gatewayConfig)),
                hasProperty("state", is("freeipa.backup-full")),
                hasProperty("allNodes", is(nodes))
                )));
        assertThat(stateParams, hasItem(hasProperty("targetHostNames", allOf(
                hasItem("node1"),
                iterableWithSize(1)
        ))));
        assertThat(stateParams, hasItem(hasProperty("targetHostNames", allOf(
                hasItem("node2"),
                iterableWithSize(1)
        ))));

        assertEquals(2L, result.getResourceId());
        assertEquals(FullBackupEvent.FULL_BACKUP_SUCCESSFUL_EVENT.event(), result.selector());
    }

    private Node createNode(String hostname) {
        Node node = mock(Node.class);
        when(node.getHostname()).thenReturn(hostname);
        return node;
    }

    @Test
    public void testOrchestratorThrowException() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(2L)).thenReturn(stack);
        Set<InstanceMetaData> metaDataSet = Set.of();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(metaDataSet);
        Node node1 = createNode("node1");
        Set<Node> nodes = Set.of(node1);
        when(nodeService.mapInstancesToNodes(metaDataSet)).thenReturn(nodes);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        doThrow(new CloudbreakOrchestratorFailedException("tada")).when(orchestrator).runOrchestratorState(any(OrchestratorStateParams.class));

        StackFailureEvent result = (StackFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(new CreateFullBackupEvent(2L))));

        assertEquals(2L, result.getResourceId());
        assertEquals(FullBackupEvent.FULL_BACKUP_FAILED_EVENT.event(), result.selector());
        assertEquals("tada", result.getException().getMessage());
    }
}