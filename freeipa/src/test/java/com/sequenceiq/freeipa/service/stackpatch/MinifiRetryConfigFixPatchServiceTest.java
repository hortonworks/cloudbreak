package com.sequenceiq.freeipa.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackPatchType;
import com.sequenceiq.freeipa.orchestrator.SaltUpdateService;
import com.sequenceiq.freeipa.service.GatewayConfigService;

@ExtendWith(MockitoExtension.class)
class MinifiRetryConfigFixPatchServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String CRN = "crn:cluster";

    private static final String ENVIRONMENT_CRN = "crn:env";

    private static final String ACCOUNT_ID = "accountId";

    private static final String FQDN = "host1.example.com";

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SaltUpdateService saltUpdateService;

    @InjectMocks
    private MinifiRetryConfigFixPatchService underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn(CRN);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
    }

    @Test
    void getStackPatchTypeReturnsMinifiRetryConfigFix() {
        assertTrue(StackPatchType.MINIFI_RETRY_CONFIG_FIX == underTest.getStackPatchType());
    }

    @Test
    void isAffectedReturnsTrueWhenMinifiActive() throws Exception {
        Stack stackWithNodes = stackWithNode();
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stackWithNodes)).thenReturn(mock(GatewayConfig.class));
        when(hostOrchestrator.runCommandOnHosts(anyList(), eq(Set.of(FQDN)), any()))
                .thenReturn(Map.of(FQDN, "active\n"));

        assertTrue(underTest.isAffected(stackWithNodes));
    }

    @Test
    void isAffectedReturnsFalseWhenMinifiInactive() throws Exception {
        Stack stackWithNodes = stackWithNode();
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stackWithNodes)).thenReturn(mock(GatewayConfig.class));
        when(hostOrchestrator.runCommandOnHosts(anyList(), eq(Set.of(FQDN)), any()))
                .thenReturn(Map.of(FQDN, "inactive\n"));

        assertFalse(underTest.isAffected(stackWithNodes));
    }

    @Test
    void isAffectedReturnsFalseWhenNoNodes() {
        Stack stackWithNodes = spy(stack);
        doReturn(Set.of()).when(stackWithNodes).getNotDeletedInstanceMetaDataSet();

        assertFalse(underTest.isAffected(stackWithNodes));
    }

    @Test
    void isAffectedThrowsWhenCommandFails() throws Exception {
        Stack stackWithNodes = stackWithNode();
        when(gatewayConfigService.getPrimaryGatewayConfigForSalt(stackWithNodes)).thenReturn(mock(GatewayConfig.class));
        when(hostOrchestrator.runCommandOnHosts(anyList(), eq(Set.of(FQDN)), any()))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.isAffected(stackWithNodes));
    }

    @Test
    void doApplyTriggersSaltUpdateFlow() throws Exception {
        when(saltUpdateService.updateSaltStates(ENVIRONMENT_CRN, ACCOUNT_ID))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "flowId"));

        boolean applied = underTest.doApply(stack);

        assertTrue(applied);
        verify(saltUpdateService).updateSaltStates(ENVIRONMENT_CRN, ACCOUNT_ID);
    }

    @Test
    void doApplyThrowsWhenFlowTriggerFails() {
        when(saltUpdateService.updateSaltStates(ENVIRONMENT_CRN, ACCOUNT_ID)).thenThrow(new RuntimeException("boom"));

        assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(stack));
        verify(gatewayConfigService, never()).getPrimaryGatewayConfigForSalt(any());
    }

    private Stack stackWithNode() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(FQDN);
        Stack stackWithNodes = spy(stack);
        doReturn(Set.of(instanceMetaData)).when(stackWithNodes).getNotDeletedInstanceMetaDataSet();
        return stackWithNodes;
    }
}
