package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.handler;

import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.SwitchFreeIpaMasterToPrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class SwitchFreeIpaMasterToPrimaryGatewayHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaNodeUtilService nodeService;

    @Mock
    private Stack stack;

    @Mock
    private GatewayConfig primaryGatewayConfig;

    @InjectMocks
    private SwitchFreeIpaMasterToPrimaryGatewayHandler underTest;

    @Test
    void testSelector() {
        String expected = EventSelectorUtil.selector(SwitchFreeIpaMasterToPrimaryGatewayEvent.class);
        String actual = underTest.selector();
        assertEquals(expected, actual);
    }

    @Test
    void testDefaultFailureEvent() {
        Exception exception = new RuntimeException("Test exception");
        SwitchFreeIpaMasterToPrimaryGatewayEvent eventData = new SwitchFreeIpaMasterToPrimaryGatewayEvent(STACK_ID);
        Event<SwitchFreeIpaMasterToPrimaryGatewayEvent> event = new Event<>(eventData);

        Selectable result = underTest.defaultFailureEvent(STACK_ID, exception, event);

        assertInstanceOf(ChangePrimaryGatewayFailureEvent.class, result);
        ChangePrimaryGatewayFailureEvent failureEvent = (ChangePrimaryGatewayFailureEvent) result;
        assertEquals(STACK_ID, failureEvent.getResourceId());
        assertEquals("Switching FreeIPA master to Primary Gateway", failureEvent.getFailedPhase());
        assertEquals(Set.of(), failureEvent.getSuccess());
        assertEquals(Map.of(), failureEvent.getFailureDetails());
        assertEquals(exception, failureEvent.getException());
    }

    @Test
    void testDoAcceptSuccess() throws Exception {
        SwitchFreeIpaMasterToPrimaryGatewayEvent eventData = new SwitchFreeIpaMasterToPrimaryGatewayEvent(STACK_ID);
        Event<SwitchFreeIpaMasterToPrimaryGatewayEvent> event = new Event<>(eventData);
        HandlerEvent<SwitchFreeIpaMasterToPrimaryGatewayEvent> handlerEvent = new HandlerEvent<>(event);

        Set<InstanceMetaData> instanceMetaDataSet = Set.of();
        Set<Node> nodes = Set.of();

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);
        when(nodeService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);

        Selectable result = underTest.doAccept(handlerEvent);

        verify(hostOrchestrator).switchFreeIpaMasterToPrimaryGateway(eq(primaryGatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));

        assertInstanceOf(StackEvent.class, result);
        StackEvent stackEvent = (StackEvent) result;
        assertEquals(CHANGE_PRIMARY_GATEWAY_SWITCH_FREEIPA_MASTER_TO_PRIMARY_GATEWAY_FINISHED_EVENT.event(), stackEvent.selector());
        assertEquals(STACK_ID, stackEvent.getResourceId());
    }

    @Test
    void testDoAcceptFailure() throws Exception {
        SwitchFreeIpaMasterToPrimaryGatewayEvent eventData = new SwitchFreeIpaMasterToPrimaryGatewayEvent(STACK_ID);
        Event<SwitchFreeIpaMasterToPrimaryGatewayEvent> event = new Event<>(eventData);
        HandlerEvent<SwitchFreeIpaMasterToPrimaryGatewayEvent> handlerEvent = new HandlerEvent<>(event);

        Set<InstanceMetaData> instanceMetaDataSet = Set.of();
        Set<Node> nodes = Set.of();
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Orchestration failed");

        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);
        when(nodeService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(nodes);
        doThrow(orchestratorException).when(hostOrchestrator)
                .switchFreeIpaMasterToPrimaryGateway(eq(primaryGatewayConfig), eq(nodes), any(StackBasedExitCriteriaModel.class));

        Selectable result = underTest.doAccept(handlerEvent);

        assertInstanceOf(ChangePrimaryGatewayFailureEvent.class, result);
        ChangePrimaryGatewayFailureEvent failureEvent = (ChangePrimaryGatewayFailureEvent) result;
        assertEquals(STACK_ID, failureEvent.getResourceId());
        assertEquals("Switching FreeIPA master to Primary Gateway", failureEvent.getFailedPhase());
        assertEquals(Set.of(), failureEvent.getSuccess());
        assertEquals(Map.of(), failureEvent.getFailureDetails());
        assertEquals(orchestratorException, failureEvent.getException());
    }
}