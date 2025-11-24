package com.sequenceiq.environment.experience.common;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNullOtherwise;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.Experience;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceConnectorService;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.common.responses.CpInternalCluster;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;
import com.sequenceiq.environment.experience.policy.response.ProviderPolicyResponse;

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
        configuredExperiences = identifyConfiguredExperiences(config);
    }

    @Override
    public Set<ExperienceCluster> getConnectedClustersForEnvironment(EnvironmentExperienceDto environment) {
        LOGGER.debug("About to find connected experiences for environment which is in the following tenant: " + environment.getAccountId());
        Set<ExperienceCluster> activeExperiences = getActiveExperiences(environment.getCrn());
        if (!activeExperiences.isEmpty()) {
            String combinedNames = activeExperiences
                    .stream()
                    .map(ExperienceCluster::getName)
                    .collect(Collectors.joining(","));
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
                .filter(CommonExperience::hasResourceDeleteAccess)
                .filter(commonExperience -> activeExperiences.stream().anyMatch(c -> c.getExperienceName().equals(commonExperience.getName())))
                .forEach(commonExperience -> delete(commonExperience, environment));
    }

    @Override
    public Map<String, String> collectPolicy(EnvironmentExperienceDto environment) {
        Map<String, String> policies = new LinkedHashMap<>();
        configuredExperiences.stream()
                .filter(CommonExperience::hasFineGradePolicy)
                .forEachOrdered(configuredExperience -> {
                    String xpPath = commonExperiencePathCreator.createPathToExperiencePolicyProvider(configuredExperience);
                    try {
                        LOGGER.info("Requesting {} to fetch granular policy for experience '{}' for cloud platform '{}'",
                                ExperienceConnectorService.class.getSimpleName(), configuredExperience.getBusinessName(), environment.getCloudPlatform());
                        ExperiencePolicyResponse res = experienceConnectorService.collectPolicy(xpPath, environment.getCloudPlatform());
                        policies.put(configuredExperience.getBusinessName(), getIfNotNullOtherwise(res.getAws(), ProviderPolicyResponse::getPolicy, null));
                    } catch (ExperienceOperationFailedException eofe) {
                        LOGGER.warn("Unable to fetch policy from experience \"" + configuredExperience.getName() + "\" due to: " + eofe.getMessage(), eofe);
                        policies.put(configuredExperience.getBusinessName(), "");
                    }
                });
        return policies;
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
                .filter(CommonExperience::hasResourceDeleteAccess)
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
                            .withPublicName(xp.getBusinessName())
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

    private void delete(CommonExperience commonExperience, EnvironmentExperienceDto environment) {
        experienceConnectorService.deleteWorkspaceForEnvironment(
                commonExperiencePathCreator.createPathToExperience(commonExperience),
                environment.getCrn(),
                commonExperience.isForceDeleteCapable());
    }

    private boolean isExperienceConfigured(CommonExperience xp) {
        boolean filled = experienceValidator.isExperienceFilled(xp);
        if (!filled) {
            LOGGER.debug("The following experience is not filled properly: {}", xp.getName());
        }
        return filled;
    }

}
