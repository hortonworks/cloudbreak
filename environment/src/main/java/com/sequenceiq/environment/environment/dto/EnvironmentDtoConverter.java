package com.sequenceiq.environment.environment.dto;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;

@Component
public class EnvironmentDtoConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDtoConverter.class);

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final CredentialViewConverter credentialViewConverter;

    public EnvironmentDtoConverter(Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap,
            Map<CloudPlatform, EnvironmentParametersConverter> environmentParamsConverterMap,
            AuthenticationDtoConverter authenticationDtoConverter, CredentialViewConverter credentialViewConverter) {
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.environmentParamsConverterMap = environmentParamsConverterMap;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.credentialViewConverter = credentialViewConverter;
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
                .withCredentialView(credentialViewConverter.convert(environment.getCredential()))
                .withDeletionTimestamp(environment.getDeletionTimestamp())
                .withLocationDto(environmentToLocationDto(environment))
                .withRegions(environment.getRegionSet())
                .withTelemetry(environment.getTelemetry())
                .withEnvironmentStatus(environment.getStatus())
                .withCreator(environment.getCreator())
                .withAuthentication(authenticationDtoConverter.authenticationToDto(environment.getAuthentication()))
                .withCreateFreeIpa(environment.isCreateFreeIpa())
                .withCreated(environment.getCreated())
                .withStatusReason(environment.getStatusReason())
                .withExperimentalFeatures(environment.getExperimentalFeatures())
                .withSecurityAccess(environmentToSecurityAccessDto(environment))
                .withAdminGroupName(environment.getAdminGroupName());

        doIfNotNull(environment.getParameters(), parameters -> builder.withParameters(
                environmentParamsConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform())).convertToDto(parameters)));
        doIfNotNull(environment.getNetwork(), network -> builder.withNetwork(
                environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform())).convertToDto(network)));
        return builder.build();
    }

    public Environment creationDtoToEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = new Environment();
        environment.setAccountId(creationDto.getAccountId());
        environment.setCreator(creationDto.getCreator());
        environment.setName(creationDto.getName());
        environment.setArchived(false);
        environment.setCloudPlatform(creationDto.getCloudPlatform());
        environment.setDescription(creationDto.getDescription());
        environment.setLatitude(creationDto.getLocation().getLatitude());
        environment.setLongitude(creationDto.getLocation().getLongitude());
        environment.setLocation(creationDto.getLocation().getName());
        environment.setTelemetry(creationDto.getTelemetry());
        environment.setLocationDisplayName(creationDto.getLocation().getDisplayName());
        environment.setStatus(EnvironmentStatus.CREATION_INITIATED);
        environment.setCreateFreeIpa(creationDto.isCreateFreeIpa());
        environment.setAdminGroupName(creationDto.getAdminGroupName());
        environment.setCreated(System.currentTimeMillis());
        environment.setExperimentalFeatures(creationDto.getExperimentalFeatures());
        return environment;
    }

    public LocationDto environmentToLocationDto(Environment environment) {
        return LocationDto.builder()
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
