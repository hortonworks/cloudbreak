package com.sequenceiq.cloudbreak.service.telemetry;

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
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.dynamicentitlement.DynamicEntitlementRefreshConfig;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.FeatureSetting;

@ExtendWith(MockitoExtension.class)
class DynamicEntitlementRefreshServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:datalake:f551c9f0-e837-4c61-9623-b8fe596757c4";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private DynamicEntitlementRefreshConfig dynamicEntitlementRefreshConfig;

    @InjectMocks
    private DynamicEntitlementRefreshService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private Telemetry telemetry;

    @Mock
    private Features features;

    @BeforeEach
    void setup() {
        lenient().when(dynamicEntitlementRefreshConfig.getCbEntitlements()).thenReturn(List.of("CDP_CENTRAL_COMPUTE_MONITORING"));
        lenient().when(dynamicEntitlementRefreshConfig.getWatchedEntitlements()).thenReturn(Set.of("CDP_CENTRAL_COMPUTE_MONITORING"));
    }

    @Test
    void testSaltRefreshNeeded() {
        Boolean result = underTest.saltRefreshNeeded(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));
        assertTrue(result);
    }

    @Test
    void testSaltRefreshNotNeeded() {
        Boolean result = underTest.saltRefreshNeeded(Map.of(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE));
        assertFalse(result);
    }

    @Test
    void testSaltRefreshNeededEmpty() {
        Boolean result = underTest.saltRefreshNeeded(Collections.emptyMap());
        assertFalse(result);
    }

    @Test
    void testSaltRefreshNeededNull() {
        Boolean result = underTest.saltRefreshNeeded(null);
        assertFalse(result);
    }

    @Test
    void testGetChangedWatchedEntitlementsNotChanged() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        when(componentConfigProviderService.getTelemetry(STACK_ID)).thenReturn(telemetry);
        //monitoring enabled in telemetry component
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlements(stackDto);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChangedWatchedEntitlementsEmptyTelemetry() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        when(componentConfigProviderService.getTelemetry(STACK_ID)).thenReturn(telemetry);
        //telemetry component is empty
        when(telemetry.getDynamicEntitlements()).thenReturn(null);

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlements(stackDto);

        assertTrue(result.isEmpty());
        verify(componentConfigProviderService).replaceTelemetryComponent(STACK_ID, telemetry);
    }

    @Test
    void testGetChangedWatchedEntitlementsNoRightEntitlementInTelemetry() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        //monitoring enabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(List.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
        when(componentConfigProviderService.getTelemetry(STACK_ID)).thenReturn(telemetry);
        //telemetry component is empty
        Map<String, Boolean> dynamicEntitlements = new HashMap<>();
        dynamicEntitlements.put("RANDOM_ENTITLEMENT", Boolean.FALSE);
        when(telemetry.getDynamicEntitlements()).thenReturn(dynamicEntitlements);

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlements(stackDto);

        assertTrue(result.isEmpty());
        verify(componentConfigProviderService).replaceTelemetryComponent(STACK_ID, telemetry);
    }

    @Test
    void testGetChangedWatchedEntitlementsChanged() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        //monitoring disabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(Collections.emptyList());
        when(componentConfigProviderService.getTelemetry(STACK_ID)).thenReturn(telemetry);
        //monitoring enabled in telemetry component
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlements(stackDto);

        assertFalse(result.isEmpty());
        assertFalse(result.get(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name()));
    }

    @Test
    void testGetChangedWatchedEntitlementsWatchedEntitlementsEmpty() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getResourceCrn()).thenReturn(STACK_CRN);
        //monitoring disabled in ums
        when(entitlementService.getEntitlements(any())).thenReturn(Collections.emptyList());
        when(dynamicEntitlementRefreshConfig.getWatchedEntitlements()).thenReturn(Collections.emptySet());
        when(componentConfigProviderService.getTelemetry(STACK_ID)).thenReturn(telemetry);
        //monitoring enabled in telemetry component
        when(telemetry.getDynamicEntitlements()).thenReturn(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        Map<String, Boolean> result = underTest.getChangedWatchedEntitlements(stackDto);

        assertTrue(result.isEmpty());
    }

    @Test
    void testStoreChangedEntitlementsInTelemetry() {
        when(componentConfigProviderService.getTelemetry(STACK_ID)).thenReturn(telemetry);
        when(telemetry.getDynamicEntitlements()).thenReturn(new HashMap<>(Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE)));
        when(telemetry.getFeatures()).thenReturn(features);
        FeatureSetting monitoring = mock(FeatureSetting.class);
        when(features.getMonitoring()).thenReturn(monitoring);
        underTest.storeChangedEntitlementsInTelemetry(STACK_ID, Map.of(Entitlement.CDP_CENTRAL_COMPUTE_MONITORING.name(), Boolean.TRUE));

        ArgumentCaptor<Boolean> monitoringEnabled = ArgumentCaptor.forClass(Boolean.class);
        verify(monitoring).setEnabled(monitoringEnabled.capture());
        verify(componentConfigProviderService).replaceTelemetryComponent(eq(STACK_ID), eq(telemetry));
        assertTrue(monitoringEnabled.getValue());
    }

    @Test
    void testStoreChangedEntitlementsInTelemetryMonitoringNotChanged() {
        when(componentConfigProviderService.getTelemetry(STACK_ID)).thenReturn(telemetry);
        when(telemetry.getDynamicEntitlements()).thenReturn(new HashMap<>(Map.of(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE)));
        FeatureSetting monitoring = mock(FeatureSetting.class);
        underTest.storeChangedEntitlementsInTelemetry(STACK_ID, Map.of(Entitlement.CLOUDERA_INTERNAL_ACCOUNT.name(), Boolean.TRUE));

        verify(monitoring, never()).setEnabled(any());
        verify(componentConfigProviderService).replaceTelemetryComponent(eq(STACK_ID), eq(telemetry));
    }
}

