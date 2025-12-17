package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_DISTRIBUTION_LIST_CREATION_FAILED_WITH_REASON;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_DISTRIBUTION_LISTS_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.DistributionListManagementService;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;

@Component
public class DistributionListCreationHandler extends ExceptionCatcherEventHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionListCreationHandler.class);

    private final ParametersService parametersService;

    private final DistributionListManagementService distributionListManagementService;

    private final EventSenderService eventSenderService;

    public DistributionListCreationHandler(ParametersService parametersService,
            DistributionListManagementService distributionListManagementService,
            EventSenderService eventSenderService) {
        this.parametersService = parametersService;
        this.distributionListManagementService = distributionListManagementService;
        this.eventSenderService = eventSenderService;
    }

    @Override
    public String selector() {
        return CREATE_DISTRIBUTION_LISTS_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentDto> event) {
        return getEnvCreateEvent(event.getData());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentDto> event) {
        LOGGER.debug("Accepting CreateDistributionList event");
        EnvironmentDto environmentDto = event.getData();
        try {
            Long environmentId = environmentDto.getId();
            List<EventChannelPreference> preferences = NotificationType.getEventTypeIds().stream()
                    .map(id -> new EventChannelPreference(id, Set.of(ChannelType.EMAIL), Set.of(NotificationSeverity.WARNING)))
                    .toList();
            CreateDistributionListRequest request = new CreateDistributionListRequest(
                    environmentDto.getResourceCrn(),
                    environmentDto.getName(),
                    preferences);
            Optional<DistributionList> distributionList = distributionListManagementService.createOrUpdateList(request);
            distributionList.ifPresent(list -> parametersService.updateDistributionListDetails(environmentId, list));
            return getEnvCreateEvent(environmentDto);
        } catch (Exception e) {
            LOGGER.error("Creating distribution list failed, we proceed with logging it", e);
            eventSenderService.sendEventAndNotification(environmentDto, ThreadBasedUserCrnProvider.getUserCrn(),
                    ENVIRONMENT_DISTRIBUTION_LIST_CREATION_FAILED_WITH_REASON, Set.of(e.getMessage()));
            return getEnvCreateEvent(environmentDto);
        }
    }

    private EnvCreationEvent getEnvCreateEvent(EnvironmentDto environmentDto) {
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(FINISH_ENV_CREATION_EVENT.selector())
                .build();
    }
}
