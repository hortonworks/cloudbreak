package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_IDBROKER_MAPPINGS_DELETE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class NetworkDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final EnvironmentNetworkService environmentNetworkService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final TransactionService transactionService;

    private HandlerExceptionProcessor exceptionProcessor;

    protected NetworkDeleteHandler(EventSender eventSender,
            EnvironmentService environmentService,
            EnvironmentNetworkService environmentNetworkService,
            EnvironmentDtoConverter environmentDtoConverter,
            TransactionService transactionService,
            HandlerExceptionProcessor exceptionProcessor) {
        super(eventSender);
        this.environmentService = environmentService;
        this.environmentNetworkService = environmentNetworkService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.exceptionProcessor = exceptionProcessor;
        this.transactionService = transactionService;
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
            deleteNetworkResource(environmentDto);
            archiveNetworkEntity(environmentDto);
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    private void archiveNetworkEntity(EnvironmentDto environmentDto) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(env -> {
            if (env.getNetwork() != null) {
                env.getNetwork().setName(env.getResourceCrn() + "_network_DELETED_@_" + System.currentTimeMillis());
                env.getNetwork().setArchived(true);
                env.getNetwork().setDeletionTimestamp(System.currentTimeMillis());
                environmentService.save(env);
            }
        }));
    }

    private void deleteNetworkResource(EnvironmentDto environmentDto) {
        environmentService.findEnvironmentById(environmentDto.getId())
                .map(Environment::getNetwork)
                .filter(network -> network.getRegistrationType() == RegistrationType.CREATE_NEW)
                .ifPresent(network -> environmentNetworkService.deleteNetwork(environmentDto));
    }

    @Override
    public String selector() {
        return DELETE_NETWORK_EVENT.selector();
    }

}
