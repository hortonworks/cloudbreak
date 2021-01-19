package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_EXPERIENCE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ExperienceDeletionHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final int SINGLE_FAILURE = 1;

    private final EntitlementService entitlementService;

    private final PollingService<ExperiencePollerObject> experiencePollingService;

    private final EnvironmentService environmentService;

    private final ExperienceConnectorService experienceConnectorService;

    protected ExperienceDeletionHandler(EventSender eventSender, EntitlementService entitlementService, PollingService<ExperiencePollerObject> experiencePollingService,
            EnvironmentService environmentService, ExperienceConnectorService experienceConnectorService) {
        super(eventSender);
        this.entitlementService = entitlementService;
        this.experiencePollingService = experiencePollingService;
        this.environmentService = environmentService;
        this.experienceConnectorService = experienceConnectorService;
    }

    @Override
    public String selector() {
        return DELETE_EXPERIENCE_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDeletionDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDeletionDtoEvent.getData();
        EnvironmentDto envDto = environmentDeletionDtoEvent.getData().getEnvironmentDto();

        try {
            if (entitlementService.isExperienceDeletionEnabled(envDto.getAccountId())) {
                environmentService.findEnvironmentById(envDto.getId())
                        .ifPresent(this::deleteExperiences);
            }
            EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDeletionDto);
            eventSender().sendEvent(envDeleteEvent, environmentDeletionDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = EnvDeleteFailedEvent.builder()
                    .withEnvironmentID(envDto.getId())
                    .withException(e)
                    .withResourceCrn(envDto.getResourceCrn())
                    .withResourceName(envDto.getName())
                    .build();
            eventSender().sendEvent(failedEvent, environmentDeletionDtoEvent.getHeaders());
        }
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_FREEIPA_DELETE_EVENT.selector())
                .build();
    }

    private void deleteExperiences(Environment environment) {
        EnvironmentExperienceDto environmentExperienceDto = new EnvironmentExperienceDto.Builder()
                .withName(environment.getName())
                .withCrn(environment.getResourceCrn())
                .withAccountId(environment.getAccountId())
                .build();
        experienceConnectorService.deleteConnectedExperiences(environmentExperienceDto);
        Pair<PollingResult, Exception> result = experiencePollingService.pollWithTimeout(
                new ExperienceDeletionRetrievalTask(experienceConnectorService),
                new ExperiencePollerObject(environment.getResourceCrn(), environment.getName(), environment.getAccountId()),
                ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL,
                ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT,
                SINGLE_FAILURE);
        if (!isSuccess(result.getLeft())) {
            throw new ExperienceOperationFailedException("Failed to delete Experience! " + getIfNotNull(result.getRight(), Throwable::getMessage));
        }
    }

}
