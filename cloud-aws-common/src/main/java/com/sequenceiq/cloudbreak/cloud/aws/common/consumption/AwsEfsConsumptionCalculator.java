package com.sequenceiq.cloudbreak.cloud.aws.common.consumption;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsEfsCommonService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;
import com.sequenceiq.common.model.FileSystemType;

import software.amazon.awssdk.services.efs.model.DescribeFileSystemsResponse;
import software.amazon.awssdk.services.efs.model.FileSystemDescription;

@Service
public class AwsEfsConsumptionCalculator implements ConsumptionCalculator {

    private static final StorageType EFS = StorageType.EFS;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEfsConsumptionCalculator.class);

    private static final long NO_BYTE_IN_MB = 1000L * 1000L;

    private static final int DATE_RANGE_WIDTH_IN_DAYS = 2;

    @Inject
    private AwsEfsCommonService awsEfsCommonService;

    @Override
    public void validate(CloudConsumption cloudConsumption) throws ValidationException {
        String storageLocation = cloudConsumption.getStorageLocation();
        if (storageLocation == null || !storageLocation.startsWith("fs-")) {
            throw new ValidationException(String.format("EFS id must start with 'fs-' if required file system type is '%s'!",
                    FileSystemType.EFS.name()));
        }
    }

    public Set<StorageSizeResponse> calculate(StorageSizeRequest request) {
        DescribeFileSystemsResponse result = awsEfsCommonService.getEfsSize(
                request.getCredential(),
                request.getRegion().value(),
                request.getStartTime(),
                request.getEndTime(),
                request.getFirstCloudObjectId());
        Optional<FileSystemDescription> latestFileSystemDescription = result.fileSystems().stream().findFirst();
        if (latestFileSystemDescription.isPresent()) {
            FileSystemDescription fileSystemDescription = latestFileSystemDescription.get();
            LOGGER.debug("Gathered FileSystemDescription from EFS: {}", fileSystemDescription);
            StorageSizeResponse storageSizeResponse = StorageSizeResponse.builder()
                    .withStorageInBytes(fileSystemDescription.sizeInBytes().value())
                    .build();
            return Set.of(storageSizeResponse);
        } else {
            String message = String.format("Unable to describe EFS volume with ID %s", request.getCloudObjectIdsString());
            LOGGER.error(message);
            throw new CloudConnectorException(message);
        }
    }

    @Override
    public String getObjectId(String objectId) {
        return objectId;
    }

    @Override
    public MeteringEventsProto.StorageHeartbeat convertToStorageHeartbeat(CloudConsumption cloudConsumption, double sizeInBytes) {
        MeteringEventsProto.StorageHeartbeat.Builder storageHeartbeatBuilder = MeteringEventsProto.StorageHeartbeat.newBuilder();
        validate(cloudConsumption);
        MeteringEventsProto.Storage.Builder storageBuilder = MeteringEventsProto.Storage.newBuilder();
        storageBuilder.setId(getObjectId(cloudConsumption.getStorageLocation()));
        storageBuilder.setSizeInMB(sizeInBytes / NO_BYTE_IN_MB);
        storageBuilder.setType(MeteringEventsProto.StorageType.Value.EFS);
        storageHeartbeatBuilder.addStorages(storageBuilder.build());

        MeteringEventsProto.MeteredResourceMetadata.Builder metaBuilder = MeteringEventsProto.MeteredResourceMetadata.newBuilder();
        metaBuilder.setEnvironmentCrn(cloudConsumption.getEnvironmentCrn());
        storageHeartbeatBuilder.setMeteredResourceMetadata(metaBuilder.build());

        MeteringEventsProto.StorageHeartbeat ret = storageHeartbeatBuilder.build();
        LOGGER.debug("Converted StorageHeartbeat event: {}", ret);
        return ret;
    }

    @Override
    public int dateRangeInDays() {
        return DATE_RANGE_WIDTH_IN_DAYS;
    }

    @Override
    public StorageType storageType() {
        return EFS;
    }

    @Override
    public MeteringEventsProto.ServiceType.Value getMeteringServiceType() {
        return MeteringEventsProto.ServiceType.Value.ENVIRONMENT;
    }

    @Override
    public MeteringEventsProto.ServiceFeature.Value getServiceFeature() {
        return MeteringEventsProto.ServiceFeature.Value.FILE_STORAGE;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }
}
