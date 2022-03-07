package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.FAILED_DIAGNOSTICS_COLLECTION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.service.stack.FreeIpaNodeStatusService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class DiagnosticsPreFlightCheckHandlerTest {

    private static final Long STACK_ID = 1L;

    @InjectMocks
    private DiagnosticsPreFlightCheckHandler underTest;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private FreeIpaNodeStatusService freeIpaNodeStatusService;

    @Mock
    private RPCResponse<NodeStatusProto.NodeStatusReport> rpcResponse;

    @BeforeEach
    public void setUp() {
        underTest = new DiagnosticsPreFlightCheckHandler();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDoAccept() throws FreeIpaClientException {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(mock(Stack.class));
        given(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).willReturn(metadataSet());
        given(freeIpaNodeStatusService.nodeNetworkReport(any(), any())).willReturn(rpcResponse);
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(), STACK_ID, "crn",
                new DiagnosticParameters());
        underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        // THEN
        verify(freeIpaNodeStatusService, times(1)).nodeNetworkReport(any(), any());
    }

    @Test
    public void testDoAcceptWithClientException() throws FreeIpaClientException {
        // GIVEN
        given(stackService.getByIdWithListsInTransaction(STACK_ID)).willReturn(mock(Stack.class));
        given(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).willReturn(metadataSet());
        doThrow(new FreeIpaClientException("ex")).when(freeIpaNodeStatusService).nodeNetworkReport(any(), any());
        // WHEN
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(), STACK_ID, "crn",
                new DiagnosticParameters());
        underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        // THEN
        verify(freeIpaNodeStatusService, times(1)).nodeNetworkReport(any(), any());
    }

    @Test
    public void testFailureType() {
        assertEquals(UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET, underTest.getFailureType());
    }

    @Test
    public void testSelector() {
        assertEquals(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(), underTest.selector());
    }

    @Test
    public void testFailureEvent() {
        DiagnosticsCollectionEvent event = new DiagnosticsCollectionEvent(PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector(),
                STACK_ID, "crn", new DiagnosticParameters());
        Selectable result = underTest.defaultFailureEvent(STACK_ID, new IllegalArgumentException("ex"), new Event<>(event));
        assertEquals(FAILED_DIAGNOSTICS_COLLECTION_EVENT.selector(), result.selector());
    }

    private Set<InstanceMetaData> metadataSet() {
        InstanceMetaData imd = new InstanceMetaData();
        imd.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        return Set.of(imd);
    }
}
