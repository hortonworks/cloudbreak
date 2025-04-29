package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.SET_SELINUX_TO_ENFORCING_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.ModifySeLinuxResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxEvent;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
public class SeLinuxEnablementServiceTest {

    private static final String ENVIRONMENT_CRN = "environment_crn";

    private static final String ACCOUNT_ID = "account_id";

    private static final String USER_CRN = "user_crn";

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private StackService stackService;

    @Mock
    private OperationService operationService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @InjectMocks
    private SeLinuxEnablementService underTest;

    @Mock
    private Stack stack;

    @Captor
    private ArgumentCaptor<FreeIpaEnableSeLinuxEvent> seLinuxEventCaptor;

    @Test
    void testEnableSeLinuxOnAllNodes() throws CloudbreakOrchestratorException {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        when(stack.getId()).thenReturn(1L);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        underTest.enableSeLinuxOnAllNodes(stack);
    }

    @Test
    void testEnableSeLinuxOnAllNodesFailed() throws CloudbreakOrchestratorException {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        when(stack.getId()).thenReturn(1L);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        doThrow(new CloudbreakOrchestratorFailedException("test")).when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());
        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.enableSeLinuxOnAllNodes(stack));
        assertEquals("test", exception.getMessage());
    }

    @Test
    void testSetSeLinuxToEnforcingByCrn() {
        when(stackService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        Operation operation = mock(Operation.class);
        when(operation.getOperationId()).thenReturn("test-op");
        when(operationService.startOperation(ACCOUNT_ID, OperationType.MODIFY_SELINUX_MODE, Set.of(stack.getEnvironmentCrn()), Collections.emptySet()))
                .thenReturn(operation);
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        when(flowManager.notify(eq(SET_SELINUX_TO_ENFORCING_EVENT.event()), any())).thenReturn(flowIdentifier);
        ModifySeLinuxResponse response = underTest.setSeLinuxToEnforcingByCrn(ENVIRONMENT_CRN, ACCOUNT_ID);
        verify(flowManager).notify(eq(SET_SELINUX_TO_ENFORCING_EVENT.event()), seLinuxEventCaptor.capture());
        assertEquals(SET_SELINUX_TO_ENFORCING_EVENT.event(), seLinuxEventCaptor.getValue().selector());
        assertEquals("test-op", seLinuxEventCaptor.getValue().getOperationId());
        assertEquals(response.getFlowIdentifier(), flowIdentifier);
    }

    @Test
    void testSetSeLinuxToEnforcingByCrnThrowsException() {
        when(stackService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        Operation operation = mock(Operation.class);
        when(operation.getOperationId()).thenReturn("test-op");
        when(operationService.startOperation(ACCOUNT_ID, OperationType.MODIFY_SELINUX_MODE, Set.of(stack.getEnvironmentCrn()), Collections.emptySet()))
                .thenReturn(operation);
        doThrow(new RuntimeException("test")).when(flowManager).notify(eq(SET_SELINUX_TO_ENFORCING_EVENT.event()), any());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () ->
                underTest.setSeLinuxToEnforcingByCrn(ENVIRONMENT_CRN, ACCOUNT_ID));
        assertTrue(exception.getMessage().startsWith("Couldn't start Freeipa SELinux enablement flow: test"));
        verify(operationService).failOperation(ACCOUNT_ID, "test-op", "Couldn't start Freeipa SELinux enablement flow: test");
    }
}
