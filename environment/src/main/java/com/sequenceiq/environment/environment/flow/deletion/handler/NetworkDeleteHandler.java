package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_IDBROKER_MAPPINGS_DELETE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class NetworkDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private HandlerExceptionProcessor exceptionProcessor;

    protected NetworkDeleteHandler(EventSender eventSender,
            EnvironmentService environmentService,
            EnvironmentNetworkService environmentNetworkService,
            EnvironmentDtoConverter environmentDtoConverter,
            HandlerExceptionProcessor exceptionProcessor) {
        super(eventSender);
        this.environmentService = environmentService;
        this.environmentNetworkService = environmentNetworkService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.exceptionProcessor = exceptionProcessor;
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_IDBROKER_MAPPINGS_DELETE_EVENT.selector())
                .build();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                BaseNetwork network = environment.getNetwork();
                if (network != null) {
                    RegistrationType registrationType = network.getRegistrationType();
                    if (RegistrationType.CREATE_NEW == registrationType) {
                        environmentNetworkService.deleteNetwork(environmentDtoConverter.environmentToDto(environment));
                    }
                    network.setName(environment.getResourceCrn() + "_network_DELETED_@_" + System.currentTimeMillis());
                    environmentService.save(environment);
                }
            });
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    @Override
    public String selector() {
        return DELETE_NETWORK_EVENT.selector();
    }

}
