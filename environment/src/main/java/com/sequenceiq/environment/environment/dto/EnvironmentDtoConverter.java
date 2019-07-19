package com.sequenceiq.environment.environment.dto;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Component
public class EnvironmentDtoConverter {

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    public EnvironmentDtoConverter(Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap,
            AuthenticationDtoConverter authenticationDtoConverter) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.authenticationDtoConverter = authenticationDtoConverter;
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
                .withTelemetry(environment.getTelemetry())
                .withEnvironmentStatus(environment.getStatus())
                .withCreator(environment.getCreator())
                .withCreateFreeIpa(environment.isCreateFreeIpa())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environment.getAuthentication()))
                .withCreateFreeIpa(environment.isCreateFreeIpa())
                .withCreated(environment.getCreated())
                .withStatusReason(environment.getStatusReason())
                .withTunnel(environment.getTunnel())
                .withSecurityAccess(environmentToSecurityAccessDto(environment))
                .withIdBrokerMappingSource(environment.getIdBrokerMappingSource());
        if (environment.getNetwork() != null) {
            builder.withNetwork(environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform()))
                    .convertToDto(environment.getNetwork()));
        }
        return builder.build();
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
        if (creationDto.getTelemetry() != null) {
            environment.setTelemetry(new Json(creationDto.getTelemetry()));
        }
        environment.setLocationDisplayName(creationDto.getLocation().getDisplayName());
        environment.setStatus(EnvironmentStatus.CREATION_INITIATED);
        environment.setCreateFreeIpa(creationDto.isCreateFreeIpa());
        environment.setTunnel(creationDto.getTunnel());
        environment.setIdBrokerMappingSource(creationDto.getIdBrokerMappingSource());
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

    private SecurityAccessDto environmentToSecurityAccessDto(Environment environment) {
        return SecurityAccessDto.builder()
                .withCidr(environment.getCidr())
                .withSecurityGroupIdForKnox(environment.getSecurityGroupIdForKnox())
                .withDefaultSecurityGroupId(environment.getDefaultSecurityGroupId())
                .build();
    }
}
