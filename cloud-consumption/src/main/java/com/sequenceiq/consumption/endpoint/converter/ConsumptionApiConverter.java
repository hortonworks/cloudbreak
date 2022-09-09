package com.sequenceiq.consumption.endpoint.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.request.ConsumptionBaseRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto.Builder;

@Component
public class ConsumptionApiConverter {

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public ConsumptionCreationDto initCreationDtoForStorage(StorageConsumptionRequest request) {
        Builder builder = initCreationDtoBuilder(request, ConsumptionType.STORAGE);
        return builder
                .withStorageLocation(request.getStorageLocation())
                .build();
    }

    public ConsumptionCreationDto initCreationDtoForCloudResource(CloudResourceConsumptionRequest request, ConsumptionType consumptionType) {
        Builder builder = initCreationDtoBuilder(request, consumptionType);
        return builder
                .withStorageLocation(request.getCloudResourceId())
                .build();
    }

    private Builder initCreationDtoBuilder(ConsumptionBaseRequest request, ConsumptionType consumptionType) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return ConsumptionCreationDto.builder()
                .withName(generateName(request.getMonitoredResourceName(), consumptionType))
                .withAccountId(accountId)
                .withResourceCrn(regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CONSUMPTION, accountId))
                .withEnvironmentCrn(request.getEnvironmentCrn())
                .withConsumptionType(consumptionType)
                .withMonitoredResourceType(request.getMonitoredResourceType())
                .withMonitoredResourceCrn(request.getMonitoredResourceCrn());
    }

    private String generateName(String monitoredResourceName, ConsumptionType consumptionType) {
        return monitoredResourceName + '_' + consumptionType.name();
    }
}
