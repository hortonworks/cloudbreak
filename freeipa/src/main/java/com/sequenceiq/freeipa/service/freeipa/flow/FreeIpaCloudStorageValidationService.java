package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaCloudStorageValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCloudStorageValidationService.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private StackService stackService;

    public void validate(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);

        validateCloudStorage(stackId, stack, gatewayConfigs, allNodes);
    }

    private void validateCloudStorage(Long stackId, Stack stack, List<GatewayConfig> gatewayConfigs, Set<Node> allNodes) throws CloudbreakOrchestratorException {
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);

        try {
            hostOrchestrator.validateCloudStorageBackup(primaryGatewayConfig, gatewayConfigs, allNodes, new StackBasedExitCriteriaModel(stackId));
        } catch (CloudbreakOrchestratorException e) {
            Backup backup = stack.getBackup();
            String errorMsg = "Validating FreeIPA cloud storage permission for backup failed.";
            if (backup != null) {
                if (backup.getS3() != null) {
                    errorMsg = String.format("Validating FreeIPA cloud storage permission for backup failed. " +
                                    "The instance profile %s did not have permission to write to %s. " +
                                    "If provisioning was done using the UI, then verify the log's instance profile and logs location base when provisioning " +
                                    "in the UI. " +
                                    "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                                    "Specifically verify the logStorage.instanceProfile and logStorage.storageLocationBase. " +
                                    "Refer to Cloudera documentation at %s for the required rights.",
                            backup.getS3().getInstanceProfile(), backup.getStorageLocation(),
                            DocumentationLinkProvider.awsCloudStorageSetupLink());
                } else if (backup.getAdlsGen2() != null) {
                    errorMsg = String.format("Validating FreeIPA cloud storage permission for backup failed. " +
                                    "The managed profile %s did not have permission to write to %s. " +
                                    "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning " +
                                    "in the UI. " +
                                    "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                                    "Specifically, verify the logStorage.managedIdentity and logStorage.storageLocationBase. " +
                                    "Refer to Cloudera documentation at %s for the required rights.",
                            backup.getAdlsGen2().getManagedIdentity(), backup.getStorageLocation(),
                            DocumentationLinkProvider.azureCloudStorageSetupLink());
                } else if (backup.getGcs() != null) {
                    errorMsg = String.format("Validating FreeIPA cloud storage permission for backup failed. " +
                                    "The managed profile %s did not have permission to write to %s. " +
                                    "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning " +
                                    "in the UI. " +
                                    "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. " +
                                    "Specifically, verify the logStorage.managedIdentity and logStorage.storageLocationBase. " +
                                    "Refer to Cloudera documentation at %s for the required rights.",
                            backup.getGcs().getServiceAccountEmail(), backup.getStorageLocation(),
                            DocumentationLinkProvider.googleCloudStorageSetupLink());
                }
            }
            LOGGER.error(errorMsg, e);
            validationBuilder.error(errorMsg);
        }

        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }
}
