package com.sequenceiq.cloudbreak.cloud.aws.common.consumption;

import java.util.Optional;

import javax.inject.Inject;
import javax.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsEbsCommonService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;

@Service
public class AwsEBSConsumptionCalculator implements ConsumptionCalculator {

    private static final StorageType EBS = StorageType.EBS;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEBSConsumptionCalculator.class);

    private static final long NO_BYTE_IN_MB = 1000L * 1000L;

    private static final int DATE_RANGE_WIDTH_IN_DAYS = 2;

    @Inject
    private AwsEbsCommonService awsEbsCommonService;

    @Override
    public void validate(CloudConsumption cloudConsumption) throws ValidationException {
        String storageLocation = cloudConsumption.getStorageLocation();
        if (storageLocation == null || !storageLocation.startsWith("vol-")) {
            throw new ValidationException(String.format("EBS id must start with 'vol-' if required file system type is 'EBS'!"));
        }
    }

    @Override
    public StorageSizeResponse calculate(StorageSizeRequest request) {
        DescribeVolumesResult result = awsEbsCommonService.getEbsSize(
                request.getCredential(),
                request.getRegion().value(),
                request.getStartTime(),
                request.getEndTime(),
                request.getObjectStoragePath());
        Optional<Volume> latestVolume = result.getVolumes().stream().findFirst();
        if (latestVolume.isPresent()) {
            Volume volume = latestVolume.get();
            LOGGER.debug("Gathered Volume from EBS: {}", volume);
            return StorageSizeResponse.builder().withStorageInBytes(volume.getSize() * NO_BYTE_IN_MB).build();
        } else {
            String message = String.format("No EBS were returned by ebs id %s and timeframe from %s to %s",
                    request.getObjectStoragePath(), request.getStartTime().toString(), request.getEndTime().toString());
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
        storageBuilder.setType(MeteringEventsProto.StorageType.Value.EBS);
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
        return EBS;
    }

    @Override
    public MeteringEventsProto.ServiceType.Value getMeteringServiceType() {
        return MeteringEventsProto.ServiceType.Value.DATAHUB;
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