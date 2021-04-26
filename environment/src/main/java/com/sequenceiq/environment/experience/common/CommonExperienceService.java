package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.Experience;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;

@Service
public class CommonExperienceService implements Experience {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperienceService.class);

    private static final String DELETED = "DELETED";

    private final CommonExperienceConnectorService experienceConnectorService;

    private final CommonExperiencePathCreator commonExperiencePathCreator;

    private final CommonExperienceValidator experienceValidator;

    private final Set<CommonExperience> configuredExperiences;

    public CommonExperienceService(CommonExperienceConnectorService experienceConnectorService, ExperienceServicesConfig config,
            CommonExperienceValidator experienceValidator, CommonExperiencePathCreator commonExperiencePathCreator) {
        this.commonExperiencePathCreator = commonExperiencePathCreator;
        this.experienceConnectorService = experienceConnectorService;
        this.experienceValidator = experienceValidator;
        this.configuredExperiences = identifyConfiguredExperiences(config);
    }

    @Override
    public Set<ExperienceCluster> getConnectedClustersForEnvironment(EnvironmentExperienceDto environment) {
        LOGGER.debug("About to find connected experiences for environment which is in the following tenant: " + environment.getAccountId());
        Set<ExperienceCluster> activeExperiences = getActiveExperiences(environment.getCrn());
        if (!activeExperiences.isEmpty()) {
            String combinedNames = activeExperiences.stream().map(ExperienceCluster::getName).collect(Collectors.joining(","));
            LOGGER.info("The following experiences has connected to this env: [env: {}, experience(s): {}]", environment.getName(), combinedNames);
        }
        return activeExperiences;
    }

    @Override
    public ExperienceSource getSource() {
        return ExperienceSource.BASIC;
    }

    @Override
    public void deleteConnectedExperiences(EnvironmentExperienceDto environment) {
        throwIfTrue(environment == null, () -> new IllegalArgumentException(EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!"));
        Set<ExperienceCluster> activeExperiences = getActiveExperiences(environment.getCrn());
        configuredExperiences
                .stream()
                .filter(commonExperience ->  activeExperiences.stream().anyMatch(c -> c.getExperienceName().equals(commonExperience.getName())))
                .forEach(commonExperience -> experienceConnectorService
                        .deleteWorkspaceForEnvironment(commonExperiencePathCreator.createPathToExperience(commonExperience), environment.getCrn()));
    }

    /**
     * Collects info about all the configured experiences for any existing workspace which has a connection with the given environment.
     * If so, it will return the set of clusters of the given experience.
     *
     * @param environmentCrn the resource crn of the environment. It must not be null or empty.
     * @return set of the experiences which has an active workspace for the given environment, from all configured common experiences.
     * @throws IllegalArgumentException if environmentCrn is null or empty
     */
    private Set<ExperienceCluster> getActiveExperiences(@NotNull String environmentCrn) {
        throwIfTrue(StringUtils.isEmpty(environmentCrn), () -> new IllegalArgumentException("Unable to check environment - experience relation, since the " +
                "given environment crn is null or empty!"));
        return configuredExperiences
                .stream()
                .map(xp -> getExperiencesForEnvironment(xp.getName(), xp, environmentCrn))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    private Set<ExperienceCluster> getExperiencesForEnvironment(String experienceName, CommonExperience xp, String environmentCrn) {
        LOGGER.debug("Checking whether the environment (crn: {}) has an active experience (name: {}) or not.", environmentCrn, experienceName);
        Set<CpInternalCluster> experienceClusters =
                experienceConnectorService.getExperienceClustersConnectedToEnv(commonExperiencePathCreator.createPathToExperience(xp), environmentCrn);
        if (!experienceClusters.isEmpty()) {
            LOGGER.info("The following experience ({}) has an active entry for the given environment! [entries: {}, environmentCrn: {}]",
                    experienceName, experienceClusters, environmentCrn);
            return experienceClusters.stream()
                    .map(cp -> ExperienceCluster.builder()
                            .withName(cp.getName())
                            .withExperienceName(xp.getName())
                            .withStatus(cp.getStatus())
                            .withStatusReason(cp.getStatusReason())
                            .build())
                    .filter(cp -> !DELETED.equals(cp.getStatus()))
                    .collect(toSet());
        }
        return Set.of();
    }

    private Set<CommonExperience> identifyConfiguredExperiences(ExperienceServicesConfig config) {
        Set<CommonExperience> experiences = config.getConfigs()
                .stream()
                .filter(this::isExperienceConfigured)
                .collect(toSet());
        if (experiences.isEmpty()) {
            LOGGER.info("There are no - properly - configured experience endpoints in environment service! If you would like to check them, specify them" +
                    " in the experiences-config.yml!");
            return Collections.emptySet();
        } else {
            String xps = String.join(", ", new HashSet<>(experiences.stream().map(CommonExperience::toString).collect(toSet())));
            LOGGER.info("The following experience(s) have given for environment service: {}", xps);
            return experiences;
        }
    }

    private boolean isExperienceConfigured(CommonExperience xp) {
        boolean filled = experienceValidator.isExperienceFilled(xp);
        if (!filled) {
            LOGGER.debug("The following experience is not filled properly: {}", xp.getName());
        }
        return filled;
    }

}
