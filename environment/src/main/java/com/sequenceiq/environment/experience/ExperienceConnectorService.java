package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

@Service
public class ExperienceConnectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceConnectorService.class);

    private static final String EXPERIENCE_SCAN = "${environment.experience.scan.enabled}";

    private static final String XP_SCAN_DISABLED_BASE_MSG = "Scanning experience(s) has disabled due to {}, which means the returning amount of connected " +
            "experiences may not represent the reality!";

    private final EntitlementService entitlementService;

    private final boolean experienceScanEnabled;

    private final List<Experience> experiences;

    public ExperienceConnectorService(List<Experience> experiences, EntitlementService entitlementService,
            @Value(EXPERIENCE_SCAN) boolean experienceScanEnabled) {
        this.experienceScanEnabled = experienceScanEnabled;
        this.entitlementService = entitlementService;
        this.experiences = experiences;
    }

    public int getConnectedExperienceCount(EnvironmentExperienceDto dto) {
        return getConnectedExperiences(dto).size();
    }

    public Set<ExperienceCluster> getConnectedExperiences(EnvironmentExperienceDto environmentExperienceDto) {
        checkEnvironmentExperienceDto(environmentExperienceDto);
        if (experienceScanEnabled) {
            LOGGER.debug("The {} config property is enabled, therefore scanning for experience shall happen", EXPERIENCE_SCAN);
            if (isEntitlementEnabledForExperienceInteraction(environmentExperienceDto)) {
                LOGGER.debug("The {} entitlement is enabled, which means experience deletion through CB can be performed.",
                        CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT.name());
                if (!experiences.isEmpty()) {
                    LOGGER.debug("Collecting connected experiences for environment: {}", environmentExperienceDto.getName());
                    return experiences.stream()
                            .map(experience -> experience.getConnectedClustersForEnvironment(environmentExperienceDto))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                } else {
                    LOGGER.info("No configured experiences has been provided for checking!");
                }
            } else {
                LOGGER.info(XP_SCAN_DISABLED_BASE_MSG, "the experience deletion is disabled by entitlement " +
                        "(" + CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT.name() + ")");
            }
        } else {
            LOGGER.info(XP_SCAN_DISABLED_BASE_MSG, "the experience deletion is disabled in the Spring config");
        }
        return Set.of();
    }

    public Map<String, String> collectExperiencePoliciesForCredentialCreation(@NotNull EnvironmentExperienceDto environmentExperienceDto) {
        checkEnvironmentExperienceDto(environmentExperienceDto);
        Map<String, String> policies = new LinkedHashMap<>();
        LOGGER.info("About to collect policy JSONs from experiences");
        if (experienceScanEnabled) {
            if (isNotEmpty(experiences)) {
                for (Experience experience : experiences) {
                    experience.collectPolicy(environmentExperienceDto).forEach((k, v) -> policies.put(k, v));
                }
            } else {
                LOGGER.info("No configured experiences has been provided for policy collecting!");
            }
        } else {
            LOGGER.info(XP_SCAN_DISABLED_BASE_MSG, "the experience deletion is disabled in the Spring config");
        }
        return policies;
    }

    public void deleteConnectedExperiences(EnvironmentExperienceDto dto) {
        checkEnvironmentExperienceDto(dto);
        deleteLiftieBasedExperiences(dto);
        deleteBasicExperiences(dto);
    }

    private void deleteLiftieBasedExperiences(EnvironmentExperienceDto dto) {
        experiences.stream().filter(experience -> experience.getSource().equals(ExperienceSource.LIFTIE)).forEach(experience -> {
            LOGGER.info("About to delete LIFTIE experiences for environment '{}'", dto.getName());
            experience.deleteConnectedExperiences(dto);
        });
    }

    private void deleteBasicExperiences(EnvironmentExperienceDto dto) {
        experiences.stream().filter(experience -> experience.getSource().equals(ExperienceSource.BASIC)).forEach(experience -> {
            LOGGER.info("About to delete basic experiences for environment '{}'", dto.getName());
            experience.deleteConnectedExperiences(dto);
        });
    }

    private void checkEnvironmentExperienceDto(EnvironmentExperienceDto dto) {
        throwIfNull(dto, () -> new IllegalArgumentException("environment should not be null!"));
    }

    private boolean isEntitlementEnabledForExperienceInteraction(EnvironmentExperienceDto environmentExperienceDto) {
        return entitlementService.isExperienceDeletionEnabled(environmentExperienceDto.getAccountId());
    }

}
