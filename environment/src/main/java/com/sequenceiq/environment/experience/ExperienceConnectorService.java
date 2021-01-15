package com.sequenceiq.environment.experience;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

@Service
public class ExperienceConnectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceConnectorService.class);

    private final EntitlementService entitlementService;

    private final List<Experience> experiences;

    public ExperienceConnectorService(List<Experience> experiences, EntitlementService entitlementService) {
        this.experiences = experiences;
        this.entitlementService = entitlementService;
    }

    public int getConnectedExperienceCount(EnvironmentExperienceDto dto) {
        checkEnvironmentExperienceDto(dto);
        if (entitlementService.isExperienceDeletionEnabled(dto.getAccountId()) && !experiences.isEmpty()) {
            LOGGER.debug("Collecting connected experiences for environment: {}", dto.getName());
            return experiences
                    .stream()
                    .map(experience -> experience.clusterCountForEnvironment(dto))
                    .reduce(0, Integer::sum);
        }
        LOGGER.info("Scanning experience(s) has disabled, which means the returning amount of connected experiences may not represent the reality!");
        return 0;
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
        NullUtil.throwIfNull(dto, () -> new IllegalArgumentException("environment should not be null!"));
    }

}
