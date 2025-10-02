package com.sequenceiq.cloudbreak.cm.error.mapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class ClouderaManagerStorageErrorMapper implements ClouderaManagerErrorMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerStorageErrorMapper.class);

    private static final Set<String> CLOUD_STORAGE_RELATED_COMMANDS = Set.of(
            "RangerPluginCreateAuditDir",
            "CreateRangerAuditDir",
            "CreateRangerKafkaPluginAuditDirCommand",
            "CreateHiveWarehouseExternalDir",
            "CreateHiveWarehouseDir",
            "CreateRangerKnoxPluginAuditDirCommand");

    private static final String MESSAGE_VALIDATION_ERROR = "No surprise that cluster creation has failed, probably something was not validated properly " +
            "in cloud storage config. This is most probably a control plane bug: ";

    @Inject
    private EntitlementService entitlementService;

    @Override
    public boolean canHandle(StackDtoDelegate stack, List<CommandDetails> failedCommands) {
        return stack.getType() == StackType.DATALAKE
                && failedCommands.stream().map(CommandDetails::getName).anyMatch(CLOUD_STORAGE_RELATED_COMMANDS::contains)
                && isStorageValidationDisabled(stack);
    }

    private boolean isStorageValidationDisabled(StackDtoDelegate stack) {
        String cloudPlatform = stack.getCloudPlatform();
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        return CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform) && !entitlementService.awsCloudStorageValidationEnabled(accountId) ||
                CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform) && !entitlementService.azureCloudStorageValidationEnabled(accountId) ||
                CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform) && !entitlementService.gcpCloudStorageValidationEnabled(accountId);
    }

    @Override
    public String map(StackDtoDelegate stack, List<CommandDetails> failedCommands, String originalMessage) {
        String cloudPlatform = stack.getCloudPlatform();
        ClusterView cluster = stack.getCluster();
        String mappedMessage = cluster.isRangerRazEnabled() ? getRazError(originalMessage) : mapNonRazMessage(originalMessage, cloudPlatform, cluster);
        LOGGER.debug("Mapped error message: {} original: {}", mappedMessage, originalMessage);
        return mappedMessage;
    }

    private String mapNonRazMessage(String originalMessage, String cloudPlatform, ClusterView cluster) {
        Optional<CloudStorage> cloudStorage = Optional.of(cluster)
                .map(ClusterView::getFileSystem)
                .map(FileSystem::getCloudStorage);
        if (cloudStorage.isPresent() && cloudStorage.get().getCloudIdentities() != null && cloudStorage.get().getAccountMapping() != null
                && cloudStorage.get().getAccountMapping().getUserMappings() != null) {
            return mapCloudPlatformSpecificMessage(originalMessage, cloudPlatform);
        } else {
            LOGGER.warn(MESSAGE_VALIDATION_ERROR + JsonUtil.writeValueAsStringSilent(cloudStorage.orElse(null)));
            return originalMessage;
        }
    }

    private String mapCloudPlatformSpecificMessage(String originalMessage, String cloudPlatform) {
        try {
            switch (cloudPlatform) {
                case CloudConstants.AWS:
                    return String.format("%s %s", originalMessage, awsError());
                case CloudConstants.AZURE:
                    return String.format("%s %s", originalMessage, azureError());
                case CloudConstants.GCP:
                    return String.format("%s %s", originalMessage, gcpError());
                default:
                    LOGGER.debug("We don't have error message mapper for platform: {}", cloudPlatform);
                    return originalMessage;
            }
        } catch (RuntimeException runtimeException) {
            LOGGER.error(MESSAGE_VALIDATION_ERROR, runtimeException);
            return originalMessage;
        }
    }

    private String getRazError(String originalMessage) {
        StringBuilder sb = new StringBuilder();
        if (!Strings.isNullOrEmpty(originalMessage)) {
            sb.append(originalMessage);
            sb.append(originalMessage.endsWith(".") ? " " : ". ");
        }
        return sb.append("Ranger RAZ is enabled on this cluster.").toString();
    }

    private String awsError() {
        return String.format("Services running on the cluster were unable to write to the cloud storage. " +
                        "Please refer to Cloudera documentation at %s for the required rights.",
                DocumentationLinkProvider.awsCloudStorageSetupLink());
    }

    private String gcpError() {
        return String.format("Services running on the cluster were unable to write to the cloud storage. " +
                        "Please refer to Cloudera documentation at %s for the required rights.",
                DocumentationLinkProvider.googleCloudStorageSetupLink());
    }

    private String azureError() {
        return String.format("Services running on the cluster were unable to write to the cloud storage. " +
                        "Please refer to Cloudera documentation at %s for the required rights.",
                DocumentationLinkProvider.azureCloudStorageSetupLink());
    }
}
