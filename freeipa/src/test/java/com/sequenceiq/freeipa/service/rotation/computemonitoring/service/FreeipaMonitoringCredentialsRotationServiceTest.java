package com.sequenceiq.freeipa.service.rotation.computemonitoring.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigView;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;

@ExtendWith(MockitoExtension.class)
public class FreeipaMonitoringCredentialsRotationServiceTest {

    @Mock
    private SecretRotationSaltService saltService;

    @Mock
    private MonitoringConfigService monitoringConfigService;

    @Mock
    private TelemetryContextProvider telemetryContextProvider;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private FreeipaMonitoringCredentialsRotationService underTest;

    @Mock
    private EnvironmentService environmentService;

    @Test
    void testEnablementCheckIfMonitoringDisabledForStack() {
        Stack stack = new Stack();
        Telemetry telemetry = new Telemetry();
        Monitoring monitoring = new Monitoring();
        telemetry.setMonitoring(monitoring);
        stack.setTelemetry(telemetry);

        assertThrows(SecretRotationException.class, () -> underTest.validateEnablement(stack));

        verifyNoInteractions(entitlementService);
    }

    @Test
    void testEnablementCheckIfMonitoringDisabledForAccount() {
        Stack stack = new Stack();
        Telemetry telemetry = new Telemetry();
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl("url");
        telemetry.setMonitoring(monitoring);
        stack.setTelemetry(telemetry);
        when(entitlementService.isComputeMonitoringEnabled(any())).thenReturn(false);

        assertThrows(SecretRotationException.class, () -> underTest.validateEnablement(stack));
    }

    @Test
    void testEnablementCheckIfMonitoringEnabled() {
        Stack stack = new Stack();
        Telemetry telemetry = new Telemetry();
        Monitoring monitoring = new Monitoring();
        monitoring.setRemoteWriteUrl("url");
        telemetry.setMonitoring(monitoring);
        stack.setTelemetry(telemetry);
        when(entitlementService.isComputeMonitoringEnabled(any())).thenReturn(true);

        underTest.validateEnablement(stack);
    }

    @Test
    void testUpdate() throws CloudbreakOrchestratorFailedException {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        instanceGroup.setTemplate(new Template());
        stack.setInstanceGroups(Set.of(instanceGroup));
        when(telemetryContextProvider.createTelemetryContext(any())).thenReturn(new TelemetryContext());
        when(monitoringConfigService.createConfigs(any())).thenReturn(new MonitoringConfigView.Builder().build());
        doNothing().when(saltService).updateSaltPillar(any(), any());
        doNothing().when(saltService).executeSaltState(any(), any(), any());

        underTest.updateMonitoringCredentials(stack);

        ArgumentCaptor<Map> pillarMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(saltService).updateSaltPillar(any(), pillarMapCaptor.capture());
        assertEquals("monitoring", pillarMapCaptor.getValue().keySet().iterator().next());

        ArgumentCaptor<List> stateCaptor = ArgumentCaptor.forClass(List.class);
        verify(saltService).executeSaltState(any(), any(), stateCaptor.capture());
        assertEquals(List.of("monitoring.init"), stateCaptor.getValue());
    }
}
