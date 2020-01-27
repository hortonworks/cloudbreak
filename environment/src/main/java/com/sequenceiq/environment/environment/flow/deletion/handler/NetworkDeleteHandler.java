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
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class NetworkDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentService environmentService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    protected NetworkDeleteHandler(EventSender eventSender,
            EnvironmentService environmentService,
            EnvironmentNetworkService environmentNetworkService,
            EnvironmentDtoConverter environmentDtoConverter) {
        super(eventSender);
        this.environmentService = environmentService;
        this.environmentNetworkService = environmentNetworkService;
        this.environmentDtoConverter = environmentDtoConverter;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                BaseNetwork network = environment.getNetwork();
                if (network != null) {
                    RegistrationType registrationType = network.getRegistrationType();
                    if (RegistrationType.CREATE_NEW == registrationType) {
                        environmentNetworkService.deleteNetwork(environmentDtoConverter.environmentToDto(environment));
                    }
                    network.setName(network.getName() + "_DELETED_@_" + System.currentTimeMillis());
                    environmentService.save(environment);
                }
            });
            EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                    .withResourceId(environmentDto.getResourceId())
                    .withResourceName(environmentDto.getName())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withSelector(START_IDBROKER_MAPPINGS_DELETE_EVENT.selector())
                    .build();
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                    .withEnvironmentID(environmentDto.getId())
                    .withException(e)
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    @Override
    public String selector() {
        return DELETE_NETWORK_EVENT.selector();
    }

}
