package com.sequenceiq.datalake.service.sdx;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@Service
public class CloudStorageManifester {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageManifester.class);

    @Inject
    private CloudbreakServiceUserCrnClient cloudbreakClient;

    public CloudStorageRequest initCloudStorageRequest(String cloudPlatform, String blueprint, SdxCluster sdxCluster, SdxClusterRequest clusterRequest) {
        SdxCloudStorageRequest cloudStorage = clusterRequest.getCloudStorage();
        validateCloudStorage(cloudPlatform, cloudStorage);

        FileSystemParameterV4Responses fileSystemRecommendations = getFileSystemRecommendations(sdxCluster.getInitiatorUserCrn(),
                blueprint,
                sdxCluster.getClusterName(),
                cloudStorage);

        LOGGER.info("File recommendations {}", fileSystemRecommendations);

        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        setStorageLocations(fileSystemRecommendations, cloudStorageRequest);
        setIdentities(cloudStorage, cloudStorageRequest);
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

    private void setIdentities(SdxCloudStorageRequest cloudStorage, CloudStorageRequest cloudStorageRequest) {
        StorageIdentityBase logStorageIdentity = new StorageIdentityBase();
        logStorageIdentity.setType(CloudIdentityType.LOG);
        FileSystemType fileSystemType = cloudStorage.getFileSystemType();
        if (fileSystemType.isS3()) {
            S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
            s3CloudStorageV1Parameters.setInstanceProfile(cloudStorage.getS3().getInstanceProfile());
            logStorageIdentity.setS3(s3CloudStorageV1Parameters);
        } else if (fileSystemType.isWasb()) {
            WasbCloudStorageV1Parameters wasbCloudStorageV1Parameters = new WasbCloudStorageV1Parameters();
            WasbCloudStorageV1Parameters wasb = cloudStorage.getWasb();
            wasbCloudStorageV1Parameters.setSecure(wasb.isSecure());
            wasbCloudStorageV1Parameters.setAccountName(wasb.getAccountName());
            wasbCloudStorageV1Parameters.setAccountKey(wasb.getAccountKey());
            logStorageIdentity.setWasb(wasbCloudStorageV1Parameters);
        }
        cloudStorageRequest.setIdentities(List.of(logStorageIdentity));
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
