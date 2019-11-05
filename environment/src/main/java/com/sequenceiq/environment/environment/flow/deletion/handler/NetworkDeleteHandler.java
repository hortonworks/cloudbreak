package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_IDBROKER_MAPPINGS_DELETE_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class NetworkDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    private final NetworkService networkService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    protected NetworkDeleteHandler(EventSender eventSender,
            EnvironmentService environmentService, NetworkService networkService,
            EnvironmentNetworkService environmentNetworkService,
            EnvironmentDtoConverter environmentDtoConverter) {
        super(eventSender);
        this.environmentService = environmentService;
        this.networkService = networkService;
        this.environmentNetworkService = environmentNetworkService;
        this.environmentDtoConverter = environmentDtoConverter;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                if (environment.getNetwork() != null) {
                    RegistrationType registrationType = environment.getNetwork().getRegistrationType();
                    if (registrationType != null && registrationType.equals(RegistrationType.CREATE_NEW)) {
                        environmentNetworkService.deleteNetwork(environmentDtoConverter.environmentToDto(environment));
                    }
                    environment.setNetwork(null);
                    environmentService.save(environment);
                }
            });

            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> deleteNetworkIfExists(environment.getId()));

            EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                    .withResourceId(environmentDto.getResourceId())
                    .withResourceName(environmentDto.getName())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withSelector(START_IDBROKER_MAPPINGS_DELETE_EVENT.selector())
                    .build();
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    @Override
    public String selector() {
        return DELETE_NETWORK_EVENT.selector();
    }

    private void deleteNetworkIfExists(Long environmentId) {
        networkService.findByEnvironment(environmentId).ifPresent(networkService::delete);
    }

}
