package com.sequenceiq.datalake.service.sdx;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.S3Guard;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@Service
public class CloudStorageManifester {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageManifester.class);

    @Inject
    private CloudbreakServiceUserCrnClient cloudbreakClient;

    public CloudStorageRequest initCloudStorageRequest(DetailedEnvironmentResponse environment, String blueprint,
        SdxCluster sdxCluster, SdxClusterRequest clusterRequest) {
        SdxCloudStorageRequest cloudStorage = clusterRequest.getCloudStorage();
        validateCloudStorage(environment.getCloudPlatform(), cloudStorage);

        FileSystemParameterV4Responses fileSystemRecommendations = getFileSystemRecommendations(sdxCluster.getInitiatorUserCrn(),
                blueprint,
                sdxCluster.getClusterName(),
                cloudStorage);

        LOGGER.info("File recommendations {}", fileSystemRecommendations);

        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        setStorageLocations(fileSystemRecommendations, cloudStorageRequest);
        setStorageParameters(environment, cloudStorageRequest);
        setIdentities(environment, cloudStorage, cloudStorageRequest);
        return cloudStorageRequest;
    }

    private void setStorageLocations(FileSystemParameterV4Responses fileSystemRecommendations, CloudStorageRequest cloudStorageRequest) {
        List<StorageLocationBase> storageLocations = fileSystemRecommendations.getResponses().stream().map(response -> {
            StorageLocationBase storageLocation = new StorageLocationBase();
            storageLocation.setValue(response.getDefaultPath());
            storageLocation.setType(response.getType());
            return storageLocation;
        }).collect(Collectors.toList());
        cloudStorageRequest.setLocations(storageLocations);
    }

    private void setStorageParameters(DetailedEnvironmentResponse environment, CloudStorageRequest cloudStorageRequest) {
        if (isS3GuardConfigured(environment)) {
            if (!Strings.isNullOrEmpty(environment.getAws().getS3guard().getDynamoDbTableName())) {
                AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
                S3Guard s3Guard = new S3Guard();
                s3Guard.setDynamoTableName(environment.getAws().getS3guard().getDynamoDbTableName());
                awsStorageParameters.setS3Guard(s3Guard);
                cloudStorageRequest.setAws(awsStorageParameters);
            }
        }
    }

    private boolean isS3GuardConfigured(DetailedEnvironmentResponse environment) {
        return environment.getAws() != null && environment.getAws().getS3guard() != null;
    }

    private void setIdentities(DetailedEnvironmentResponse environment,
        SdxCloudStorageRequest cloudStorage,
        CloudStorageRequest cloudStorageRequest) {
        addIdBrokerIdentity(cloudStorage, cloudStorageRequest);
        addLogIdentity(environment, cloudStorageRequest);
    }

    private void addIdBrokerIdentity(SdxCloudStorageRequest cloudStorage, CloudStorageRequest cloudStorageRequest) {
        StorageIdentityBase idBroker = new StorageIdentityBase();
        idBroker.setType(CloudIdentityType.ID_BROKER);
        FileSystemType fileSystemType = cloudStorage.getFileSystemType();
        if (isFileSystemConfigured(fileSystemType)) {
            if (fileSystemType.isS3()) {
                idBroker.setS3(cloudStorage.getS3());
            } else if (fileSystemType.isWasb()) {
                idBroker.setWasb(cloudStorage.getWasb());
            }
            cloudStorageRequest.getIdentities().add(idBroker);
        }
    }

    private boolean isFileSystemConfigured(FileSystemType fileSystemType) {
        return fileSystemType != null;
    }

    private void addLogIdentity(DetailedEnvironmentResponse environment, CloudStorageRequest cloudStorageRequest) {
        StorageIdentityBase log = new StorageIdentityBase();
        log.setType(CloudIdentityType.LOG);
        if (isLoggingConfigured(environment)) {
            LoggingResponse logging = environment.getTelemetry().getLogging();
            if (logging.getS3() != null) {
                log.setS3(logging.getS3());
            } else if (logging.getWasb() != null) {
                log.setWasb(logging.getWasb());
            }
            cloudStorageRequest.getIdentities().add(log);
        }
    }

    private boolean isLoggingConfigured(DetailedEnvironmentResponse environment) {
        return environment.getTelemetry() != null && environment.getTelemetry().getLogging() != null;
    }

    private void validateCloudStorage(String cloudPlatform, SdxCloudStorageRequest cloudStorage) {
        if (CloudPlatform.AWS.name().equalsIgnoreCase(cloudPlatform)) {
            if (cloudStorage.getS3() == null || StringUtils.isEmpty(cloudStorage.getS3().getInstanceProfile())) {
                throw new BadRequestException("instance profile must be defined for S3");
            }
        }
    }

    private FileSystemParameterV4Responses getFileSystemRecommendations(String userCrn, String blueprint,
            String clusterName, SdxCloudStorageRequest cloudStorageRequest) {
        return cloudbreakClient.withCrn(userCrn).filesystemV4Endpoint()
                .getFileSystemParameters(0L,
                        blueprint,
                        clusterName,
                        "",
                        cloudStorageRequest.getBaseLocation(),
                        cloudStorageRequest.getFileSystemType().toString(),
                        false,
                        false);
    }

}
