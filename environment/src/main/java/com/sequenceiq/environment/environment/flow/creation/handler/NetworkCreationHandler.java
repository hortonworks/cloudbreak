package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.YARN;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_PUBLICKEY_CREATION_EVENT;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class NetworkCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkCreationHandler.class);

    private final EnvironmentService environmentService;

    private final NetworkService networkService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final EnvironmentResourceService environmentResourceService;

    private final CloudNetworkService cloudNetworkService;

    private final Set<String> enabledPlatforms;

    private final EventBus eventBus;

    protected NetworkCreationHandler(EventSender eventSender,
            EnvironmentService environmentService,
            NetworkService networkService,
            EnvironmentNetworkService environmentNetworkService,
            CloudNetworkService cloudNetworkService,
            EnvironmentResourceService environmentResourceService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms, EventBus eventBus) {
        super(eventSender);
        this.environmentService = environmentService;
        this.networkService = networkService;
        this.environmentNetworkService = environmentNetworkService;
        this.enabledPlatforms = enabledPlatforms;
        this.cloudNetworkService = cloudNetworkService;
        this.environmentResourceService = environmentResourceService;
        this.eventBus = eventBus;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> {
                        setChildEnvironmentNetworkIfItHasParentWithTheSameCloudProvider(environmentDto);
                        if (environmentDto.getNetwork() != null) {
                            LOGGER.debug("Environment ({}) dto has network, hence we're filling it's related subnet fields", environment.getName());
                            environmentDto.getNetwork().setSubnetMetas(cloudNetworkService.retrieveSubnetMetadata(environmentDto,
                                    environmentDto.getNetwork()));
                            environmentResourceService.createAndSetNetwork(environment, environmentDto.getNetwork(), environment.getAccountId(),
                                    environmentDto.getNetwork().getSubnetMetas());
                        } else {
                            LOGGER.debug("Environment ({}) dto has no network!", environment.getName());
                        }
                        createCloudNetworkIfNeeded(environmentDto, environment);
                        environmentService.save(environment);
                    });
            initiateNextStep(environmentDtoEvent, environmentDto);
        } catch (Exception e) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                    environmentDto.getId(),
                    environmentDto.getName(),
                    e,
                    environmentDto.getResourceCrn());
            eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
        }
    }

    private void setChildEnvironmentNetworkIfItHasParentWithTheSameCloudProvider(EnvironmentDto currentEnvDto) {
        String parentEnvName = currentEnvDto.getParentEnvironmentName();
        if (StringUtils.isNotEmpty(parentEnvName) && currentEnvDto.getCloudPlatform() != null &&
                currentEnvDto.getCloudPlatform().equals(currentEnvDto.getParentEnvironmentCloudPlatform())) {
            LOGGER.debug("Parent environment (with name: {} ) has detected, going to fetch it for it's network for the child.", parentEnvName);
            EnvironmentDto parentEnvDto = environmentService.getByNameAndAccountId(parentEnvName, currentEnvDto.getAccountId());
            NetworkDto parentNetworkDto = parentEnvDto.getNetwork();
            parentNetworkDto.setId(currentEnvDto.getNetwork().getId());
            currentEnvDto.setNetwork(parentNetworkDto);
        }
    }

    private void createCloudNetworkIfNeeded(EnvironmentDto environmentDto, Environment environment) {
        if (hasNetwork(environment) && environment.getNetwork().getRegistrationType() == RegistrationType.CREATE_NEW) {
            BaseNetwork baseNetwork = environmentNetworkService.createCloudNetwork(environmentDto, environment.getNetwork());
            baseNetwork = networkService.save(baseNetwork);
            environment.setNetwork(baseNetwork);
        }
    }

    private boolean hasNetwork(Environment environment) {
        return Objects.nonNull(environment.getNetwork())
                && !YARN.equalsIgnoreCase(environment.getCloudPlatform())
                && !MOCK.equalsIgnoreCase(environment.getCloudPlatform())
                && enabledPlatforms.contains(environment.getCloudPlatform());
    }

    private void initiateNextStep(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto) {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_PUBLICKEY_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return CREATE_NETWORK_EVENT.selector();
    }
}
