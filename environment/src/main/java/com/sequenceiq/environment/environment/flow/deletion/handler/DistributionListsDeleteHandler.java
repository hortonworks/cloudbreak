package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DISTRIBUTION_LIST;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.notification.sender.DistributionListManagementService;

@Component
public class DistributionListsDeleteHandler extends ExceptionCatcherEventHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionListsDeleteHandler.class);

    private final DistributionListManagementService distributionListManagementService;

    public DistributionListsDeleteHandler(DistributionListManagementService distributionListManagementService) {
        this.distributionListManagementService = distributionListManagementService;
    }

    @Override
    public String selector() {
        return DELETE_DISTRIBUTION_LIST.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentDeletionDto> event) {
        EnvironmentDto environmentDto = event.getData().getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .withForceDelete(event.getData().isForceDelete())
                .withSelector(FINISH_ENV_DELETE_EVENT.selector())
                .build();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentDeletionDto> event) {
        LOGGER.debug("Accepting delete distribution list event");
        EnvironmentDeletionDto environmentDeletionDto = event.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        try {
            distributionListManagementService.deleteDistributionList(environmentDto.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Deleting distribution list failed, we proceed with logging it", e);
        }
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(FINISH_ENV_DELETE_EVENT.selector())
                .build();
    }
}
