package com.sequenceiq.cloudbreak.cloud;

import java.util.Set;

import jakarta.validation.ValidationException;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;

public interface ConsumptionCalculator {

    void validate(CloudConsumption cloudConsumption) throws ValidationException;

    Set<StorageSizeResponse> calculate(StorageSizeRequest request);

    String getObjectId(String objectId);

    MeteringEventsProto.StorageHeartbeat convertToStorageHeartbeat(CloudConsumption cloudConsumption, double sizeInBytes);

    int dateRangeInDays();

    StorageType storageType();

    MeteringEventsProto.ServiceType.Value getMeteringServiceType();

    MeteringEventsProto.ServiceFeature.Value getServiceFeature();

    CloudPlatform cloudPlatform();
}
