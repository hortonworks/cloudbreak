package com.sequenceiq.datalake.service.sdx;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceUserCrnClient;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@Service
public class CloudStorageManifester {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageManifester.class);

    @Inject
    private CloudbreakServiceUserCrnClient cloudbreakClient;

    public CloudStorageV4Request getCloudStorageConfig(String cloudPlatform, String blueprint, SdxCluster sdxCluster, SdxClusterRequest clusterRequest) {
        SdxCloudStorageRequest cloudStorage = clusterRequest.getCloudStorage();
        validateCloudStorage(cloudPlatform, cloudStorage);

        FileSystemParameterV4Responses fileSystemRecommendations = getFileSystemRecommendations(sdxCluster.getInitiatorUserCrn(),
                blueprint,
                sdxCluster.getClusterName(),
                cloudStorage);

        LOGGER.info("File recommendations {}", fileSystemRecommendations);

        CloudStorageV4Request cloudStorageV4Request = new CloudStorageV4Request();
        Set<StorageLocationV4Request> locations = new HashSet<>();
        for (FileSystemParameterV4Response response : fileSystemRecommendations.getResponses()) {
            StorageLocationV4Request sl = new StorageLocationV4Request();
            sl.setPropertyName(response.getPropertyName());
            sl.setPropertyFile(response.getPropertyFile());
            sl.setValue(response.getDefaultPath());
            locations.add(sl);
        }
        cloudStorageV4Request.setLocations(locations);
        cloudStorageV4Request.setS3(cloudStorage.getS3());
        return cloudStorageV4Request;
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
