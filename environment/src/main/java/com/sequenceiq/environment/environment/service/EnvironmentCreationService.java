package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@Service
public class EnvironmentCreationService {

    private final EnvironmentService environmentService;

    private final EnvironmentValidatorService validatorService;

    private final ProxyConfigService proxyConfigService;

    private final EnvironmentResourceService environmentResourceService;

    private final EventSender eventSender;

    private final EnvironmentDtoConverter environmentDtoConverter;

    public EnvironmentCreationService(EnvironmentService environmentService, EnvironmentValidatorService validatorService,
            ProxyConfigService proxyConfigService, EnvironmentResourceService environmentResourceService,
            EventSender eventSender, EnvironmentDtoConverter environmentDtoConverter) {
        this.environmentService = environmentService;
        this.validatorService = validatorService;
        this.proxyConfigService = proxyConfigService;
        this.environmentResourceService = environmentResourceService;
        this.eventSender = eventSender;
        this.environmentDtoConverter = environmentDtoConverter;
    }

    // TODO: trigger creation properly
    public void triggerCreationFlow() {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.EnvCreationEventBuilder.anEnvCreationEvent()
                .withSelector(START_NETWORK_CREATION_EVENT.selector())
                .withResourceId(1L)
                .withResourceName("hello")
                .build();
        eventSender.sendEvent(envCreationEvent);
    }

    public EnvironmentDto create(EnvironmentCreationDto creationDto) {
        if (environmentService.isNameOccupied(creationDto.getName(), creationDto.getAccountId())) {
            throw new BadRequestException(String.format("Environment with name '%s' already exists in account '%s'.",
                    creationDto.getName(), creationDto.getAccountId()));
        }
        Environment environment = initializeEnvironment(creationDto);
        CloudRegions cloudRegions = setLocationAndRegions(creationDto, environment);
        validateCreation(creationDto, environment, cloudRegions);
        environment = environmentService.save(environment);
        environmentResourceService.createAndSetNetwork(environment, creationDto.getNetwork());
        return environmentDtoConverter.environmentToDto(environment);
    }

    private Environment initializeEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = environmentDtoConverter.creationDtoToEnvironment(creationDto);
        environment.setResourceCrn(createCrn(creationDto.getAccountId()));
        Credential credential = environmentResourceService
                .getCredentialFromRequest(creationDto.getCredential(), creationDto.getAccountId());
        environment.setCredential(credential);
        environment.setCloudPlatform(credential.getCloudPlatform());
        return environment;
    }

    private CloudRegions setLocationAndRegions(EnvironmentCreationDto creationDto, Environment environment) {
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        environmentService.setLocation(environment, creationDto.getLocation(), cloudRegions);
        if (cloudRegions.areRegionsSupported()) {
            environmentService.setRegions(environment, creationDto.getRegions(), cloudRegions);
        }
        return cloudRegions;
    }

    private void validateCreation(EnvironmentCreationDto creationDto, Environment environment, CloudRegions cloudRegions) {
        ValidationResult validationResult = validatorService.validateCreation(environment, creationDto, cloudRegions);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    public String createCrn(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}
