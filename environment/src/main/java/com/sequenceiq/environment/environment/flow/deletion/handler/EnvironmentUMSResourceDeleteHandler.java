package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_UMS_RESOURCE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class EnvironmentUMSResourceDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentUMSResourceDeleteHandler.class);

    private final OwnerAssignmentService ownerAssignmentService;

    private final EnvironmentService environmentService;

    private final VirtualGroupService virtualGroupService;

    protected EnvironmentUMSResourceDeleteHandler(EventSender eventSender,
            EnvironmentService environmentService, OwnerAssignmentService ownerAssignmentService, VirtualGroupService virtualGroupService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.ownerAssignmentService = ownerAssignmentService;
        this.virtualGroupService = virtualGroupService;
    }

    @Override
    public String selector() {
        return DELETE_UMS_RESOURCE_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        String environmentCrn = null;
        try {
            environmentCrn = environmentDto.getResourceCrn();
            AtomicReference<String> resourceCrn = new AtomicReference<>(null);
            environmentService.findEnvironmentById(environmentDto.getId())
                    .ifPresent(environment -> {
                        resourceCrn.set(environment.getResourceCrn());
                    });
            if (StringUtils.isBlank(environmentCrn)) {
                environmentCrn = resourceCrn.get();
            }
            virtualGroupService.cleanupVirtualGroups(Crn.fromString(environmentCrn).getAccountId(), environmentCrn);
            ownerAssignmentService.notifyResourceDeleted(environmentCrn);
        } catch (Exception e) {
            LOGGER.warn("UMS delete event failed (this event is not critical)", e);
        }
        EnvDeleteEvent envDeleteEvent = EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentCrn)
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(FINISH_ENV_DELETE_EVENT.selector())
                .build();
        eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
    }
}
