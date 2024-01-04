package com.sequenceiq.cloudbreak.cloud.aws.common.consumption;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsCloudWatchCommonService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;
import com.sequenceiq.common.model.FileSystemType;

import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;

@Service
public class AwsS3ConsumptionCalculator implements ConsumptionCalculator {

    private static final StorageType S3 = StorageType.S3;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsS3ConsumptionCalculator.class);

    private static final int DATE_RANGE_WIDTH_IN_DAYS = 2;

    private static final long NO_BYTE_IN_MB = 1000L * 1000L;

    @Inject
    private AwsCloudWatchCommonService cloudWatchCommonService;

    @Override
    public void validate(CloudConsumption cloudConsumption) throws ValidationException {
        String storageLocation = cloudConsumption.getStorageLocation();
        if (storageLocation == null || !storageLocation.startsWith(FileSystemType.S3.getProtocol())) {
            throw new ValidationException(String.format("Storage location must start with '%s' if required file system type is '%s'!",
                    FileSystemType.S3.getProtocol(), FileSystemType.S3.name()));
        }
    }

    @Override
    public Set<StorageSizeResponse> calculate(StorageSizeRequest request) {
        GetMetricStatisticsResponse result = cloudWatchCommonService.getBucketSize(request.getCredential(), request.getRegion().value(),
                request.getStartTime(), request.getEndTime(), request.getFirstCloudObjectId());
        Optional<Datapoint> latestDatapoint = result.datapoints().stream().max(Comparator.comparing(Datapoint::timestamp));
        if (latestDatapoint.isPresent()) {
            Datapoint datapoint = latestDatapoint.get();
            LOGGER.debug("Gathered datapoint from CloudWatch: {}", datapoint);
            StorageSizeResponse storageSizeResponse = StorageSizeResponse.builder()
                    .withStorageInBytes(datapoint.maximum())
                    .build();
            return Set.of(storageSizeResponse);
        } else {
            String message = String.format("No datapoints were returned by CloudWatch for bucket %s and timeframe from %s to %s",
                    request.getCloudObjectIdsString(), request.getStartTime().toString(), request.getEndTime().toString());
            LOGGER.error(message);
            throw new CloudConnectorException(message);
        }
    }

    @Override
    public String getObjectId(String objectId) {
        objectId = objectId.replace(FileSystemType.S3.getProtocol() + "://", "");
        return objectId.split("/")[0];
    }

    @Override
    public MeteringEventsProto.StorageHeartbeat convertToStorageHeartbeat(CloudConsumption cloudConsumption, double sizeInBytes) {
        MeteringEventsProto.StorageHeartbeat.Builder storageHeartbeatBuilder = MeteringEventsProto.StorageHeartbeat.newBuilder();
        validate(cloudConsumption);
        MeteringEventsProto.Storage.Builder storageBuilder = MeteringEventsProto.Storage.newBuilder();
        storageBuilder.setId(getObjectId(cloudConsumption.getStorageLocation()));
        storageBuilder.setSizeInMB(sizeInBytes / NO_BYTE_IN_MB);
        storageBuilder.setType(MeteringEventsProto.StorageType.Value.S3);
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
        return S3;
    }

    @Override
    public MeteringEventsProto.ServiceType.Value getMeteringServiceType() {
        return MeteringEventsProto.ServiceType.Value.ENVIRONMENT;
    }

    @Override
    public MeteringEventsProto.ServiceFeature.Value getServiceFeature() {
        return MeteringEventsProto.ServiceFeature.Value.OBJECT_STORAGE;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }
}
