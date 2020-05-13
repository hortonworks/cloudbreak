package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_EXPERIENCE_EVENT;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.experience.ExperienceConnectorService;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ExperienceDeletionHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final int SINGLE_FAILURE = 1;

    private final PollingService<ExperiencePollerObject> experiencePollingService;

    private final ExperienceConnectorService experienceConnectorService;

    protected ExperienceDeletionHandler(EventSender eventSender, PollingService<ExperiencePollerObject> experiencePollingService,
            ExperienceConnectorService experienceConnectorService) {
        super(eventSender);
        this.experiencePollingService = experiencePollingService;
        this.experienceConnectorService = experienceConnectorService;
    }

    @Override
    public String selector() {
        return DELETE_EXPERIENCE_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> env) {
        EnvironmentDto envDto = env.getData().getEnvironmentDto();
        Pair<PollingResult, Exception> result = experiencePollingService.pollWithTimeout(
                new ExperienceDeletionRetrievalTask(experienceConnectorService),
                new ExperiencePollerObject(envDto.getResourceCrn(), envDto.getName(), envDto.getAccountId()),
                ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL,
                ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT,
                SINGLE_FAILURE);
        if (!isSuccess(result.getLeft())) {
            throw new ExperienceOperationFailedException("Failed to delete Experience! " + getIfNotNull(result.getRight(), Throwable::getMessage));
        }
    }

}
