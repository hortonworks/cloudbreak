package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

import io.micrometer.core.instrument.util.StringUtils;

public class ExperienceDeletionRetrievalTask extends SimpleStatusCheckerTask<ExperiencePollerObject> {

    public static final int EXPERIENCE_RETRYING_INTERVAL = 5000;

    public static final int EXPERIENCE_RETRYING_COUNT = 900;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceDeletionRetrievalTask.class);

    private static final String DELETE_FAILED_STATUS = "DELETE_FAILED";

    private final ExperienceConnectorService experienceConnectorService;

    public ExperienceDeletionRetrievalTask(ExperienceConnectorService experienceConnectorService) {
        this.experienceConnectorService = experienceConnectorService;
    }

    @Override
    public boolean checkStatus(ExperiencePollerObject pollerObject) {
        EnvironmentExperienceDto dto = buildDto(pollerObject);
        Set<ExperienceCluster> connectedExperiences = experienceConnectorService.getConnectedExperiences(dto);
        if (connectedExperiences.isEmpty()) {
            LOGGER.info("No active experience has been found for the environment (name: {}, crn: {})", pollerObject.getEnvironmentName(),
                    pollerObject.getEnvironmentCrn());
            return true;
        } else {
            Set<ExperienceCluster> experiencesWithDeleteFailed = connectedExperiences.stream()
                    .filter(e -> DELETE_FAILED_STATUS.equals(e.getStatus()))
                    .collect(Collectors.toSet());
            if (experiencesWithDeleteFailed.isEmpty()) {
                LOGGER.info(connectedExperiences.size() + " experience has found for the environment (name: {}, crn: {})",
                        pollerObject.getEnvironmentName(), pollerObject.getEnvironmentCrn());
                return false;
            } else {
                String collectedStatusReason = experiencesWithDeleteFailed.stream()
                        .map(e -> "Failed to delete " + e.getName() + " experience, the problem was: " + getMessage(e))
                        .collect(Collectors.joining(", "));
                throw new ExperienceOperationFailedException(collectedStatusReason);
            }
        }
    }

    private String getMessage(ExperienceCluster e) {
        return StringUtils.isBlank(e.getStatusReason())
                ? "Could not identify the problem, please contact with our support team"
                : e.getStatusReason();
    }

    @Override
    public void handleTimeout(ExperiencePollerObject experiencePollerObject) {
        LOGGER.debug("Timeout handler passthrough, {}", experiencePollerObject);
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.debug("Exception handler passthrough", e);
    }

    @Override
    public String successMessage(ExperiencePollerObject experiencePollerObject) {
        return "Experience deletion was successful!";
    }

    @Override
    public boolean exitPolling(ExperiencePollerObject experiencePollerObject) {
        EnvironmentExperienceDto dto = buildDto(experiencePollerObject);
        return experienceConnectorService.getConnectedExperienceCount(dto) == 0;
    }

    @Override
    public boolean initialExitCheck(ExperiencePollerObject experiencePollerObject) {
        return false;
    }

    private EnvironmentExperienceDto buildDto(ExperiencePollerObject pollerObject) {
        return new EnvironmentExperienceDto.Builder()
                .withCrn(pollerObject.getEnvironmentCrn())
                .withAccountId(pollerObject.getAccountId())
                .withName(pollerObject.getEnvironmentName())
                .withCloudPlatform(pollerObject.getCloudPlatform())
                .build();
    }

}
