package com.sequenceiq.cloudbreak.cm.error.mapper;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cm.exception.CloudStorageConfigurationFailedException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

@Component
public class ClouderaManagerStorageErrorMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerStorageErrorMapper.class);

    private static final String MESSAGE_VALIDATION_ERROR = "No surprise that cluster creation has failed, probably something was not validated properly " +
            "in cloud storage config. This is most probably a control plane bug: ";

    public String map(CloudStorageConfigurationFailedException e, String cloudPlatform, Cluster cluster) {
        String originalMessage = e.getMessage();
        String mappedMessage = cluster.isRangerRazEnabled() ? getRazError(originalMessage) : mapNonRazMessage(originalMessage, cloudPlatform, cluster);
        LOGGER.debug("Mapped error message: {} original: {}", mappedMessage, originalMessage);
        return mappedMessage;
    }

    private String mapNonRazMessage(String originalMessage, String cloudPlatform, Cluster cluster) {
        Optional<CloudStorage> cloudStorage = Optional.of(cluster)
                .map(Cluster::getFileSystem)
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
