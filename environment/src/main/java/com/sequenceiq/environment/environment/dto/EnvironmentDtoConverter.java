package com.sequenceiq.environment.environment.dto;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Component
public class EnvironmentDtoConverter {

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    public EnvironmentDtoConverter(Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
    }

    public EnvironmentDto environmentToDto(Environment environment) {
        EnvironmentDto.Builder builder = EnvironmentDto.builder()
                .withId(environment.getId())
                .withResourceCrn(environment.getResourceCrn())
                .withName(environment.getName())
                .withDescription(environment.getDescription())
                .withAccountId(environment.getAccountId())
                .withArchived(environment.isArchived())
                .withCloudPlatform(environment.getCloudPlatform())
                .withCredential(environment.getCredential())
                .withDeletionTimestamp(environment.getDeletionTimestamp())
                .withLocationDto(environmentToLocationDto(environment))
                .withRegions(environment.getRegions())
                .withEnvironmentStatus(environment.getStatus())
                .withCreator(environment.getCreator())
                .withCreateFreeIpa(environment.isCreateFreeIpa());
        if (environment.getNetwork() != null) {
            builder.withNetwork(environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform()))
                    .convertToDto(environment.getNetwork()));
        }
        return builder.build();
    }

    public Environment dtoToEnvironment(EnvironmentDto environmentDto) {
        Environment environment = new Environment();
        environment.setAccountId(environmentDto.getAccountId());
        environment.setArchived(environmentDto.isArchived());
        environment.setCloudPlatform(environmentDto.getCloudPlatform());
        environment.setCredential(environmentDto.getCredential());
        environment.setDeletionTimestamp(environmentDto.getDeletionTimestamp());
        environment.setDescription(environmentDto.getDescription());
        environment.setId(environmentDto.getId());
        environment.setLatitude(environmentDto.getLocation().getLatitude());
        environment.setLocation(environmentDto.getLocation().getName());
        environment.setLocationDisplayName(environmentDto.getLocation().getDisplayName());
        environment.setLongitude(environmentDto.getLocation().getLongitude());
        environment.setName(environmentDto.getName());
        if (environmentDto.getNetwork() != null) {
            environment.setNetwork(environmentNetworkConverterMap.get(
                    CloudPlatform.valueOf(environmentDto.getCloudPlatform())).convert(environmentDto));
        }
        environment.setResourceCrn(environmentDto.getResourceCrn());
        environment.setStatus(environmentDto.getStatus());
        environment.setCreator(environmentDto.getCreator());
        environment.setCreateFreeIpa(environmentDto.isCreateFreeIpa());
        return environment;
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
        environment.setStatus(EnvironmentStatus.CREATION_INITIATED);
        environment.setCreateFreeIpa(creationDto.isCreateFreeIpa());
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
