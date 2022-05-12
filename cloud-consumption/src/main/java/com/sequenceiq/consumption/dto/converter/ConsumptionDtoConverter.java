package com.sequenceiq.consumption.dto.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;

@Component
public class ConsumptionDtoConverter {

    public Consumption creationDtoToConsumption(ConsumptionCreationDto creationDto) {
        Consumption consumption = new Consumption();
        consumption.setName(creationDto.getName());
        consumption.setDescription(creationDto.getDescription());
        consumption.setAccountId(creationDto.getAccountId());
        consumption.setResourceCrn(creationDto.getResourceCrn());
        consumption.setEnvironmentCrn(creationDto.getEnvironmentCrn());
        consumption.setMonitoredResourceType(creationDto.getMonitoredResourceType());
        consumption.setMonitoredResourceCrn(creationDto.getMonitoredResourceCrn());
        consumption.setConsumptionType(creationDto.getConsumptionType());
        consumption.setStorageLocation(creationDto.getStorageLocation());
        return consumption;
    }
}