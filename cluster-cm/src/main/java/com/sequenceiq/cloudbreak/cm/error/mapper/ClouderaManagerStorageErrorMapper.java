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
import com.sequenceiq.cloudbreak.domain.cloudstorage.AccountMapping;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AdlsGen2Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.GcsIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;

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
            return mapCloudPlatformSpecificMessage(originalMessage, cloudPlatform, cloudStorage.get());
        } else {
            LOGGER.warn(MESSAGE_VALIDATION_ERROR + JsonUtil.writeValueAsStringSilent(cloudStorage.orElse(null)));
            return originalMessage;
        }
    }

    private String mapCloudPlatformSpecificMessage(String originalMessage, String cloudPlatform, CloudStorage cloudStorage) {
        try {
            switch (cloudPlatform) {
                case CloudConstants.AWS:
                    return awsError(cloudStorage);
                case CloudConstants.AZURE:
                    return azureError(cloudStorage);
                case CloudConstants.GCP:
                    return gcpError(cloudStorage);
                default:
                    LOGGER.debug("We don't have error message mapper for platform: {}", cloudPlatform);
                    return originalMessage;
            }
        } catch (RuntimeException runtimeException) {
            LOGGER.error(MESSAGE_VALIDATION_ERROR + JsonUtil.writeValueAsStringSilent(cloudStorage), runtimeException);
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

    private String awsError(CloudStorage cloudStorage) {

        String assumerInstanceProfile = "";

        for (CloudIdentity cloudIdentity : cloudStorage.getCloudIdentities()) {
            if (CloudIdentityType.ID_BROKER == cloudIdentity.getIdentityType()) {
                assumerInstanceProfile = Optional.ofNullable(cloudIdentity.getS3Identity()).map(S3Identity::getInstanceProfile).orElse("");
                break;
            }
        }

        String auditLocation = getRangerAuditDir(cloudStorage);
        AccountMapping accountMapping = cloudStorage.getAccountMapping();
        String dataAccessRole = accountMapping.getUserMappings().get("hive");
        // There is no ranger in the IDBroker mapping, so we can use solr
        String rangerAuditRole = accountMapping.getUserMappings().get("solr");

        return String.format("Services running on the cluster were unable to write to %s location. " +
                        "This problem usually occurs due to cloud storage permission misconfiguration. " +
                        "Services on the cluster are using Data Access Role (%s) and Ranger Audit Role (%s) to write to the Ranger Audit location (%s), " +
                        "therefore please verify that these roles have write access to this location. " +
                        "During Data Lake cluster creation, CDP Control Plane attaches Assumer Instance Profile (%s) to the IDBroker Virtual Machine. " +
                        "IDBroker will then use it to assume the Data Access Role and Ranger Audit Role, therefore Assumer Instance Profile (%s) " +
                        "permissions must, at a minimum, allow to assume Data Access Role and Ranger Audit Role." +
                        "Refer to Cloudera documentation at %s for the required rights.",
                auditLocation, dataAccessRole, rangerAuditRole, auditLocation, assumerInstanceProfile, assumerInstanceProfile,
                DocumentationLinkProvider.awsCloudStorageSetupLink());
    }

    private String gcpError(CloudStorage cloudStorage) {

        String serviceAccountEmail = "";

        for (CloudIdentity cloudIdentity : cloudStorage.getCloudIdentities()) {
            if (CloudIdentityType.ID_BROKER == cloudIdentity.getIdentityType()) {
                serviceAccountEmail = Optional.ofNullable(cloudIdentity.getGcsIdentity()).map(GcsIdentity::getServiceAccountEmail).orElse("");
                break;
            }
        }

        String auditLocation = getRangerAuditDir(cloudStorage);
        AccountMapping accountMapping = cloudStorage.getAccountMapping();
        String dataAccessRole = accountMapping.getUserMappings().get("hive");
        // There is no ranger in the IDBroker mapping, so we can use solr
        String rangerAuditRole = accountMapping.getUserMappings().get("solr");

        return String.format("Services running on the cluster were unable to write to %s location. " +
                        "This problem usually occurs due to cloud storage permission misconfiguration. " +
                        "Services on the cluster are using Data Access Role (%s) and Ranger Audit Role (%s) to write to the Ranger Audit location (%s), " +
                        "therefore please verify that these roles have write access to this location. " +
                        "During Data Lake cluster creation, CDP Control Plane attaches Assumer Instance Profile (%s) to the IDBroker Virtual Machine. " +
                        "IDBroker will then use it to assume the Data Access Role and Ranger Audit Role, therefore Assumer Instance Profile (%s) " +
                        "permissions must, at a minimum, allow to assume Data Access Role and Ranger Audit Role." +
                        "Refer to Cloudera documentation at %s for the required rights.",
                auditLocation, dataAccessRole, rangerAuditRole, auditLocation, serviceAccountEmail, serviceAccountEmail,
                DocumentationLinkProvider.googleCloudStorageSetupLink());
    }

    private String azureError(CloudStorage cloudStorage) {

        String assumerIdentity = "";

        for (CloudIdentity cloudIdentity : cloudStorage.getCloudIdentities()) {
            if (CloudIdentityType.ID_BROKER == cloudIdentity.getIdentityType()) {
                assumerIdentity = Optional.ofNullable(cloudIdentity.getAdlsGen2Identity()).map(AdlsGen2Identity::getManagedIdentity).orElse("");
                break;
            }
        }

        String auditLocation = getRangerAuditDir(cloudStorage);
        AccountMapping accountMapping = cloudStorage.getAccountMapping();
        String dataAccessIdentity = accountMapping.getUserMappings().get("hive");
        // There is no ranger in the IDBroker mapping, so we can use solr
        String rangerAuditIdentity = accountMapping.getUserMappings().get("solr");

        return String.format("Services running on the cluster were unable to write to %s location. " +
                        "This problem usually occurs due to cloud storage permission misconfiguration. " +
                        "Services on the cluster are using Data Access Identity (%s) and Ranger Audit Identity (%s) to write to the " +
                        "Ranger Audit location (%s), therefore please verify that these roles have write access to this location. " +
                        "During Data Lake cluster creation, CDP Control Plane attaches Assumer Identity (%s) to the IDBroker Virtual Machine. " +
                        "IDBroker will then use it to attach the other managed identities to the IDBroker Virtual Machine, therefore Assumer Identity (%s) " +
                        "permissions must, at a minimum, allow to attach the Data Access Identity and Ranger Access Identity. " +
                        "Refer to Cloudera documentation at %s for the required rights.",
                auditLocation, dataAccessIdentity, rangerAuditIdentity, auditLocation, assumerIdentity, assumerIdentity,
                DocumentationLinkProvider.azureCloudStorageSetupLink());
    }

    private String getRangerAuditDir(CloudStorage cloudStorage) {
        if (cloudStorage.getLocations() != null) {
            for (StorageLocation location : cloudStorage.getLocations()) {
                if (CloudStorageCdpService.RANGER_AUDIT == location.getType()) {
                    return location.getValue();
                }
            }
        }
        return "";
    }
}
