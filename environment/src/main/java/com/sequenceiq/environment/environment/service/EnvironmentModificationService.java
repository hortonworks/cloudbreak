package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Optional;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
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

    private final EnvironmentResourceService environmentResourceService;

    public EnvironmentModificationService(EnvironmentDtoConverter environmentDtoConverter, EnvironmentService environmentService,
            CredentialService credentialService, NetworkService networkService, AuthenticationDtoConverter authenticationDtoConverter,
            ParametersService parametersService, EnvironmentFlowValidatorService environmentFlowValidatorService,
            EnvironmentResourceService environmentResourceService) {
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentService = environmentService;
        this.credentialService = credentialService;
        this.networkService = networkService;
        this.authenticationDtoConverter = authenticationDtoConverter;
        this.parametersService = parametersService;
        this.environmentFlowValidatorService = environmentFlowValidatorService;
        this.environmentResourceService = environmentResourceService;
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

    public EnvironmentDto changeTelemetryFeaturesByEnvironmentName(String accountId, String environmentName,
            EnvironmentFeatures features) {
        Environment environment = environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(environmentName, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with name '%s'", environmentName)));
        return changeTelemetryFeatures(features, environment);
    }

    public EnvironmentDto changeTelemetryFeaturesByEnvironmentCrn(String accountId, String crn,
            EnvironmentFeatures features) {
        Environment environment = environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(crn, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("No environment found with CRN '%s'", crn)));
        return changeTelemetryFeatures(features, environment);
    }

    private EnvironmentDto edit(EnvironmentEditDto editDto, Environment env) {
        editDescriptionIfChanged(env, editDto);
        editTelemetryIfChanged(env, editDto);
        editNetworkIfChanged(env, editDto);
        editAdminGroupNameIfChanged(env, editDto);
        editAuthenticationIfChanged(editDto, env);
        editSecurityAccessIfChanged(editDto, env);
        editIdBrokerMappingSource(editDto, env);
        editCloudStorageValidation(editDto, env);
        editTunnelIfChanged(editDto, env);
        editEnvironmentParameters(editDto, env);
        Environment saved = environmentService.save(env);
        return environmentDtoConverter.environmentToDto(saved);
    }

    private EnvironmentDto changeCredential(String accountId, String environmentName, EnvironmentChangeCredentialDto dto, Environment environment) {
        //CHECKSTYLE:OFF
        // TODO: 2019. 06. 03. also we have to check for SDXs and DistroXs what uses the given credential. If there is at least one, we have to update the crn reference through the other services
        //CHECKSTYLE:ON
        Credential credential = credentialService.getByNameForAccountId(dto.getCredentialName(), accountId, ENVIRONMENT);
        environment.setCredential(credential);
        LOGGER.debug("About to change credential on environment \"{}\"", environmentName);
        Environment saved = environmentService.save(environment);
        return environmentDtoConverter.environmentToDto(saved);
    }

    private EnvironmentDto changeTelemetryFeatures(EnvironmentFeatures features, Environment environment) {
        EnvironmentTelemetry telemetry = environment.getTelemetry();
        if (telemetry != null) {
            EnvironmentFeatures actualFeatures = telemetry.getFeatures();
            if (actualFeatures != null) {
                if (features.getClusterLogsCollection() != null) {
                    actualFeatures.setClusterLogsCollection(features.getClusterLogsCollection());
                    LOGGER.debug("Updating cluster log collection (environment telemetry feature): {}.",
                            features.getClusterLogsCollection().isEnabled());
                }
                if (features.getWorkloadAnalytics() != null) {
                    actualFeatures.setWorkloadAnalytics(features.getWorkloadAnalytics());
                    LOGGER.debug("Updating workload analytics (environment telemetry feature): {}.",
                            features.getWorkloadAnalytics().isEnabled());
                }
                if (features.getCloudStorageLogging() != null) {
                    actualFeatures.setCloudStorageLogging(features.getCloudStorageLogging());
                    LOGGER.debug("Updating cloud storage logging (environment telemetry feature): {}.",
                            features.getCloudStorageLogging().isEnabled());
                }
                telemetry.setFeatures(actualFeatures);
                // required to re-set as telemetry is saved as a JSON string
                environment.setTelemetry(telemetry);
            } else if (features != null) {
                telemetry.setFeatures(features);
                // required to re-set as telemetry is saved as a JSON string
                environment.setTelemetry(telemetry);
                LOGGER.debug("Adding new feature object for environment level telemetry.");
            } else {
                LOGGER.debug("No telemetry feature related changes has been made.");
            }
        }
        Environment saved = environmentService.save(environment);
        return environmentDtoConverter.environmentToDto(saved);
    }

    private void editNetworkIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (networkChanged(editDto)) {
            BaseNetwork network = networkService.validate(environment.getNetwork(), editDto, environment);
            network = networkService.refreshMetadataFromCloudProvider(network, editDto, environment);
            if (network != null) {
                environment.setNetwork(network);
            }
        }
    }

    private boolean networkChanged(EnvironmentEditDto editDto) {
        return editDto.getNetworkDto() != null;
    }

    private void editDescriptionIfChanged(Environment environment, EnvironmentEditDto editDto) {
        if (StringUtils.isNotEmpty(editDto.getDescription())) {
            environment.setDescription(editDto.getDescription());
        }
    }

    @VisibleForTesting
    void editAuthenticationIfChanged(EnvironmentEditDto editDto, Environment environment) {
        AuthenticationDto authenticationDto = editDto.getAuthentication();
        if (authenticationDto != null) {
            EnvironmentValidatorService validatorService = environmentService.getValidatorService();
            ValidationResult validationResult = validatorService.validateAuthenticationModification(editDto, environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            EnvironmentAuthentication originalAuthentication = environment.getAuthentication();
            environment.setAuthentication(authenticationDtoConverter.dtoToAuthentication(authenticationDto));
            boolean cleanupOldSshKey = true;
            if (StringUtils.isNotEmpty(authenticationDto.getPublicKey())) {
                cleanupOldSshKey = environmentResourceService.createAndUpdateSshKey(environment);
            }
            if (cleanupOldSshKey) {
                String oldSshKeyId = originalAuthentication.getPublicKeyId();
                LOGGER.info("The '{}' of ssh key is replaced with {}", oldSshKeyId, environment.getAuthentication().getPublicKeyId());
                if (originalAuthentication.isManagedKey()) {
                    environmentResourceService.deletePublicKey(environment, oldSshKeyId);
                }
            } else {
                LOGGER.info("Authentication modification was unsuccessful. The authentication was reverted to the previous version.");
                environment.setAuthentication(originalAuthentication);
            }
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
            EnvironmentValidatorService validatorService = environmentService.getValidatorService();
            ValidationResult validationResult = validatorService.validateSecurityAccessModification(securityAccessDto, environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            validationResult = validatorService.validateSecurityGroups(editDto, environment);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
            environmentService.editSecurityAccess(environment, securityAccessDto);
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

    private void editCloudStorageValidation(EnvironmentEditDto editDto, Environment environment) {
        CloudStorageValidation cloudStorageValidation = editDto.getCloudStorageValidation();
        if (cloudStorageValidation != null) {
            ExperimentalFeatures experimentalFeaturesJson = environment.getExperimentalFeaturesJson();
            experimentalFeaturesJson.setCloudStorageValidation(cloudStorageValidation);
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
                    AwsParameters awsOriginalParameters = (AwsParameters) originalParameters;
                    parametersDto.getAwsParametersDto().setFreeIpaSpotPercentage(awsOriginalParameters.getFreeIpaSpotPercentage());
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
                    .validateParameters(environmentDto, parametersDto);
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }
}
