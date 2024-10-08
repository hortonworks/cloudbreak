package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String OPERATION_ID = "1";

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:datalake:f551c9f0-e837-4c61-9623-b8fe596757c4";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @Mock
    private TelemetryConfigService telemetryConfigService;

    @Mock
    private FreeIpaFlowManager freeIpaFlowManager;

    @Mock
    private OperationService operationService;

    @Mock
    private MonitoringUrlResolver monitoringUrlResolver;

    @InjectMocks
    private DynamicEntitlementRefreshService underTest;

    @Mock
    private Stack stack;

    @Mock
    private Telemetry telemetry;

    @Mock
    private Features features;

    @Mock
    private Operation operation;

    @BeforeEach
    void setup() {
        lenient().when(dynamicEntitlementRefreshConfig.getWatchedEntitlements()).thenReturn(Set.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        lenient().when(stack.getTelemetry()).thenReturn(telemetry);
        lenient().when(stack.getAccountId()).thenReturn("accountId");
        lenient().when(operation.getStatus()).thenReturn(OperationState.RUNNING);
        lenient().when(operation.getOperationId()).thenReturn(OPERATION_ID);
    }

    @Test
    void testGetChangedWatchedEntitlementsNotChanged() {
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        //monitoring enabled in telemetry component
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChangedWatchedEntitlementsEmptyTelemetry() {
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        //telemetry component is empty
        when(telemetry.getDynamicEntitlements()).thenReturn(new HashMap<>());

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
        verify(telemetryConfigService).storeTelemetry(STACK_ID, telemetry);
    }

    @Test
    void testGetChangedWatchedEntitlementsNoRightEntitlementInTelemetry() {
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        //telemetry component is empty
        Map<String, Boolean> dynamicEntitlements = new HashMap<>();
        dynamicEntitlements.put("RANDOM_ENTITLEMENT", Boolean.FALSE);
        when(telemetry.getDynamicEntitlements()).thenReturn(dynamicEntitlements);

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
        verify(telemetryConfigService).storeTelemetry(STACK_ID, telemetry);
    }

    @Test
    void testGetChangedWatchedEntitlementsChanged() {
        //monitoring disabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(Collections.emptyList());
        //monitoring enabled in telemetry component
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertFalse(result.isEmpty());
        assertFalse(result.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
    }

    @Test
    void testGetChangedWatchedEntitlementsWatchedEntitlementsEmpty() {
        //monitoring disabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(Collections.emptyList());
        when(dynamicEntitlementRefreshConfig.getWatchedEntitlements()).thenReturn(Collections.emptySet());
        //monitoring enabled in telemetry component
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testStoreChangedEntitlementsInTelemetry() {
        when(telemetry.getDynamicEntitlements()).thenReturn(new HashMap<>(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE)));
        when(telemetry.getFeatures()).thenReturn(features);
        FeatureSetting monitoring = mock(FeatureSetting.class);
        when(features.getMonitoring()).thenReturn(monitoring);
        underTest.storeChangedEntitlementsInTelemetry(stack, Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        ArgumentCaptor<Boolean> monitoringEnabled = ArgumentCaptor.forClass(Boolean.class);
        verify(monitoring).setEnabled(monitoringEnabled.capture());
        verify(telemetryConfigService).storeTelemetry(eq(STACK_ID), eq(telemetry));
        assertTrue(monitoringEnabled.getValue());
    }

    @Test
    void testStoreChangedEntitlementsInTelemetryMonitoringNotChanged() {
        when(telemetry.getDynamicEntitlements()).thenReturn(new HashMap<>(Map.of(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE)));
        FeatureSetting monitoring = mock(FeatureSetting.class);
        underTest.storeChangedEntitlementsInTelemetry(stack, Map.of(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE));

        verify(monitoring, never()).setEnabled(any());
        verify(telemetryConfigService).storeTelemetry(eq(STACK_ID), eq(telemetry));
    }

    @Test
    void testChangeClusterConfigurationEntitlementsNotChanged() {
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(stack.getId()).thenReturn(STACK_ID);
        when(telemetry.getDynamicEntitlements()).thenReturn(new HashMap<>(Map.of(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE)));
        underTest.changeClusterConfigurationIfEntitlementsChanged(stack);

        verify(operationService, never()).startOperation(any(), any(), any(), any());
    }

    @Test
    void testChangeClusterConfigurationEntitlementsChanged() {
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.FALSE));
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        when(stack.getId()).thenReturn(STACK_ID);
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);
        underTest.changeClusterConfigurationIfEntitlementsChanged(stack);

        verify(operationService, never()).failOperation(any(), any(), any());
        verify(freeIpaFlowManager).notify(eq(FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT), any());
    }

    @Test
    void testPreviousOperationFailedFlowFailed() {
        when(operationService.getOperationForAccountIdAndOperationId(ACCOUNT_ID, OPERATION_ID)).thenReturn(operation);
        when(operation.getStatus()).thenReturn(OperationState.FAILED);

        boolean result = underTest.previousOperationFailed(stack, OPERATION_ID);

        assertTrue(result);
    }

    @Test
    void testPreviousOperationSuccess() {
        when(operationService.getOperationForAccountIdAndOperationId(ACCOUNT_ID, OPERATION_ID)).thenReturn(operation);
        when(operation.getStatus()).thenReturn(OperationState.COMPLETED);

        boolean result = underTest.previousOperationFailed(stack, OPERATION_ID);

        assertFalse(result);
    }

}