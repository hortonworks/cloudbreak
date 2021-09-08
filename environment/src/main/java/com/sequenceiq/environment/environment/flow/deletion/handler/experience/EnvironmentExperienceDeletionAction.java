package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import static com.sequenceiq.cloudbreak.polling.PollingResult.EXIT;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;
import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_COUNT;
import static com.sequenceiq.environment.environment.flow.deletion.handler.experience.ExperienceDeletionRetrievalTask.EXPERIENCE_RETRYING_INTERVAL;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

@Component
public class EnvironmentExperienceDeletionAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentExperienceDeletionAction.class);

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
     * @param forceDelete is force deletion applied (i.e. we don't care if experiences could not be deleted)
     * @throws ExperienceOperationFailedException if some not successful result happens
     */
    public void execute(Environment environment, boolean forceDelete) {
        LOGGER.debug("Experience deletion executing for environment {} with force delete={}", environment.getName(), forceDelete);
        EnvironmentExperienceDto environmentExperienceDto = EnvironmentExperienceDto.fromEnvironment(environment);
        if (isDeleteCallWasSuccessful(environmentExperienceDto, forceDelete)) {
            waitForResult(environment, forceDelete);
        }
    }

    private boolean isDeleteCallWasSuccessful(EnvironmentExperienceDto environmentExperienceDto, boolean forceDelete) {
        try {
            experienceConnectorService.deleteConnectedExperiences(environmentExperienceDto);
            return true;
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Rethrow IllegalArgumentException during experienceConnectorService.deleteConnectedExperiences", e);
            throw e;
        } catch (IllegalStateException ise) {
            LOGGER.debug(IllegalStateException.class.getSimpleName() + " has occurred as the result of calling " +
                    "experienceConnectorService.deleteConnectedExperiences!", ise);
            if (!forceDelete) {
                throw new ExperienceOperationFailedException("Unable to delete connected experience(s)!", ise);
            }
            return false;
        } catch (RuntimeException e) {
            if (!forceDelete) {
                LOGGER.debug("Rethrow exception during experienceConnectorService.deleteConnectedExperiences", e);
                throw e;
            }
            LOGGER.debug("Forced environment delete causes skipping of experience deletion failure.", e);
            return false;
        }
    }

    private void waitForResult(Environment environment, boolean forceDelete) {
        Pair<PollingResult, Exception> result = experiencePollingService.pollWithTimeout(
                new ExperienceDeletionRetrievalTask(experienceConnectorService),
                new ExperiencePollerObject(environment.getResourceCrn(), environment.getName(), environment.getCloudPlatform(), environment.getAccountId()),
                EXPERIENCE_RETRYING_INTERVAL,
                EXPERIENCE_RETRYING_COUNT,
                SINGLE_FAILURE);
        if (!isSuccess(result.getLeft())) {
            if (result.getLeft().equals(EXIT)) {
                reCheckDeletionOtherwise(
                        environment,
                        env -> experienceConnectorService.getConnectedExperienceCount(EnvironmentExperienceDto.fromEnvironment(env)) == 0,
                        () -> processFailureWithForceDelete(result, forceDelete));
            } else {
                processFailureWithForceDelete(result, forceDelete);
            }
        }
    }

    private void processFailureWithForceDelete(Pair<PollingResult, Exception> result, boolean forceDelete) {
        if (forceDelete) {
            LOGGER.debug("Forced environment delete causes skipping of experience deletion failure.");
        } else {
            processFailure(result);
        }
    }

    private void reCheckDeletionOtherwise(Environment environment, Function<Environment, Boolean> reCheck, Runnable processFailure) {
        LOGGER.info("Re-checking experience deletion result for environment: {} [crn: {}]", environment.getName(), environment.getResourceCrn());
        if (!reCheck.apply(environment)) {
            processFailure.run();
        }
    }

    private void processFailure(Pair<PollingResult, Exception> result) {
        String rootMsg = "Failed to delete Experience!";
        if (result.getRight() == null) {
            LOGGER.debug("Experience deletion has failed but no exception has come from the polling result: {}", result.getLeft());
            throw new ExperienceOperationFailedException(rootMsg);
        }
        throw new ExperienceOperationFailedException(String.format("%s %s", rootMsg, experiencePollingFailureResolver.getMessageForFailure(result)),
                result.getRight());
    }

}
