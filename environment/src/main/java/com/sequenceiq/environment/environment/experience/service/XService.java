package com.sequenceiq.environment.environment.experience.service;

import com.sequenceiq.environment.environment.experience.resolve.Experience;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class XService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XService.class);

    private static final String DEFAULT_EXPERIENCE_PROTOCOL = "https";

    private final ExperienceConnectorService experienceConnectorService;

    private final Map<String, Experience> configuredExperiences;

    private final ExperienceValidator experienceValidator;

    private final String experienceProtocol;

    private final String pathPostfix;

    public XService(@Value("${xp.protocol:https}") String experienceProtocol, @Value("${xp.path.postfix}") String pathPostfix,
                    ExperienceConnectorService experienceConnectorService, XPServices experienceProvider, ExperienceValidator experienceValidator) {
        this.experienceValidator = experienceValidator;
        this.configuredExperiences = identifyConfiguredExperiences(experienceProvider);
        this.pathPostfix = StringUtils.isEmpty(pathPostfix) ? "" : pathPostfix;
        LOGGER.debug("Experience checking postfix set to: {}", this.pathPostfix);
        this.experienceConnectorService = experienceConnectorService;
        this.experienceProtocol = StringUtils.isEmpty(experienceProtocol) ? DEFAULT_EXPERIENCE_PROTOCOL : experienceProtocol;
        LOGGER.debug("Experience connection protocol set to: {}", this.experienceProtocol);
    }

    /**
     * Checks all the configured experiences for any existing workspace which has a connection with the given environment.
     * If so, it will return the set of the names of the given experience.
     *
     * @param environmentCrn the resource crn of the environment. It must not be null or empty.
     * @return the name of the experiences which has an active workspace for the given environment.
     * @throws IllegalArgumentException if environmentCrn is null or empty
     */
    public Set<String> environmentHasActiveExperience(@NotNull String environmentCrn) {
        throwIfTrue(StringUtils.isEmpty(environmentCrn), () -> new IllegalArgumentException("Unable to check environment - experience relation, since the " +
                "given environment crn is null or empty!"));
        Set<String> affectedExperiences;
        affectedExperiences = configuredExperiences
                .entrySet()
                .stream()
                .filter(this::isExperienceConfigured)
                .map(xp -> isExperienceActiveForEnvironment(xp.getKey(), xp.getValue(), environmentCrn))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toSet());
        return affectedExperiences;
    }

    private Optional<String> isExperienceActiveForEnvironment(String experienceName, Experience xp, String environmentCrn) {
        LOGGER.debug("Checking whether the environment (crn: {}) has an active experience (name: {}) or not.", environmentCrn, experienceName);
        Set<String> entries = collectExperienceEntryNamesWhenItHasActiveWorkspaceForEnv(xp, environmentCrn);
        if (!entries.isEmpty()) {
            String entryNames = String.join(",", entries);
            LOGGER.info("The following experience ({}) has an active entry for the given environment! [entries: {}, environmentCrn: {}]",
                    experienceName, entryNames, environmentCrn);
            return Optional.of(experienceName);
        }
        return Optional.empty();
    }

    private Set<String> collectExperienceEntryNamesWhenItHasActiveWorkspaceForEnv(Experience xp, String envCrn) {
        String pathToExperience = experienceProtocol + "://" + xp.getPathPrefix() + ":" + xp.getPort() + xp.getPathInfix() + pathPostfix;
        return experienceConnectorService.getWorkspaceNamesConnectedToEnv(pathToExperience, envCrn);
    }

    private Map<String, Experience> identifyConfiguredExperiences(XPServices experienceProvider) {
        Map<String, Experience> experiences = experienceProvider.getExperiences()
                .entrySet()
                .stream()
                .filter(this::isExperienceConfigured)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (experiences.isEmpty()) {
            LOGGER.info("There are no - properly - configured experience endpoints in environment service! If you would like to check them, specify them" +
                    " in the application.yml!");
            return emptyMap();
        } else {
            String names = String.join(", ", new HashSet<>(experiences.keySet()));
            LOGGER.info("The following experience(s) have given for environment service: {}", names);
            return experiences;
        }
    }

    private boolean isExperienceConfigured(Map.Entry<String, Experience> xp) {
        boolean filled = experienceValidator.isExperienceFilled(xp.getValue());
        if (!filled) {
            LOGGER.debug("The following experience has not filled properly: {}", xp.getKey());
        }
        return filled;
    }

}
