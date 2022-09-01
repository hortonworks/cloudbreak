package com.sequenceiq.cloudbreak.cloud;

import javax.validation.ValidationException;
import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeRequest;
import com.sequenceiq.cloudbreak.cloud.model.StorageSizeResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;

public interface ConsumptionCalculator {

    void validate(CloudConsumption cloudConsumption) throws ValidationException;

    StorageSizeResponse calculate(StorageSizeRequest request);

    String getObjectId(String objectId);

    MeteringEventsProto.StorageHeartbeat convertToStorageHeartbeat(CloudConsumption cloudConsumption, double sizeInBytes);

    int dateRangeInDays();

    StorageType storageType();

    CloudPlatform cloudPlatform();

}
