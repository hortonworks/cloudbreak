package com.sequenceiq.environment.experience.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.experience.common.CommonExperience;

class ExperienceServicesConfigTest {

    private ExperienceServicesConfig underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExperienceServicesConfig();
    }

    @Test
    void testWhenSettingTheExperiencesWithNullThenEmptyListShouldReturnInsteadOfNull() {
        underTest.setExperiences(null);

        List<CommonExperience> result = underTest.getExperiences();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}