package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.proxy.ProxyConfigService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@Service
public class EnvironmentCreationService {

    private final EnvironmentService environmentService;

    private final EnvironmentValidatorService validatorService;

    private final ProxyConfigService proxyConfigService;

    private final EnvironmentCredentialOperationService environmentCredentialOperationService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final EventSender eventSender;

    private final EnvironmentDtoConverter environmentDtoConverter;

    public EnvironmentCreationService(EnvironmentService environmentService, EnvironmentValidatorService validatorService,
            ProxyConfigService proxyConfigService, EnvironmentCredentialOperationService environmentCredentialOperationService,
            EnvironmentNetworkService environmentNetworkService, EventSender eventSender, EnvironmentDtoConverter environmentDtoConverter) {
        this.environmentService = environmentService;
        this.validatorService = validatorService;
        this.proxyConfigService = proxyConfigService;
        this.environmentCredentialOperationService = environmentCredentialOperationService;
        this.environmentNetworkService = environmentNetworkService;
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
        Environment environment = initEnvironment(creationDto);
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        environmentService.setLocation(environment, creationDto.getLocation(), cloudRegions);
        ValidationResult validationResult;
        if (cloudRegions.areRegionsSupported()) {
            environmentService.setRegions(environment, creationDto.getRegions(), cloudRegions);
        }
        validationResult = validatorService.validateCreation(environment, creationDto, cloudRegions);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        MDCBuilder.buildMdcContext(environment);
        environment = environmentService.save(environment);
        createAndSetNetwork(environment, creationDto.getNetwork());
        return environmentDtoConverter.environmentToDto(environment);
    }

    private Environment initEnvironment(EnvironmentCreationDto creationDto) {
        Environment environment = environmentDtoConverter.creationDtoToEnvironment(creationDto);
        environment.setProxyConfigs(proxyConfigService.findByNamesInAccount(creationDto.getProxyNames(), creationDto.getAccountId()));
        Credential credential = environmentCredentialOperationService
                .getCredentialFromRequest(creationDto.getCredential(), creationDto.getAccountId());
        environment.setCredential(credential);
        return environment;
    }

    private void createAndSetNetwork(Environment environment, NetworkDto networkDto) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        BaseNetwork network = environmentNetworkService.createNetworkIfPossible(environment, networkDto, cloudPlatform);
        if (network != null) {
            environment.setNetwork(network);
        }
    }

}
