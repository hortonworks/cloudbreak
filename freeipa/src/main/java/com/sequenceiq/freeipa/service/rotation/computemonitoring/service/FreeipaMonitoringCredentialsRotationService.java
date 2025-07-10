package com.sequenceiq.freeipa.service.rotation.computemonitoring.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigView;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;

@Service
public class FreeipaMonitoringCredentialsRotationService {

    private static final String MONITORING_KEY = "monitoring";

    @Inject
    private SecretRotationSaltService saltService;

    @Inject
    private MonitoringConfigService monitoringConfigService;

    @Inject
    private TelemetryContextProvider telemetryContextProvider;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private EnvironmentService environmentService;

    public void validateEnablement(Stack stack) {
        if (!stack.getTelemetry().isComputeMonitoringEnabled() || !entitlementService.isComputeMonitoringEnabled(stack.getAccountId())) {
            throw new SecretRotationException("Compute monitoring is not enabled for the cluster!");
        }
    }

    public void updateMonitoringCredentials(Stack stack) {
        refreshMonitoringPillars(stack);
        executeMonitoringRelatedSaltStates(stack);
    }

    private void executeMonitoringRelatedSaltStates(Stack stack) {
        try {
            Set<String> targets = stack.getAllFunctioningNodes().stream().map(Node::getHostname).collect(Collectors.toSet());
            // this will update cluster monitoringCredential as well, thus no need to rotate it explicitly
            saltService.executeSaltState(stack, targets, List.of("monitoring.init"));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to execute Compute Monitoring relevant salt states.", e);
        }
    }

    private void refreshMonitoringPillars(Stack stackDto) {
        try {
            TelemetryContext telemetryContext = telemetryContextProvider.createTelemetryContext(stackDto);
            MonitoringConfigView monitoringConfigView = monitoringConfigService.createConfigs(telemetryContext);
            SaltPillarProperties saltPillarProperties = new SaltPillarProperties("/" + MONITORING_KEY + "/init.sls",
                    Map.of(MONITORING_KEY, monitoringConfigView.toMap()));
            saltService.updateSaltPillar(stackDto, Map.of(MONITORING_KEY, saltPillarProperties));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to refresh Compute Monitoring relevant salt pillars.", e);
        }
    }
}
