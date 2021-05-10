package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.telemetry.converter.DiagnosticCloudStorageConverter;
import com.sequenceiq.common.model.diagnostics.CloudStorageDiagnosticsParameters;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;

@Service
public class FreeIpaCloudStorageValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCloudStorageValidationService.class);

    private static final String DEFAULT_ERROR_MESSAGE = "Validating FreeIPA cloud storage permission for backup failed.";

    private static final String PARAMETERS_ROOT = "telemetry";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private DiagnosticCloudStorageConverter diagnosticCloudStorageConverter;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    public void validate(Stack stack) throws CloudbreakOrchestratorException {
        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        List<GatewayConfig> allGateways = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDataSet);
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet);
        StackBasedExitCriteriaModel exitCriteriaModel = new StackBasedExitCriteriaModel(stack.getId());
        validateCloudStorageBackup(stack, allGateways, allNodes, exitCriteriaModel);
        validateCloudStorage(stack, allGateways, allNodes, exitCriteriaModel);
    }

    private void validateCloudStorageBackup(Stack stack, List<GatewayConfig> allGateways, Set<Node> allNodes, StackBasedExitCriteriaModel exitCriteriaModel) {
        try {
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            hostOrchestrator.validateCloudStorageBackup(primaryGatewayConfig, allGateways, allNodes, exitCriteriaModel);
        } catch (CloudbreakOrchestratorException e) {
            String errorMessage = getErrorMessage(stack, "backup");
            LOGGER.error(errorMessage, e);
        }
    }

    private void validateCloudStorage(Stack stack, List<GatewayConfig> allGateways, Set<Node> allNodes, StackBasedExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException {
        try {
            if (stack.getTelemetry() != null && stack.getTelemetry().isCloudStorageLoggingEnabled()) {
                Set<String> targetHostNames = allNodes.stream().map(Node::getHostname).limit(1).collect(Collectors.toSet());
                CloudStorageDiagnosticsParameters cloudStorageParameters = diagnosticCloudStorageConverter
                        .loggingToCloudStorageDiagnosticsParameters(stack.getTelemetry().getLogging(), stack.getRegion());
                DiagnosticParameters parameters = new DiagnosticParameters();
                parameters.setRoot(PARAMETERS_ROOT);
                parameters.setCloudStorageDiagnosticsParameters(cloudStorageParameters);
                telemetryOrchestrator.validateCloudStorage(allGateways, allNodes, targetHostNames, parameters.toMap(), exitCriteriaModel);
            }
        } catch (CloudbreakOrchestratorException e) {
            String errorMessage = getErrorMessage(stack, "logging");
            LOGGER.error(errorMessage, e);
            throw new CloudbreakOrchestratorFailedException(errorMessage, e);
        }
    }

    private String getErrorMessage(Stack stack, String function) {
        Backup backup = stack.getBackup();
        if (backup != null) {
            if (backup.getS3() != null) {
                return String.format("Validating FreeIPA cloud storage permission for %s failed. " +
                                "The instance profile %s did not have permission to write to %s. " +
                                "If provisioning was done using the UI, then verify the log's instance profile and logs location base when provisioning " +
                                "in the UI. " +
                                "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                                "Specifically verify the logStorage.instanceProfile and logStorage.storageLocationBase. " +
                                "Refer to Cloudera documentation at %s for the required rights.",
                        function,
                        backup.getS3().getInstanceProfile(), backup.getStorageLocation(),
                        "https://docs.cloudera.com/management-console/cloud/environments/topics/mc-idbroker-minimum-setup.html");
            } else if (backup.getAdlsGen2() != null) {
                return String.format("Validating FreeIPA cloud storage permission for %s failed. " +
                                "The managed profile %s did not have permission to write to %s. " +
                                "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning " +
                                "in the UI. " +
                                "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                                "Specifically, verify the logStorage.managedIdentity and logStorage.storageLocationBase. " +
                                "Refer to Cloudera documentation at %s for the required rights.",
                        function,
                        backup.getAdlsGen2().getManagedIdentity(), backup.getStorageLocation(),
                        "https://docs.cloudera.com/management-console/cloud/environments-azure/topics/mc-az-minimal-setup-for-cloud-storage.html");
            } else if (backup.getGcs() != null) {
                return String.format("Validating FreeIPA cloud storage permission for %s failed. " +
                                "The managed profile %s did not have permission to write to %s. " +
                                "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning " +
                                "in the UI. " +
                                "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                                "Specifically, verify the logStorage.serviceAccountEmail and logStorage.storageLocationBase. " +
                                "Refer to Cloudera documentation at %s for the required rights.",
                        function,
                        backup.getGcs().getServiceAccountEmail(), backup.getStorageLocation(),
                        "https://docs.cloudera.com/management-console/cloud/environments-gcp/topics/mc-gcp_minimum_setup_for_cloud_storage.html");
            }
        }
        return DEFAULT_ERROR_MESSAGE;
    }
}
