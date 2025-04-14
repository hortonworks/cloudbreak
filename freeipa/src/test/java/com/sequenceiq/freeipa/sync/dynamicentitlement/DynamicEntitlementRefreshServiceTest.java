package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringUrlResolver;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.FeatureSetting;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.freeipa.entity.DynamicEntitlement;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.service.DynamicEntitlementService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

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
    private StackService stackService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private FreeIpaFlowManager freeIpaFlowManager;

    @Mock
    private FlowService flowService;

    @Mock
    private MonitoringUrlResolver monitoringUrlResolver;

    @Mock
    private DynamicEntitlementService dynamicEntitlementService;

    @InjectMocks
    private DynamicEntitlementRefreshService underTest;

    @Mock
    private Stack stack;

    @Mock
    private Telemetry telemetry;

    @Mock
    private Features features;

    @Mock
    private FlowIdentifier flowIdentifier;

    @BeforeEach
    void setup() throws TransactionService.TransactionExecutionException {
        lenient().when(dynamicEntitlementRefreshConfig.getWatchedEntitlements()).thenReturn(Set.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        lenient().when(stack.getTelemetry()).thenReturn(telemetry);
        lenient().when(stack.getAccountId()).thenReturn("accountId");
        lenient().when(flowIdentifier.getPollableId()).thenReturn(FLOW_CHAIN_ID);
        lenient().when(stackService.getStackById(eq(STACK_ID))).thenReturn(stack);
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
    }

    @Test
    void testGetChangedWatchedEntitlementsNotChanged() {
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        //monitoring enabled in freeipa
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(Set.of(new DynamicEntitlement(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE, null)));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChangedWatchedEntitlementsLegacyEntitlementsStoredInTelemetry() {
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        //monitoring enabled in telemetry component (deprecated)
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.FALSE));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertFalse(result.isEmpty());
        verify(stackService).save(stack);
    }

    @Test
    void testGetChangedWatchedEntitlementsEmpty() throws TransactionService.TransactionExecutionException {
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        //in freeipa it is empty
        when(dynamicEntitlementService.findByStackId(STACK_ID)).thenReturn(new HashSet<>());

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
        verify(stackService).save(stack);
    }

    @Test
    void testGetChangedWatchedEntitlementsNoRightEntitlementStored() {
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        //in freeipa it is empty
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(new HashSet<>(Set.of(new DynamicEntitlement("RANDOM_ENTITLEMENT", Boolean.FALSE, null))));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
        verify(stackService).save(stack);
    }

    @Test
    void testGetChangedWatchedEntitlementsChanged() {
        //monitoring disabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(Collections.emptyList());
        //monitoring enabled in freeipa
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(Set.of(new DynamicEntitlement(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE, null)));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertFalse(result.isEmpty());
        assertFalse(result.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
    }

    @Test
    void testGetChangedWatchedEntitlementsWatchedEntitlementsEmpty() {
        //monitoring disabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(Collections.emptyList());
        when(dynamicEntitlementRefreshConfig.getWatchedEntitlements()).thenReturn(Collections.emptySet());
        //monitoring enabled in freeipa
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(Set.of(new DynamicEntitlement(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE, null)));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testStoreChangedEntitlements() {
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(new HashSet<>(Set.of(new DynamicEntitlement(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE, null))));

        when(telemetry.getFeatures()).thenReturn(features);
        FeatureSetting monitoring = mock(FeatureSetting.class);
        when(features.getMonitoring()).thenReturn(monitoring);
        underTest.storeChangedEntitlementsAndTelemetry(stack, Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        ArgumentCaptor<Boolean> monitoringEnabled = ArgumentCaptor.forClass(Boolean.class);
        verify(monitoring).setEnabled(monitoringEnabled.capture());
        verify(stackService).save(stack);
        assertTrue(monitoringEnabled.getValue());
    }

    @Test
    void testStoreChangedEntitlementsInTelemetryMonitoringNotChanged() {
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(new HashSet<>(Set.of(new DynamicEntitlement(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE, null))));

        FeatureSetting monitoring = mock(FeatureSetting.class);
        underTest.storeChangedEntitlementsAndTelemetry(stack, Map.of(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE));

        verify(monitoring, never()).setEnabled(any());
        verify(stackService).save(stack);
    }

    @Test
    void testChangeClusterConfigurationEntitlementsNotChanged() {
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getTelemetry()).thenReturn(telemetry);
        when(stack.getId()).thenReturn(STACK_ID);
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(new HashSet<>(Set.of(new DynamicEntitlement(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE, null))));

        underTest.changeClusterConfigurationIfEntitlementsChanged(stack);

        verify(freeIpaFlowManager, never()).notify(anyString(), any());
    }

    @Test
    void testChangeClusterConfigurationEntitlementsChanged() {
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        when(dynamicEntitlementService.findByStackId(STACK_ID))
                .thenReturn(Set.of(new DynamicEntitlement(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.FALSE, null)));

        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        when(stack.getId()).thenReturn(STACK_ID);
        underTest.changeClusterConfigurationIfEntitlementsChanged(stack);

        verify(freeIpaFlowManager).notify(eq(FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT), any());
    }

    @Test
    void testPreviousFlowFailedFlowFailed() {
        when(flowService.isPreviousFlowFailed(STACK_ID, FLOW_CHAIN_ID)).thenReturn(Boolean.TRUE);

        boolean result = underTest.previousFlowFailed(stack, FLOW_CHAIN_ID);

        assertTrue(result);
    }

    @Test
    void testPreviousOperationSuccess() {
        when(flowService.isPreviousFlowFailed(STACK_ID, FLOW_CHAIN_ID)).thenReturn(Boolean.FALSE);

        boolean result = underTest.previousFlowFailed(stack, FLOW_CHAIN_ID);

        assertFalse(result);
    }

}