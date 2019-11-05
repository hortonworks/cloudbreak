package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_CLUSTER_DEFINITION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_UMS_RESOURCE_DELETE_EVENT;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceDeletionService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ClusterDefinitionDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final EnvironmentService environmentService;

    protected ClusterDefinitionDeleteHandler(EventSender eventSender, EnvironmentResourceDeletionService environmentResourceDeletionService,
            EnvironmentService environmentService) {
        super(eventSender);
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.environmentService = environmentService;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        AtomicReference<String> resourceCrn = new AtomicReference<>(null);
        try {
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> {
                        resourceCrn.set(environment.getResourceCrn());
                        environmentResourceDeletionService.deleteClusterDefinitionsOnCloudbreak(environment.getResourceCrn());
                    });
            EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.EnvDeleteEventBuilder.anEnvDeleteEvent()
                    .withResourceId(environmentDto.getResourceId())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceName(environmentDto.getName())
                    .withSelector(START_UMS_RESOURCE_DELETE_EVENT.selector())
                    .build();
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    @Override
    public String selector() {
        return DELETE_CLUSTER_DEFINITION_EVENT.selector();
    }

}
