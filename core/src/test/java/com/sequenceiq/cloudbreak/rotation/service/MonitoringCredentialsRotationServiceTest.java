package com.sequenceiq.cloudbreak.rotation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigView;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@ExtendWith(MockitoExtension.class)
public class MonitoringCredentialsRotationServiceTest {

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private SecretRotationSaltService saltService;

    @Mock
    private MonitoringConfigService monitoringConfigService;

    @Mock
    private TelemetryContextProvider telemetryContextProvider;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private MonitoringCredentialsRotationService underTest;

    @Test
    void testEnablementCheckIfMonitoringDisabledForStack() {
        Telemetry telemetry = new Telemetry();
        Monitoring monitoring = new Monitoring();
        telemetry.setMonitoring(monitoring);
        when(componentConfigProviderService.getTelemetry(any())).thenReturn(telemetry);

        assertThrows(SecretRotationException.class, () -> underTest.validateEnablement(getStack()));

        verifyNoInteractions(entitlementService);
    }

    @Test
    void testEnablementCheckIfMonitoringDisabledForAccount() {
        Telemetry telemetry = new Telemetry();
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl("url");
        telemetry.setMonitoring(monitoring);
        when(componentConfigProviderService.getTelemetry(any())).thenReturn(telemetry);
        when(entitlementService.isComputeMonitoringEnabled(any())).thenReturn(false);

        assertThrows(SecretRotationException.class, () -> underTest.validateEnablement(getStack()));
    }

    @Test
    void testEnablementCheckIfMonitoringEnabled() {
        Telemetry telemetry = new Telemetry();
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl("url");
        telemetry.setMonitoring(monitoring);
        when(componentConfigProviderService.getTelemetry(any())).thenReturn(telemetry);
        when(entitlementService.isComputeMonitoringEnabled(any())).thenReturn(true);

        underTest.validateEnablement(getStack());
    }

    @Test
    void testUpdate() throws Exception {
        when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of(new Node(null, null, null, null)));
        when(telemetryContextProvider.createTelemetryContext(any())).thenReturn(new TelemetryContext());
        when(monitoringConfigService.createConfigs(any())).thenReturn(new MonitoringConfigView.Builder().build());
        doNothing().when(saltService).updateSaltPillar(any(), any());
        doNothing().when(saltService).executeSaltState(any(), any(), any());
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        ClusterSetupService setupService = mock(ClusterSetupService.class);
        when(clusterApi.clusterSetupService()).thenReturn(setupService);
        ClusterModificationService modService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(modService);
        doNothing().when(setupService).updateSmonConfigs(any());
        doNothing().when(modService).restartMgmtServices();

        underTest.updateMonitoringCredentials(getStack());

        ArgumentCaptor<Map> pillarMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(saltService).updateSaltPillar(any(), pillarMapCaptor.capture());
        assertEquals("monitoring", pillarMapCaptor.getValue().keySet().iterator().next());

        ArgumentCaptor<List> stateCaptor = ArgumentCaptor.forClass(List.class);
        verify(saltService).executeSaltState(any(), any(), stateCaptor.capture());
        assertEquals(List.of("monitoring.init"), stateCaptor.getValue());

        verify(setupService).updateSmonConfigs(any());
        verify(modService).restartMgmtServices();
    }

    private StackDto getStack() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getId()).thenReturn(1L);
        lenient().when(stackDto.getAccountId()).thenReturn("acc");
        return stackDto;
    }
}
