package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_CLUSTER_DEFINITION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_UMS_RESOURCE_DELETE_EVENT;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceDeletionService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
// TODO: CB-11559
public class ClusterDefinitionDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionDeleteHandler.class);

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final EnvironmentService environmentService;

    protected ClusterDefinitionDeleteHandler(EventSender eventSender, EnvironmentResourceDeletionService environmentResourceDeletionService,
            EnvironmentService environmentService) {
        super(eventSender);
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.environmentService = environmentService;
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        AtomicReference<String> resourceCrn = new AtomicReference<>(null);

        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_UMS_RESOURCE_DELETE_EVENT.selector())
                .build();

        try {
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> {
                        resourceCrn.set(environment.getResourceCrn());
                        environmentResourceDeletionService.deleteClusterDefinitionsOnCloudbreak(environment.getResourceCrn());
                    });
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) { // TODO: CB-11556
            if (environmentDeletionDto.isForceDelete()) {
                LOGGER.warn("The {} was not successful but the environment deletion was requested as force delete so " +
                        "continue the deletion flow", selector());
                eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
            } else {
                EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                        .withEnvironmentID(environmentDto.getId())
                        .withException(e)
                        .withResourceCrn(environmentDto.getResourceCrn())
                        .withResourceName(environmentDto.getName())
                        .build();
                eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
            }
        }
    }

    @Override
    public String selector() {
        return DELETE_CLUSTER_DEFINITION_EVENT.selector();
    }

}
