package com.sequenceiq.cloudbreak.rotation.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.telemetry.TelemetryContextProvider;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.cloudbreak.telemetry.monitoring.ExporterConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfigView;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class MonitoringCredentialsRotationService {

    private static final String MONITORING_KEY = "monitoring";

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private SecretRotationSaltService saltService;

    @Inject
    private MonitoringConfigService monitoringConfigService;

    @Inject
    private TelemetryContextProvider telemetryContextProvider;

    @Inject
    private MonitoringConfiguration monitoringConfiguration;

    @Inject
    private EntitlementService entitlementService;

    public void validateEnablement(StackDto stackDto) {
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stackDto.getId());
        if (!telemetry.isComputeMonitoringEnabled() || !entitlementService.isComputeMonitoringEnabled(stackDto.getAccountId())) {
            throw new SecretRotationException("Compute monitoring is not enabled for the cluster!");
        }
    }

    public String getCmMonitoringUser() {
        return Optional.ofNullable(monitoringConfiguration.getClouderaManagerExporter())
                .map(ExporterConfiguration::getUser)
                .filter(StringUtils::isNotBlank)
                .orElseThrow();
    }

    public void updateMonitoringCredentials(StackDto stackDto) {
        refreshMonitoringPillars(stackDto);
        executeMonitoringRelatedSaltStates(stackDto);
        updateSmonConfigs(stackDto);
        restartMgmtServicesInCM(stackDto);
    }

    private void restartMgmtServicesInCM(StackDto stackDto) {
        try {
            clusterApiConnectors.getConnector(stackDto).clusterModificationService().restartMgmtServices();
        } catch (Exception e) {
            throw new SecretRotationException("Failed to restart MGMT services to update them with compute monitoring credential.", e);
        }
    }

    private void updateSmonConfigs(StackDto stackDto) {
        try {
            Telemetry telemetry = componentConfigProviderService.getTelemetry(stackDto.getId());
            clusterApiConnectors.getConnector(stackDto).clusterSetupService().updateSmonConfigs(telemetry);
        } catch (Exception e) {
            throw new SecretRotationException("Failed to update MGMT services with compute monitoring credentials.", e);
        }
    }

    private void executeMonitoringRelatedSaltStates(StackDto stackDto) {
        try {
            Set<String> targets = stackUtil.collectReachableNodes(stackDto).stream().map(Node::getHostname).collect(Collectors.toSet());
            // this will update cluster monitoringCredential as well, thus no need to rotate it explicitly
            saltService.executeSaltState(stackDto, targets, List.of("monitoring.init"));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to execute Compute Monitoring relevant salt states.", e);
        }
    }

    private void refreshMonitoringPillars(StackDto stackDto) {
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
