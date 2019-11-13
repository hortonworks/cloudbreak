package com.sequenceiq.environment.environment.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@Service
public class EnvironmentModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentModificationService.class);

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentService environmentService;

    private final CredentialService credentialService;

    private final NetworkService networkService;

    private final AuthenticationDtoConverter authenticationDtoConverter;

    private final ParametersService parametersService;

    private final EnvironmentFlowValidatorService environmentFlowValidatorService;

    public EnvironmentModificationService(EnvironmentDtoConverter environmentDtoConverter, EnvironmentRepository environmentRepository,
            EnvironmentService environmentService, CredentialService credentialService, NetworkService networkService,
            AuthenticationDtoConverter authenticationDtoConverter, ParametersService parametersService,
            EnvironmentFlowValidatorService environmentFlowValidatorService) {
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentService = environmentService;
        this.credentialService = credentialService;
        this.networkService = networkService;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.environmentFlowValidatorService = environmentFlowValidatorService;
    }

    public EnvironmentDto editByName(String environmentName, EnvironmentEditDto editDto) {
        Environment env = environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(environmentName, editDto.getAccountId())
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with name '%s'", environmentName)));
        return edit(editDto, env);
    }

    public EnvironmentDto editByCrn(String crn, EnvironmentEditDto editDto) {
        Environment env = environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(crn, editDto.getAccountId())
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with crn '%s'", crn)));
        return edit(editDto, env);
    }

    public EnvironmentDto changeCredentialByEnvironmentName(String accountId, String environmentName, EnvironmentChangeCredentialDto dto) {
        Environment environment = environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(environmentName, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with name '%s'", environmentName)));
        return changeCredential(accountId, environmentName, dto, environment);
    }

    public EnvironmentDto changeCredentialByEnvironmentCrn(String accountId, String crn, EnvironmentChangeCredentialDto dto) {
        Environment environment = environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(crn, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with CRN '%s'", crn)));
        return changeCredential(accountId, crn, dto, environment);
    }

    private EnvironmentDto edit(EnvironmentEditDto editDto, Environment env) {
        editDescriptionIfChanged(env, editDto);
        editLocationAndRegionsIfChanged(env, editDto);
        editTelemetryIfChanged(env, editDto);
        editNetworkIfChanged(env, editDto);
        editAdminGroupNameIfChanged(env, editDto);
        editAuthenticationIfChanged(editDto, env);
        editSecurityAccessIfChanged(editDto, env);
        editIdBrokerMappingSource(editDto, env);
        editTunnelIfChanged(editDto, env);
        editEnvironmentParameters(editDto, env);
        Environment saved = environmentService.save(env);
        return environmentDtoConverter.environmentToDto(saved);
    }

    private EnvironmentDto changeCredential(String accountId, String environmentName, EnvironmentChangeCredentialDto dto, Environment environment) {
        //CHECKSTYLE:OFF
        // TODO: 2019. 06. 03. also we have to check for SDXs and DistroXs what uses the given credential. If there is at least one, we have to update the crn reference through the other services
        //CHECKSTYLE:ON
        Credential credential = credentialService.getByNameForAccountId(dto.getCredentialName(), accountId);
        environment.setCredential(credential);
        LOGGER.debug("About to change credential on environment \"{}\"", environmentName);
        Environment saved = environmentService.save(environment);
        return environmentDtoConverter.environmentToDto(saved);
    }

    private boolean locationAndRegionChanged(EnvironmentEditDto editDto) {
        return !CollectionUtils.isEmpty(editDto.getRegions())
                && locationChanged(editDto);
    }

    private boolean locationChanged(EnvironmentEditDto editDto) {
        return editDto.getLocation() != null && !editDto.getLocation().isEmpty();
    }

    private void editNetworkIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (networkChanged(editDto)) {
            Optional<BaseNetwork> original = networkService.findByEnvironment(environment.getId());
            original.ifPresent(baseNetwork -> editDto.getNetworkDto().setId(baseNetwork.getId()));
            BaseNetwork network = createAndSetNetwork(environment, editDto.getNetworkDto(), editDto.getAccountId());
            if (network != null) {
                environment.setNetwork(network);
            }
        }
    }

    private boolean networkChanged(EnvironmentEditDto editDto) {
        return editDto.getNetworkDto() != null;
    }

    private BaseNetwork createAndSetNetwork(Environment environment, NetworkDto networkCreationDto, String accountId) {
        Map<String, CloudSubnet> subnetMetas = networkService.retrieveSubnetMetadata(environment, networkCreationDto);
        return networkService.saveNetwork(environment, networkCreationDto, accountId, subnetMetas);
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

    private void editRegionsAndLocation(EnvironmentEditDto editDto, Environment environment, CloudRegions cloudRegions) {
        validateRegionAndLocation(editDto.getLocation(), editDto.getRegions(), cloudRegions, environment);
        environmentService.setLocation(environment, editDto.getLocation(), cloudRegions);
        environmentService.setRegions(environment, editDto.getRegions(), cloudRegions);
    }

    private void editRegions(EnvironmentEditDto request, Environment environment, CloudRegions cloudRegions) {
        LocationDto locationDto = environmentDtoConverter.environmentToLocationDto(environment);
        validateRegionAndLocation(locationDto, request.getRegions(), cloudRegions, environment);
        environmentService.setRegions(environment, request.getRegions(), cloudRegions);
    }

    private void validateRegionAndLocation(LocationDto location, Set<String> requestedRegions,
            CloudRegions cloudRegions, Environment environment) {
        ValidationResult validationResult = environmentService.getValidatorService()
                .validateRegionsAndLocation(location.getName(), requestedRegions, environment, cloudRegions)
                .build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void editLocation(EnvironmentEditDto editDto, Environment environment, CloudRegions cloudRegions) {
        Set<String> regions = environment.getRegionSet().stream()
                .map(Region::getName).collect(Collectors.toSet());
        validateRegionAndLocation(editDto.getLocation(), regions, cloudRegions, environment);
        environmentService.setLocation(environment, editDto.getLocation(), cloudRegions);
    }

    private void editAuthenticationIfChanged(EnvironmentEditDto editDto, Environment environment) {
        AuthenticationDto authenticationDto = editDto.getAuthentication();
        if (authenticationDto != null) {
            environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(authenticationDto));
        }
    }

    private void editTunnelIfChanged(EnvironmentEditDto editDto, Environment environment) {
        if (editDto.getTunnel() != null) {
            ExperimentalFeatures experimentalFeaturesJson = environment.getExperimentalFeaturesJson();
            experimentalFeaturesJson.setTunnel(editDto.getTunnel());
            environment.setExperimentalFeaturesJson(experimentalFeaturesJson);
        }
    }

    private void editSecurityAccessIfChanged(EnvironmentEditDto editDto, Environment environment) {
        SecurityAccessDto securityAccessDto = editDto.getSecurityAccess();
        if (securityAccessDto != null) {
            environmentService.setSecurityAccess(environment, securityAccessDto);
        }
    }

    private void editTelemetryIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (editDto.getTelemetry() != null) {
            ValidationResult validationResult = environmentFlowValidatorService.validateTelemetryLoggingStorageLocation(environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            environment.setTelemetry(editDto.getTelemetry());
        }
    }

    private void editIdBrokerMappingSource(EnvironmentEditDto editDto, Environment environment) {
        IdBrokerMappingSource idBrokerMappingSource = editDto.getIdBrokerMappingSource();
        if (idBrokerMappingSource != null) {
            ExperimentalFeatures experimentalFeaturesJson = environment.getExperimentalFeaturesJson();
            experimentalFeaturesJson.setIdBrokerMappingSource(idBrokerMappingSource);
            environment.setExperimentalFeaturesJson(experimentalFeaturesJson);
        }
    }

    private void editAdminGroupNameIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (editDto.getAdminGroupName() != null) {
            environmentService.setAdminGroupName(environment, editDto.getAdminGroupName());
        }
    }

    private void editEnvironmentParameters(EnvironmentEditDto editDto, Environment environment) {
        ParametersDto parametersDto = editDto.getParameters();
        if (parametersDto != null) {
            Optional<BaseParameters> original = parametersService.findByEnvironment(environment.getId());
            if (original.isPresent()) {
                BaseParameters originalParameters = original.get();
                parametersDto.setId(originalParameters.getId());
                if (originalParameters instanceof AwsParameters) {
                    validateAwsParameters(environment, parametersDto);
                }
            }
            BaseParameters parameters = parametersService.saveParameters(environment, parametersDto);
            if (parameters != null) {
                environment.setParameters(parameters);
            }
        }
    }

    private void validateAwsParameters(Environment environment, ParametersDto parametersDto) {
        if (parametersDto.getAwsParametersDto() != null) {
            EnvironmentDto environmentDto = environmentDtoConverter.environmentToDto(environment);
            ValidationResult validationResult = environmentFlowValidatorService
                    .validateAndDetermineAwsParameters(environmentDto, parametersDto.getAwsParametersDto());
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }
}
