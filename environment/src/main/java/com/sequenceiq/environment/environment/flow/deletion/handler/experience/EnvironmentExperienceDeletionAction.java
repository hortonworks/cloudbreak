package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT;
import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

@Component
public class EnvironmentExperienceDeletionAction {

    private static final int SINGLE_FAILURE = 1;

    private final ExperienceConnectorService experienceConnectorService;

    private final PollingService<ExperiencePollerObject> experiencePollingService;

    private final ExperiencePollingFailureResolver experiencePollingFailureResolver;

    public EnvironmentExperienceDeletionAction(ExperienceConnectorService experienceConnectorService,
            PollingService<ExperiencePollerObject> experiencePollingService,
            ExperiencePollingFailureResolver experiencePollingFailureResolver) {
        this.experienceConnectorService = experienceConnectorService;
        this.experiencePollingService = experiencePollingService;
        this.experiencePollingFailureResolver = experiencePollingFailureResolver;
    }

    /**
     * Creates and executes a poller based deletion operation over the given environment's experiences.
     *
     * @param environment the environment that may have some experience connected to it.
     * @throws ExperienceOperationFailedException if some not successful result happens
     */
    public void execute(Environment environment) {
        EnvironmentExperienceDto environmentExperienceDto = new EnvironmentExperienceDto.Builder()
                .withName(environment.getName())
                .withCrn(environment.getResourceCrn())
                .withAccountId(environment.getAccountId())
                .build();
        experienceConnectorService.deleteConnectedExperiences(environmentExperienceDto);
        Pair<PollingResult, Exception> result = experiencePollingService.pollWithTimeout(
                new ExperienceDeletionRetrievalTask(experienceConnectorService),
                new ExperiencePollerObject(environment.getResourceCrn(), environment.getName(), environment.getAccountId()),
                EXPERIENCE_RETRYING_INTERVAL,
                EXPERIENCE_RETRYING_COUNT,
                SINGLE_FAILURE);
        if (!isSuccess(result.getLeft())) {
            if (result.getRight() == null) {
                throw new ExperienceOperationFailedException("Failed to delete Experience!");
            }
            throw new ExperienceOperationFailedException("Failed to delete Experience! " + experiencePollingFailureResolver.getMessageForFailure(result));
        }
    }

}
