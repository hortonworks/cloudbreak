package com.sequenceiq.cloudbreak.cloud.aws.common.consumption;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsEbsCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsInstanceCommonService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

@Service
public class AwsEbsConsumptionCalculator implements ConsumptionCalculator {

    private static final StorageType EBS = StorageType.EBS;

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEbsConsumptionCalculator.class);

    private static final long NO_BYTE_FROM_GB = 1024L * 1024L * 1024L;

    private static final long NO_BYTE_IN_MB = 1024L * 1024L;

    private static final int DATE_RANGE_WIDTH_IN_DAYS = 2;

    @Inject
    private AwsEbsCommonService awsEbsCommonService;

    @Inject
    private AwsInstanceCommonService awsInstanceCommonService;

    @Override
    public void validate(CloudConsumption cloudConsumption) {
        String cloudProviderId = cloudConsumption.getStorageLocation();
        if (cloudProviderId == null || (!isEbs(cloudProviderId) && !isInstance(cloudProviderId) && !isDataHub(cloudProviderId))) {
            throw new ValidationException("EBS id must start with 'vol-' or 'i-' or 'datahub crn' if required file system type is 'EBS'!");
        }
    }

    @Override
    public Set<StorageSizeResponse> calculate(StorageSizeRequest request) {
        Set<String> allObjectStoragePath = request.getCloudObjectIds();
        Set<StorageSizeResponse> result = new HashSet<>();
        for (String objectStoragePath : allObjectStoragePath) {
            if (isInstance(objectStoragePath)) {
                Set<String> attachedVolumes = awsInstanceCommonService.getAttachedVolumes(request.getCredential(),
                        request.getRegion().value(),
                        objectStoragePath);
                for (String attachedVolume : attachedVolumes) {
                    Optional<StorageSizeResponse> storageSizeResponse = getStorageSizeResponse(request, attachedVolume);
                    if (storageSizeResponse.isPresent()) {
                        result.add(storageSizeResponse.get());
                    }
                }
            } else {
                Optional<StorageSizeResponse> storageSizeResponse = getStorageSizeResponse(
                        request,
                        objectStoragePath);
                if (storageSizeResponse.isPresent()) {
                    result.add(storageSizeResponse.get());
                }
            }
        }
        return result;
    }

    private Optional<StorageSizeResponse> getStorageSizeResponse(StorageSizeRequest request, String ebsId) {
        Optional<StorageSizeResponse> result = Optional.empty();
        Optional<DescribeVolumesResponse> describeVolumesResponse = awsEbsCommonService.getEbsSize(
                request.getCredential(),
                request.getRegion().value(),
                ebsId);
        if (describeVolumesResponse.isPresent()) {
            Optional<Volume> volumeOptional = describeVolumesResponse.get().volumes().stream().findFirst();
            if (volumeOptional.isPresent()) {
                Volume volume = volumeOptional.get();
                LOGGER.debug("Gathered Volume from EBS: {}", volume);
                result = Optional.of(
                        StorageSizeResponse.builder()
                                .withStorageInBytes(volume.size() * NO_BYTE_FROM_GB)
                                .build());
            } else {
                String message = String.format("Unable to describe EBS volume with ID %s", request.getCloudObjectIdsString());
                LOGGER.error(message);
                throw new CloudConnectorException(message);
            }
        }
        return result;
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
        return MeteringEventsProto.ServiceFeature.Value.LOW_LATENCY_STORAGE;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    private boolean isEbs(String storageLocation) {
        return storageLocation.startsWith("vol-");
    }

    private boolean isInstance(String storageLocation) {
        return storageLocation.startsWith("i-");
    }

    private boolean isDataHub(String storageLocation) {
        return storageLocation.startsWith("crn:cdp:datahub") || storageLocation.startsWith("crn:cdp-us-gov:datahub");
    }
}
