package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.YARN;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_PUBLICKEY_CREATION_EVENT;

import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class NetworkCreationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    private final NetworkService networkService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final Set<String> enabledPlatforms;

    protected NetworkCreationHandler(EventSender eventSender,
            EnvironmentService environmentService,
            NetworkService networkService,
            EnvironmentNetworkService environmentNetworkService,
            @Value("${environment.enabledplatforms}") Set<String> enabledPlatforms) {
        super(eventSender);
        this.environmentService = environmentService;
        this.networkService = networkService;
        this.environmentNetworkService = environmentNetworkService;
        this.enabledPlatforms = enabledPlatforms;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            createNetwork(environmentDto);
            initiateNextStep(environmentDtoEvent, environmentDto);
        } catch (Exception e) {
            EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                    environmentDto.getId(),
                    environmentDto.getName(),
                    e,
                    environmentDto.getResourceCrn());
            eventSender().sendEvent(failureEvent, environmentDtoEvent.getHeaders());
        }
    }

    private void createNetwork(EnvironmentDto environmentDto) {
        environmentService.findEnvironmentById(environmentDto.getId())
                .ifPresent(environment -> {
                    createCloudNetworkIfNeeded(environmentDto, environment);
                    environmentService.save(environment);
                });
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
