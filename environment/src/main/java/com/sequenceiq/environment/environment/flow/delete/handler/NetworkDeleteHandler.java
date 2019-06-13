package com.sequenceiq.environment.environment.flow.delete.handler;

import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteHandlerSelectors.DELETE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class NetworkDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    private final NetworkService networkService;

    protected NetworkDeleteHandler(EventSender eventSender,
            EnvironmentService environmentService, NetworkService networkService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.networkService = networkService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findById(environmentDto.getId()).ifPresent(environment -> {
                environment.setStatus(EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS);
                environment.setNetwork(null);
                environmentService.save(environment);
            });

            environmentService.findById(environmentDto.getId()).ifPresent(environment -> deleteNetworkIfExists(environment.getId()));

            EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.EnvDeleteEventBuilder.anEnvDeleteEvent()
                    .withResourceId(environmentDto.getResourceId())
                    .withSelector(FINISH_ENV_DELETE_EVENT.selector())
                    .build();
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e);
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
