package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;

import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class EnvironmentModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentModificationService.class);

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentRepository environmentRepository;

    private final EnvironmentService environmentService;

    private final CredentialService credentialService;

    private final NetworkService networkService;

    public EnvironmentModificationService(EnvironmentDtoConverter environmentDtoConverter, EnvironmentRepository environmentRepository,
            EnvironmentService environmentService, CredentialService credentialService, NetworkService networkService) {
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentRepository = environmentRepository;
        this.environmentService = environmentService;
        this.credentialService = credentialService;
        this.networkService = networkService;
    }

    public EnvironmentDto edit(String environmentName, EnvironmentEditDto editDto) {
        Environment env = environmentService.getByNameForAccountId(environmentName, editDto.getAccountId());
        editDescriptionIfChanged(env, editDto);
        editLocationAndRegionsIfChanged(env, editDto);
        editNetworkIfChanged(env, editDto);
        Environment saved = environmentRepository.save(env);
        return environmentDtoConverter.environmentToDto(saved);
    }

    public EnvironmentDto changeCredential(String accountId, String environmentName, EnvironmentChangeCredentialDto dto) {
        Environment environment = environmentRepository.findByNameAndAccountId(environmentName, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with name '%s'", environmentName)));
        //CHECKSTYLE:OFF
        // TODO: 2019. 06. 03. also we have to check for SDXs and DistroXs what uses the given credential. If there is at least one, we have to update the crn reference through the other services
        //CHECKSTYLE:ON
        if (environment.getStatus() == AVAILABLE) {
            Credential credential = credentialService.getByNameForAccountId(dto.getCredentialName(), accountId);
            environment.setCredential(credential);
            LOGGER.debug("About to change credential on environment \"{}\"", environmentName);
            Environment saved = environmentRepository.save(environment);
            return environmentDtoConverter.environmentToDto(saved);
        } else {
            throw new BadRequestException(String.format("Credential cannot be changed since the given environment status is not %s", AVAILABLE.name()));
        }
    }

    private boolean locationAndRegionChanged(EnvironmentEditDto editDto) {
        return !CollectionUtils.isEmpty(editDto.getRegions())
                && locationChanged(editDto);
    }

    private boolean locationChanged(EnvironmentEditDto editDto) {
        return editDto.getLocation() != null && !editDto.getLocation().isEmpty();
    }

    private boolean networkChanged(EnvironmentEditDto editDto) {
        return editDto.getNetworkDto() != null;
    }

    private BaseNetwork createAndSetNetwork(Environment environment, NetworkDto networkDto) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        return networkService.createNetworkIfPossible(environment, networkDto, cloudPlatform);
    }

    private void editRegionsAndLocation(EnvironmentEditDto editDto, Environment environment, CloudRegions cloudRegions) {
        validateRegionAndLocation(editDto.getLocation(), editDto.getRegions(), cloudRegions, environment);
        environmentService.setLocation(environment, editDto.getLocation(), cloudRegions);
        environmentService.setRegions(environment, editDto.getRegions(), cloudRegions);
    }

    private void editLocation(EnvironmentEditDto editDto, Environment environment, CloudRegions cloudRegions) {
        Set<String> regions = environment.getRegionSet().stream()
                .map(Region::getName).collect(Collectors.toSet());
        validateRegionAndLocation(editDto.getLocation(), regions, cloudRegions, environment);
        environmentService.setLocation(environment, editDto.getLocation(), cloudRegions);
    }

    private void editRegions(EnvironmentEditDto request, Environment environment, CloudRegions cloudRegions) {
        LocationDto locationDto = environmentDtoConverter.environmentToLocationDto(environment);
        validateRegionAndLocation(locationDto, request.getRegions(), cloudRegions, environment);
        environmentService.setRegions(environment, request.getRegions(), cloudRegions);
    }

    private void validateRegionAndLocation(LocationDto location, Set<String> requestedRegions,
            CloudRegions cloudRegions, Environment environment) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = environmentService.getValidatorService().validateRegions(requestedRegions,
                cloudRegions, environment.getCloudPlatform(), ValidationResult.builder());
        environmentService.getValidatorService().validateLocation(location, requestedRegions, environment, validationResultBuilder);
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void editDescriptionIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (StringUtils.isNotEmpty(editDto.getDescription())) {
            environment.setDescription(editDto.getDescription());
        }
    }

    private void editLocationAndRegionsIfChanged(Environment environment, EnvironmentEditDto editDto) {
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        if (locationAndRegionChanged(editDto)) {
            editRegionsAndLocation(editDto, environment, cloudRegions);
        } else if (locationChanged(editDto)) {
            editLocation(editDto, environment, cloudRegions);
        } else if (!CollectionUtils.isEmpty(editDto.getRegions())) {
            LocationDto locationDto = environmentDtoConverter.environmentToLocationDto(environment);
            editDto.setLocation(locationDto);
            editRegions(editDto, environment, cloudRegions);
        }
    }

    private void editNetworkIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (networkChanged(editDto)) {
            BaseNetwork network = createAndSetNetwork(environment, editDto.getNetworkDto());
            if (network != null) {
                environment.setNetwork(network);
            }
        }
    }

}
