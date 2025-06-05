package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.telemetry.converter.DiagnosticCloudStorageConverter;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.model.diagnostics.CloudStorageDiagnosticsParameters;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class CloudStorageValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageValidationService.class);

    private static final String IDBROKER = "idbroker";

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private DiagnosticCloudStorageConverter diagnosticCloudStorageConverter;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private EntitlementService entitlementService;

    public void validateCloudStorage(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        if (!entitlementService.cloudStorageValidationOnVmEnabled(accountId)) {
            LOGGER.info("Cloud storage validation on VM entitlement is missing, not validating cloud storage on VM.");
            return;
        }

        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
        if (environment.isCloudStorageLoggingEnabled()) {
            ExitCriteriaModel exitCriteriaModel = new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId());
            List<GatewayConfig> allGateways = gatewayConfigService.getAllGatewayConfigs(stack);
            Set<Node> allNodes = stackUtil.collectNodes(stack);
            runIdBrokerValidation(stack, allGateways, allNodes, environment, exitCriteriaModel);
            runRandomHostValidation(stack, allGateways, allNodes, environment, exitCriteriaModel);
        }
    }

    private void runIdBrokerValidation(Stack stack, List<GatewayConfig> allGateways,
            Set<Node> allNodes, DetailedEnvironmentResponse environment, ExitCriteriaModel exitCriteriaModel) {

        Optional<String> idbrokerHostname = getIdbrokerHostname(allNodes);
        if (idbrokerHostname.isPresent()) {
            try {
                Set<String> targetHostNames = Set.of(idbrokerHostname.get());
                executeValidation(stack, allGateways, allNodes, environment, exitCriteriaModel, targetHostNames);
            } catch (CloudbreakOrchestratorException e) {
                eventService.fireCloudbreakEvent(stack.getId(), CREATE_IN_PROGRESS.name(),
                        ResourceEvent.CLUSTER_PROVISION_CLOUD_STORAGE_VALIDATION_ON_IDBROKER_FAILED);
                LOGGER.warn("Validation of the cluster's cloud storage access for logging failed on the IDBroker host.", e);
            }
        }
    }

    private Optional<String> getIdbrokerHostname(Set<Node> allNodes) {
        return allNodes.stream()
                .filter(node -> node.getHostGroup().equalsIgnoreCase(IDBROKER))
                .map(Node::getHostname)
                .findAny();
    }

    private void runRandomHostValidation(Stack stack, List<GatewayConfig> allGateways, Set<Node> allNodes,
            DetailedEnvironmentResponse environment, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

        Optional<String> randomNonIdbrokerHostname = getRandomNonIdbrokerHostname(allNodes);
        if (randomNonIdbrokerHostname.isPresent()) {
            try {
                Set<String> targetHostNames = Set.of(randomNonIdbrokerHostname.get());
                executeValidation(stack, allGateways, allNodes, environment, exitCriteriaModel, targetHostNames);
            } catch (CloudbreakOrchestratorException e) {
                String errorMessage = getErrorMessage(environment);
                LOGGER.error(errorMessage, e);
                throw new CloudbreakOrchestratorFailedException(errorMessage, e);
            }
        } else {
            LOGGER.warn("No hostname found for stack '{}' during cloud storage validation!", stack.getResourceCrn());
        }
    }

    private Optional<String> getRandomNonIdbrokerHostname(Set<Node> allNodes) {
        return allNodes.stream()
                .filter(node -> !node.getHostGroup().equalsIgnoreCase(IDBROKER))
                .map(Node::getHostname)
                .findAny();
    }

    private String getErrorMessage(DetailedEnvironmentResponse environment) {
        String errorMessage = "Validation of the cluster's cloud storage access for logging failed.";
        if (environment.getTelemetry().getLogging().getS3() != null) {
            errorMessage = String.format("Validation of the stack's cloud storage access for logging failed. " +
                            "The instance profile %s did not have permission to write to %s. " +
                            "If provisioning was done using the UI, then verify the log's instance profile and logs location base when provisioning " +
                            "in the UI. " +
                            "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                            "Specifically verify the logStorage.instanceProfile and logStorage.storageLocationBase. " +
                            "Refer to Cloudera documentation at %s for the required rights.",
                    environment.getTelemetry().getLogging().getS3().getInstanceProfile(),
                    environment.getTelemetry().getLogging().getStorageLocation(),
                    DocumentationLinkProvider.awsCloudStorageSetupLink());
        } else if (environment.getTelemetry().getLogging().getAdlsGen2() != null) {
            errorMessage = String.format("Validation of the cluster's cloud storage access for logging failed. " +
                            "The managed profile %s did not have permission to write to %s. " +
                            "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning " +
                            "in the UI. " +
                            "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                            "Specifically, verify the logStorage.managedIdentity and logStorage.storageLocationBase. " +
                            "Refer to Cloudera documentation at %s for the required rights.",
                    environment.getTelemetry().getLogging().getAdlsGen2().getManagedIdentity(),
                    environment.getTelemetry().getLogging().getStorageLocation(),
                    DocumentationLinkProvider.azureCloudStorageSetupLink());
        } else if (environment.getTelemetry().getLogging().getGcs() != null) {
            errorMessage = String.format("Validation of the cluster's cloud storage access for logging failed. " +
                            "The managed profile %s did not have permission to write to %s. " +
                            "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning " +
                            "in the UI. " +
                            "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                            "Specifically, verify the logStorage.managedIdentity and logStorage.storageLocationBase. " +
                            "Refer to Cloudera documentation at %s for the required rights.",
                    environment.getTelemetry().getLogging().getGcs().getServiceAccountEmail(),
                    environment.getTelemetry().getLogging().getStorageLocation(),
                    DocumentationLinkProvider.googleCloudStorageSetupLink());
        }
        return errorMessage;
    }

    private void executeValidation(Stack stack, List<GatewayConfig> allGateways, Set<Node> allNodes,
            DetailedEnvironmentResponse environment, ExitCriteriaModel exitCriteriaModel, Set<String> targetHostNames) throws CloudbreakOrchestratorException {

        CloudStorageDiagnosticsParameters cloudStorageParameters = diagnosticCloudStorageConverter
                .loggingResponseToCloudStorageDiagnosticsParameters(environment.getTelemetry().getLogging(), stack.getRegion());
        DiagnosticParameters parameters = new DiagnosticParameters();
        parameters.setRoot(DiagnosticParameters.TELEMETRY_ROOT);
        parameters.setCloudStorageDiagnosticsParameters(cloudStorageParameters);
        telemetryOrchestrator.validateCloudStorage(allGateways, allNodes, targetHostNames, parameters.toMap(), exitCriteriaModel);
    }
}
