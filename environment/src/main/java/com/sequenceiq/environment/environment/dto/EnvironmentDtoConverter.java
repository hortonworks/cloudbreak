package com.sequenceiq.environment.environment.dto;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Component
public class EnvironmentDtoConverter {

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    public EnvironmentDtoConverter(Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
    }

    public EnvironmentDto environmentToDto(Environment environment) {
        return EnvironmentDto.EnvironmentDtoBuilder.anEnvironmentDto()
                .withName(environment.getName())
                .withDescription(environment.getDescription())
                .withAccountId(environment.getAccountId())
                .withArchived(environment.isArchived())
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(environment.getCredential())
                .withDeletionTimestamp(environment.getDeletionTimestamp())
                .withLocationDto(environmentToLocationDto(environment))
                //TODO: .withEnvironmentStatus(environment.getEnvironmentStatus())
                .withNetwork(environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform()))
                        .convertToDto(environment.getNetwork()))
                .build();
    }

    public Environment creationDtoToEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = new Environment();
        environment.setAccountId(creationDto.getAccountId());
        environment.setName(creationDto.getName());
        environment.setArchived(false);
        environment.setCloudPlatform(creationDto.getCloudPlatform());
        environment.setDescription(creationDto.getDescription());
        environment.setLatitude(creationDto.getLocation().getLatitude());
        environment.setLongitude(creationDto.getLocation().getLongitude());
        environment.setLocation(creationDto.getLocation().getName());
        environment.setLocationDisplayName(creationDto.getLocation().getDisplayName());
        return environment;
    }

    public LocationDto environmentToLocationDto(Environment environment) {
        return LocationDto.LocationDtoBuilder.aLocationDto()
                .withName(environment.getLocation())
                .withDisplayName(environment.getLocationDisplayName())
                .withLongitude(environment.getLongitude())
                .withLatitude(environment.getLatitude())
                .build();
    }
}
